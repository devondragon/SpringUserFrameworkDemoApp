import { startKeycloak } from './src/utils/keycloak';

/**
 * Global setup, run once before the suite. Only the chromium-step-up-oidc project needs an external
 * OpenID provider, so this brings Keycloak up only when KEYCLOAK_E2E is set (the OIDC run command and
 * CI job set it). Every other project skips this and relies solely on the `webServer` block.
 */
async function globalSetup(): Promise<void> {
  if (!process.env.KEYCLOAK_E2E) return;
  await startKeycloak();
}

export default globalSetup;
