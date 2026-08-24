import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config();

/**
 * Unique project identifier for session isolation.
 * This ensures this project's Playwright instance doesn't conflict with other projects.
 */
const PROJECT_ID = 'spring-demo-app';

/**
 * Playwright configuration for Spring User Framework Demo App E2E tests.
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests',

  /* Unique output directories for this project */
  outputDir: path.join(__dirname, 'test-results', PROJECT_ID),

  /* Bring up a Keycloak OpenID provider for the chromium-step-up-oidc project. Both hooks no-op unless
   * KEYCLOAK_E2E is set, so every other project runs without Docker. See src/utils/keycloak.ts. */
  globalSetup: path.join(__dirname, 'global-setup.ts'),
  globalTeardown: path.join(__dirname, 'global-teardown.ts'),

  /* Run tests in files in parallel */
  fullyParallel: true,

  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: !!process.env.CI,

  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,

  /* Opt out of parallel tests on CI */
  workers: process.env.CI ? 1 : undefined,

  /* Reporter to use */
  reporter: [
    ['html', { outputFolder: 'reports/html' }],
    ['json', { outputFile: 'reports/results.json' }],
    ['list']
  ],

  /* Shared settings for all the projects below */
  use: {
    /* Base URL to use in actions like `await page.goto('/')` */
    baseURL: process.env.BASE_URL || 'http://localhost:8080',

    /* Collect trace when retrying the failed test */
    trace: 'on-first-retry',

    /* Capture screenshot on failure */
    screenshot: 'only-on-failure',

    /* Capture video on failure */
    video: 'on-first-retry',

    /* Default timeout for actions */
    actionTimeout: 10000,

    /* Default navigation timeout */
    navigationTimeout: 30000,

    /* Session isolation: unique browser launch options per project */
    launchOptions: {
      args: [
        /* Disable shared memory usage for better isolation in containers/parallel runs */
        '--disable-dev-shm-usage',
        /* Disable GPU to prevent shared resource conflicts */
        '--disable-gpu',
      ],
    },

    /* Unique context options for session isolation */
    contextOptions: {
      /* Ignore HTTPS errors for local development */
      ignoreHTTPSErrors: true,
    },
  },

  /* Configure global timeout */
  timeout: 60000,

  /* Expect timeout */
  expect: {
    timeout: 10000,
  },

  /* Configure projects for major browsers.
   *
   * Tests tagged @mfa-enabled need the server running with the mfa profile and are excluded from
   * the default projects (whose specs assume MFA is off). Run them with:
   *   APP_PROFILES=local,playwright-test,mfa npx playwright test --project=chromium-mfa
   */
  projects: [
    {
      name: 'chromium',
      grepInvert: /@mfa-enabled|@step-up-enabled|@step-up-oidc/,
      use: { ...devices['Desktop Chrome'] },
    },

    {
      name: 'firefox',
      grepInvert: /@mfa-enabled|@step-up-enabled|@step-up-oidc/,
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      grepInvert: /@mfa-enabled|@step-up-enabled|@step-up-oidc/,
      use: { ...devices['Desktop Safari'] },
    },

    /* Test against mobile viewports */
    {
      name: 'Mobile Chrome',
      grepInvert: /@mfa-enabled|@step-up-enabled|@step-up-oidc/,
      use: { ...devices['Pixel 5'] },
    },

    {
      name: 'Mobile Safari',
      grepInvert: /@mfa-enabled|@step-up-enabled|@step-up-oidc/,
      use: { ...devices['iPhone 12'] },
    },

    /* MFA flow tests: Chromium only (CDP virtual authenticator), MFA-enabled server required */
    {
      name: 'chromium-mfa',
      grep: /@mfa-enabled/,
      use: { ...devices['Desktop Chrome'] },
    },

    /* Step-up (SUF-02) tests: Chromium only (CDP virtual authenticator), step-up-enabled server required.
     * Run with:
     *   APP_PROFILES=local,playwright-test,step-up npx playwright test --project=chromium-step-up
     * (the step-up profile must come last so its overrides win). */
    {
      name: 'chromium-step-up',
      grep: /@step-up-enabled/,
      use: { ...devices['Desktop Chrome'] },
    },

    /* Step-up OIDC fallback (issue #90): Chromium only, needs both a Keycloak OpenID provider and the
     * app on the docker-keycloak,playwright-test,step-up profiles. globalSetup starts Keycloak when
     * KEYCLOAK_E2E is set. The two allowInitialPasswordSetWithoutStepUp branches are separate app boots,
     * selected by STEP_UP_OIDC_ALLOW_INITIAL (and the matching SPRING_APPLICATION_JSON override):
     *   # setPassword succeeds (flag true, from playwright-test):
     *   KEYCLOAK_E2E=1 STEP_UP_OIDC_ALLOW_INITIAL=true \
     *     DS_SPRING_USER_KEYCLOAK_CLIENT_ID=ds-spring-user-framework-demo \
     *     DS_SPRING_USER_KEYCLOAK_CLIENT_SECRET=FTp1j7sGvc4g3MFdghEX4n7RPhbu86PQ \
     *     DS_SPRING_USER_KEYCLOAK_PROVIDER_AUTHORIZATION_URI=http://localhost:8180/realms/demo/protocol/openid-connect/auth \
     *     DS_SPRING_USER_KEYCLOAK_PROVIDER_TOKEN_URI=http://localhost:8180/realms/demo/protocol/openid-connect/token \
     *     DS_SPRING_USER_KEYCLOAK_PROVIDER_USER_INFO_URI=http://localhost:8180/realms/demo/protocol/openid-connect/userinfo \
     *     DS_SPRING_USER_KEYCLOAK_PROVIDER_JWK_SET_URI=http://localhost:8180/realms/demo/protocol/openid-connect/certs \
     *     APP_PROFILES=docker-keycloak,playwright-test,step-up \
     *     npx playwright test --project=chromium-step-up-oidc
     *   # setPassword denied with 403, not a permanent 401 (flag false):
     *   ... STEP_UP_OIDC_ALLOW_INITIAL=false \
     *     SPRING_APPLICATION_JSON='{"user":{"security":{"allowInitialPasswordSetWithoutStepUp":false}}}' ... */
    {
      name: 'chromium-step-up-oidc',
      grep: /@step-up-oidc/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Run your local dev server before starting the tests */
  webServer: {
    command: `cd .. && ./gradlew bootRun --args="--spring.profiles.active=${process.env.APP_PROFILES || 'local,playwright-test'}"`,
    url: 'http://localhost:8080',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
