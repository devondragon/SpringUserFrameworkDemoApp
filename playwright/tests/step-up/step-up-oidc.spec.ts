import { test, expect } from '../../src/fixtures';
import type { CDPSession, Page } from '@playwright/test';

/**
 * Step-up (SUF-02) fallback for OIDC social-login accounts. Follow-up to #75, issue #90.
 *
 * An OIDC account has no passkey, so it can never satisfy a WEBAUTHN step-up. The framework must
 * therefore govern its initial `setPassword` by `allowInitialPasswordSetWithoutStepUp` (403 when the
 * flag is false, success when true) rather than returning the permanent `401 code 6` a passkey-only
 * account gets. This spec proves that end-to-end against a real Keycloak redirect login.
 *
 * Requires a Keycloak provider (globalSetup starts one when KEYCLOAK_E2E is set) and the app on:
 *   docker-keycloak,playwright-test,step-up
 * with the DS_SPRING_USER_KEYCLOAK_* provider URIs pointed at the host (localhost:8180), because the
 * app runs on the host here rather than inside the compose network. See playwright.config.ts for the
 * full run command.
 *
 * The two flag branches are separate app boots (the flag is boot-time config), selected by
 * STEP_UP_OIDC_ALLOW_INITIAL and the matching SPRING_APPLICATION_JSON override. Tagged @step-up-oidc so
 * only the chromium-step-up-oidc project runs it; every other project's server has step-up/OIDC off.
 */

/** The realm's pre-seeded user (keycloak/realm/realm-export.json). Dev-only credentials. */
const KEYCLOAK_USER = { username: 'demo', password: 'demo', email: 'demo@example.com' } as const;

/** Which allowInitialPasswordSetWithoutStepUp branch the app under test is booted with. */
const allowInitial = process.env.STEP_UP_OIDC_ALLOW_INITIAL === 'true';

/**
 * Drive the full OIDC redirect login: app login page -> Keycloak form -> back to the app authenticated.
 * Uses a fresh browser context (Playwright's per-test default), so there is no Keycloak SSO cookie and
 * the login form is always shown.
 */
async function loginWithKeycloak(page: Page): Promise<void> {
  await page.goto('/user/login.html');
  await page.locator('a[href$="/oauth2/authorization/keycloak"]').click();

  // Now on the Keycloak-hosted login form (published on host port 8180).
  await page.waitForURL((url) => url.port === '8180', { timeout: 30000 });
  await page.locator('#username').fill(KEYCLOAK_USER.username);
  await page.locator('#password').fill(KEYCLOAK_USER.password);
  await page.locator('#kc-login').click();

  // Back on the app, authenticated, off the login page.
  await page.waitForURL((url) => url.host === 'localhost:8080' && !url.pathname.includes('/login'), {
    timeout: 30000,
  });
}

/** POST /user/setPassword from an authenticated app page, using that page's CSRF meta tokens. */
async function setPassword(page: Page, newPassword: string): Promise<{ status: number; body: any }> {
  // Load an authenticated page so its CSRF meta reflects the logged-in session.
  await page.goto('/user/update-user.html');
  await page.waitForLoadState('domcontentloaded');
  return page.evaluate(async (pw) => {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
    const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
    const response = await fetch('/user/setPassword', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify({ newPassword: pw, confirmPassword: pw }),
    });
    let body: any = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    return { status: response.status, body };
  }, newPassword);
}

/**
 * Enable a CDP WebAuthn virtual authenticator that auto-approves create()/get(), so a passkey can be
 * enrolled without a human touch. Mirrors the helper in step-up-flow.spec.ts.
 */
async function setupVirtualAuthenticator(page: Page): Promise<CDPSession> {
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('WebAuthn.enable');
  await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport: 'internal',
      hasResidentKey: true,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true,
    },
  });
  return cdp;
}

/** Read the current credential id list via the management API. */
async function getCredentialIds(page: Page): Promise<string[]> {
  return page.evaluate(async () => {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
    const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
    const response = await fetch('/user/webauthn/credentials', { headers: { [csrfHeader]: csrfToken } });
    const creds = await response.json();
    return creds.map((c: { id: string }) => c.id);
  });
}

test.describe('Step-Up OIDC fallback @step-up-oidc', () => {
  // Serial: each test logs in as the same Keycloak user, which maps to one local account. Running them
  // one at a time keeps the per-test reset (below) from racing a concurrent login re-provisioning it.
  test.describe.configure({ mode: 'serial' });

  test.beforeEach(async ({ testApiClient }) => {
    // Delete the local KEYCLOAK-provisioned account so the next OIDC login re-creates it fresh with no
    // password. This keeps the true/false branches and re-runs deterministic (a prior "success" run
    // would otherwise leave a password behind, changing setPassword's behavior).
    await testApiClient.cleanupUser(KEYCLOAK_USER.email);
  });

  test('OIDC setPassword succeeds when allowInitialPasswordSetWithoutStepUp is true', async ({ page }) => {
    test.skip(!allowInitial, 'App booted with the flag false; the success branch does not apply.');

    await loginWithKeycloak(page);

    const result = await setPassword(page, 'Test@Pass123!');
    // The fallback allows the initial set: 200, not a step-up 401 and not a 403 denial.
    expect(result.status, `setPassword body: ${JSON.stringify(result.body)}`).toBe(200);

    // The account now has a password (auth-methods wraps its fields in a `data` envelope).
    const auth = await page.evaluate(async () => (await fetch('/user/auth-methods')).json());
    expect(auth.data.hasPassword).toBe(true);
  });

  test('OIDC setPassword is denied with 403, not a permanent 401 step-up, when the flag is false', async ({
    page,
  }) => {
    test.skip(allowInitial, 'App booted with the flag true; the denial branch does not apply.');

    await loginWithKeycloak(page);

    const result = await setPassword(page, 'Test@Pass123!');
    // The account cannot satisfy WEBAUTHN step-up, so the gate falls back to the flag rather than
    // returning a permanent 401. With the flag false the fallback denies with a plain 403.
    expect(result.status, `setPassword body: ${JSON.stringify(result.body)}`).toBe(403);
    // Nail the anti-regression: it must NOT be the passkey-only 401 (JSONResponse code 6), which the
    // client would turn into a never-satisfiable step-up prompt for an account with no passkey.
    expect(result.status).not.toBe(401);

    // No password was set.
    const auth = await page.evaluate(async () => (await fetch('/user/auth-methods')).json());
    expect(auth.data.hasPassword).toBe(false);
  });

  test('a freshly logged-in OIDC user can enroll a first passkey', async ({ page }) => {
    // Flag-independent; run it once, on the true (default) boot, to avoid a duplicate run.
    test.skip(!allowInitial, 'Runs once on the flag-true boot; skipped on the flag-false boot.');

    await setupVirtualAuthenticator(page);
    await loginWithKeycloak(page);

    // OIDC login stamps an authentication factor (FACTOR_AUTHORIZATION_CODE), which satisfies the
    // enrollment gate, so a first passkey can be added right after logging in.
    await page.goto('/user/update-user.html');
    await page.evaluate(async () => {
      const { registerPasskey } = await import('/js/user/webauthn-register.js');
      await registerPasskey('oidc-first-passkey');
    });
    await page.reload();
    expect((await getCredentialIds(page)).length).toBe(1);
  });
});
