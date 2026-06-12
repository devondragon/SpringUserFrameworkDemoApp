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
      grepInvert: /@mfa-enabled/,
      use: { ...devices['Desktop Chrome'] },
    },

    {
      name: 'firefox',
      grepInvert: /@mfa-enabled/,
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      grepInvert: /@mfa-enabled/,
      use: { ...devices['Desktop Safari'] },
    },

    /* Test against mobile viewports */
    {
      name: 'Mobile Chrome',
      grepInvert: /@mfa-enabled/,
      use: { ...devices['Pixel 5'] },
    },

    {
      name: 'Mobile Safari',
      grepInvert: /@mfa-enabled/,
      use: { ...devices['iPhone 12'] },
    },

    /* MFA flow tests: Chromium only (CDP virtual authenticator), MFA-enabled server required */
    {
      name: 'chromium-mfa',
      grep: /@mfa-enabled/,
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
