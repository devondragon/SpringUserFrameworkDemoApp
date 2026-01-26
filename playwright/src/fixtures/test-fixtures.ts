import { test as base, expect, Page } from '@playwright/test';
import { TestApiClient } from '../utils/test-api-client';
import {
  LoginPage,
  RegisterPage,
  UpdateUserPage,
  UpdatePasswordPage,
  ForgotPasswordPage,
  ForgotPasswordChangePage,
  DeleteAccountPage,
  EventListPage,
  EventDetailsPage,
  AdminActionsPage,
  ProtectedPage,
} from '../pages';

/**
 * Generate a unique test email address.
 */
export function generateTestEmail(prefix: string = 'playwright'): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8);
  return `${prefix}.test.${timestamp}.${random}@example.com`;
}

/**
 * Generate a valid test password that meets password policy requirements.
 */
export function generateTestPassword(): string {
  // Password must include: uppercase, lowercase, digit, special char, min 8 chars
  return 'Test@Pass123!';
}

/**
 * Test data for a new user.
 */
export interface TestUser {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/**
 * Generate test user data.
 */
export function generateTestUser(emailPrefix: string = 'playwright'): TestUser {
  return {
    email: generateTestEmail(emailPrefix),
    password: generateTestPassword(),
    firstName: 'Test',
    lastName: 'User',
  };
}

/**
 * Extended test fixtures type.
 */
type TestFixtures = {
  testApiClient: TestApiClient;
  testUser: TestUser;
  verifiedUser: TestUser;
  loginPage: LoginPage;
  registerPage: RegisterPage;
  updateUserPage: UpdateUserPage;
  updatePasswordPage: UpdatePasswordPage;
  forgotPasswordPage: ForgotPasswordPage;
  forgotPasswordChangePage: ForgotPasswordChangePage;
  deleteAccountPage: DeleteAccountPage;
  eventListPage: EventListPage;
  eventDetailsPage: EventDetailsPage;
  adminActionsPage: AdminActionsPage;
  protectedPage: ProtectedPage;
  cleanupEmails: string[];
};

/**
 * Extended test with custom fixtures.
 */
export const test = base.extend<TestFixtures>({
  /**
   * Test API client fixture.
   */
  testApiClient: async ({}, use) => {
    const client = new TestApiClient();
    await client.init();
    await use(client);
    await client.dispose();
  },

  /**
   * Generate a unique test user for each test.
   */
  testUser: async ({}, use) => {
    const user = generateTestUser();
    await use(user);
  },

  /**
   * Create a verified user for tests that need pre-existing user.
   */
  verifiedUser: async ({ testApiClient }, use) => {
    const user = generateTestUser('verified');

    // Create user via API
    await testApiClient.createUser({
      email: user.email,
      password: user.password,
      firstName: user.firstName,
      lastName: user.lastName,
      enabled: true,
    });

    await use(user);

    // Cleanup after test
    await testApiClient.cleanupUser(user.email);
  },

  /**
   * Track emails for cleanup.
   */
  cleanupEmails: async ({ testApiClient }, use) => {
    const emails: string[] = [];
    await use(emails);

    // Cleanup all tracked emails
    for (const email of emails) {
      await testApiClient.cleanupUser(email);
    }
  },

  /**
   * Login page fixture.
   */
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  /**
   * Register page fixture.
   */
  registerPage: async ({ page }, use) => {
    await use(new RegisterPage(page));
  },

  /**
   * Update user page fixture.
   */
  updateUserPage: async ({ page }, use) => {
    await use(new UpdateUserPage(page));
  },

  /**
   * Update password page fixture.
   */
  updatePasswordPage: async ({ page }, use) => {
    await use(new UpdatePasswordPage(page));
  },

  /**
   * Forgot password page fixture.
   */
  forgotPasswordPage: async ({ page }, use) => {
    await use(new ForgotPasswordPage(page));
  },

  /**
   * Forgot password change page fixture.
   */
  forgotPasswordChangePage: async ({ page }, use) => {
    await use(new ForgotPasswordChangePage(page));
  },

  /**
   * Delete account page fixture.
   */
  deleteAccountPage: async ({ page }, use) => {
    await use(new DeleteAccountPage(page));
  },

  /**
   * Event list page fixture.
   */
  eventListPage: async ({ page }, use) => {
    await use(new EventListPage(page));
  },

  /**
   * Event details page fixture.
   */
  eventDetailsPage: async ({ page }, use) => {
    await use(new EventDetailsPage(page));
  },

  /**
   * Admin actions page fixture.
   */
  adminActionsPage: async ({ page }, use) => {
    await use(new AdminActionsPage(page));
  },

  /**
   * Protected page fixture.
   */
  protectedPage: async ({ page }, use) => {
    await use(new ProtectedPage(page));
  },
});

/**
 * Re-export expect for convenience.
 */
export { expect };

/**
 * Helper to login a user and return to a page.
 */
export async function loginUser(
  page: Page,
  email: string,
  password: string
): Promise<void> {
  const loginPage = new LoginPage(page);
  await loginPage.loginAndWait(email, password);
}

/**
 * Helper to register and verify a user.
 */
export async function registerAndVerifyUser(
  page: Page,
  testApiClient: TestApiClient,
  user: TestUser
): Promise<void> {
  const registerPage = new RegisterPage(page);
  await registerPage.registerAndWait(
    user.firstName,
    user.lastName,
    user.email,
    user.password
  );

  // Get verification token and navigate to verification URL
  const verificationUrl = await testApiClient.getVerificationUrl(user.email);
  if (verificationUrl) {
    await page.goto(verificationUrl);
    await page.waitForURL('**/registration-complete**');
  } else {
    throw new Error('Failed to get verification URL');
  }
}

/**
 * Helper to create a verified user via API and login.
 */
export async function createAndLoginUser(
  page: Page,
  testApiClient: TestApiClient,
  user?: TestUser
): Promise<TestUser> {
  const testUser = user || generateTestUser();

  // Create verified user via API
  await testApiClient.createUser({
    email: testUser.email,
    password: testUser.password,
    firstName: testUser.firstName,
    lastName: testUser.lastName,
    enabled: true,
  });

  // Login
  await loginUser(page, testUser.email, testUser.password);

  return testUser;
}
