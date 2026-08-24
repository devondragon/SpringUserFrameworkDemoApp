import { stopKeycloak } from './src/utils/keycloak';

/**
 * Global teardown, run once after the suite. Removes the Keycloak container, but only the one this run
 * started (stopKeycloak no-ops when the provider was reused rather than started here).
 */
async function globalTeardown(): Promise<void> {
  if (!process.env.KEYCLOAK_E2E) return;
  await stopKeycloak();
}

export default globalTeardown;
