import { execFileSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as http from 'node:http';
import * as path from 'node:path';

/**
 * Standalone Keycloak lifecycle for the OIDC step-up E2E (chromium-step-up-oidc).
 *
 * The rest of the Playwright suite runs the app via the `webServer` block and needs nothing external.
 * The OIDC project additionally needs an OpenID provider, so this module brings one up in a dev-mode
 * container and imports the same realm the docker-compose-keycloak stack uses. It is deliberately
 * decoupled from that compose file: dev mode (`start-dev`) needs no external database, so the only
 * dependency is Docker, which both developer machines and the CI runner already have.
 *
 * Only global-setup.ts / global-teardown.ts call these, and only when KEYCLOAK_E2E is set, so the other
 * Playwright projects never touch Docker.
 */

/** Container name for the E2E Keycloak. Fixed so a stale one can be reclaimed idempotently. */
const CONTAINER = 'suf-e2e-keycloak';

/** Keycloak image, kept in step with docker-compose-keycloak.yml so the realm imports identically. */
const IMAGE = 'quay.io/keycloak/keycloak:25.0.6';

/** Host port the browser and the host-run app both reach Keycloak on (published from container 8080). */
const HOST_PORT = 8180;

/** Realm OIDC metadata; a 200 here proves both that Keycloak is up and that the demo realm imported. */
const METADATA_URL = `http://localhost:${HOST_PORT}/realms/demo/.well-known/openid-configuration`;

/** Marker recording that this process started the container, so teardown only stops what it owns. */
const OWNED_MARKER = path.join(__dirname, '..', '..', 'test-results', '.keycloak-e2e-owned');

/** Absolute path to the realm export mounted into the container's import directory. */
function realmDir(): string {
  // __dirname is <repo>/playwright/src/utils; the realm lives at <repo>/keycloak/realm.
  return path.resolve(__dirname, '..', '..', '..', 'keycloak', 'realm');
}

/** Whether Keycloak's realm metadata is already answering (a developer's own instance, or a prior run). */
async function metadataReady(): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(METADATA_URL, (res) => {
      res.resume();
      resolve(res.statusCode === 200);
    });
    req.on('error', () => resolve(false));
    req.setTimeout(2000, () => {
      req.destroy();
      resolve(false);
    });
  });
}

/** Poll the metadata endpoint until it answers 200 or the timeout elapses. */
async function waitForMetadata(timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await metadataReady()) return;
    await new Promise((r) => setTimeout(r, 2000));
  }
  throw new Error(`Keycloak did not become ready at ${METADATA_URL} within ${timeoutMs}ms`);
}

/** Fail early with an actionable message if Docker is not usable, since KEYCLOAK_E2E requires it. */
function assertDockerAvailable(): void {
  try {
    execFileSync('docker', ['info'], { stdio: 'ignore' });
  } catch {
    throw new Error(
      'KEYCLOAK_E2E is set but Docker is not available. The chromium-step-up-oidc project needs Docker ' +
        'to run Keycloak. Start Docker, or unset KEYCLOAK_E2E to skip the OIDC project.'
    );
  }
}

/**
 * Ensure a Keycloak instance with the demo realm is answering on HOST_PORT.
 *
 * If one is already up (a developer left `docker-compose-keycloak.yml` running, or started their own),
 * it is reused and left running by teardown. Otherwise a dev-mode container is started, the realm is
 * imported, and an ownership marker is written so teardown removes exactly what this run created.
 */
export async function startKeycloak(): Promise<void> {
  if (await metadataReady()) {
    // Reuse an already-running provider; do not claim ownership, so teardown leaves it alone.
    return;
  }

  assertDockerAvailable();

  // A container by this name that is still running (but not yet answering metadata) means another
  // OIDC run started one and is mid-boot. Only one can own host port HOST_PORT, so fail fast rather
  // than tearing down a live container out from under a concurrent run.
  const running = execFileSync('docker', ['ps', '-q', '-f', `name=^${CONTAINER}$`], {
    encoding: 'utf8',
  }).trim();
  if (running) {
    throw new Error(
      `A Keycloak container named ${CONTAINER} is already running but not yet answering at ${METADATA_URL}. ` +
        'Another OIDC E2E run may be starting it (concurrent runs are not supported — they share host ' +
        `port ${HOST_PORT}). If it is stale, remove it with: docker rm -f ${CONTAINER}`
    );
  }
  // Reclaim a stale, non-running container from an interrupted run before starting a fresh one.
  try {
    execFileSync('docker', ['rm', '-f', CONTAINER], { stdio: 'ignore' });
  } catch {
    // No such container: nothing to reclaim.
  }

  try {
    // `docker run -d` blocks on the image pull before returning, so allow generous headroom, and
    // capture stderr so a real failure (port conflict, bad mount, registry throttling) is diagnosable
    // rather than surfacing as an opaque "Command failed".
    execFileSync(
      'docker',
      [
        'run',
        '-d',
        '--name',
        CONTAINER,
        '-p',
        `${HOST_PORT}:8080`,
        '-v',
        `${realmDir()}:/opt/keycloak/data/import`,
        IMAGE,
        'start-dev',
        '--import-realm',
      ],
      { stdio: ['ignore', 'ignore', 'pipe'], timeout: 180_000 }
    );
  } catch (err: any) {
    const stderr = err?.stderr ? `: ${String(err.stderr).trim()}` : '';
    throw new Error(`Failed to start the Keycloak container ${CONTAINER}${stderr}`);
  }

  fs.mkdirSync(path.dirname(OWNED_MARKER), { recursive: true });
  fs.writeFileSync(OWNED_MARKER, CONTAINER);

  try {
    // Cold start does a Liquibase-free dev boot plus the realm import; allow generous headroom for CI.
    await waitForMetadata(120_000);
  } catch (err) {
    // Readiness timed out on a container this run started. Playwright's separate globalTeardown is not
    // guaranteed to run when globalSetup throws, so clean up here rather than leaving the container and
    // marker behind (which the "already running but not answering" guard above would then reject on the
    // next run, forcing a manual `docker rm -f`). stopKeycloak removes exactly what we own.
    await stopKeycloak();
    throw err;
  }
}

/** Stop and remove the Keycloak container, but only if this run started it. */
export async function stopKeycloak(): Promise<void> {
  if (!fs.existsSync(OWNED_MARKER)) return;
  try {
    execFileSync('docker', ['rm', '-f', CONTAINER], { stdio: 'ignore' });
  } catch {
    // Already gone: nothing to do.
  } finally {
    fs.rmSync(OWNED_MARKER, { force: true });
  }
}
