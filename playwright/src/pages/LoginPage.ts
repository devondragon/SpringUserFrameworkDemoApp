import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Login page.
 */
export class LoginPage extends BasePage {
  readonly path = '/user/login.html';

  // Form elements
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;

  // Links
  readonly registerLink: Locator;
  readonly forgotPasswordLink: Locator;

  // Social login buttons
  readonly googleLoginButton: Locator;
  readonly facebookLoginButton: Locator;
  readonly keycloakLoginButton: Locator;

  // Error display
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.emailInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    // Use specific button text to avoid matching other buttons
    this.submitButton = page.getByRole('button', { name: 'Log In' });
    // Use specific link text to avoid matching dropdown menu items
    this.registerLink = page.getByRole('link', { name: 'Need to create an account? Register' });
    this.forgotPasswordLink = page.getByRole('link', { name: 'Forgot Password?' });
    this.googleLoginButton = page.locator('a[href*="oauth2/authorization/google"]');
    this.facebookLoginButton = page.locator('a[href*="oauth2/authorization/facebook"]');
    this.keycloakLoginButton = page.locator('a[href*="oauth2/authorization/keycloak"]');
    this.errorMessage = page.locator('.alert-danger');
  }

  /**
   * Fill in login credentials.
   */
  async fillCredentials(email: string, password: string): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
  }

  /**
   * Submit the login form.
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Perform complete login flow.
   */
  async login(email: string, password: string): Promise<void> {
    await this.goto();
    await this.fillCredentials(email, password);
    await this.submit();
  }

  /**
   * Login and wait for successful redirect.
   */
  async loginAndWait(email: string, password: string): Promise<void> {
    await this.login(email, password);
    // Wait for redirect away from login page
    await this.page.waitForURL((url) => !url.pathname.includes('login'), { timeout: 10000 });
  }

  /**
   * Get the error message text if present.
   */
  async getErrorText(): Promise<string | null> {
    if (await this.errorMessage.count() > 0) {
      return this.errorMessage.textContent();
    }
    return null;
  }

  /**
   * Check if error message is displayed.
   */
  async hasError(): Promise<boolean> {
    return (await this.errorMessage.count() > 0);
  }

  /**
   * Navigate to registration page.
   */
  async goToRegister(): Promise<void> {
    await this.registerLink.click();
    await this.page.waitForURL('**/register**');
  }

  /**
   * Navigate to forgot password page.
   */
  async goToForgotPassword(): Promise<void> {
    await this.forgotPasswordLink.click();
    await this.page.waitForURL('**/forgot-password**');
  }

  /**
   * Check if Google login is available.
   */
  async isGoogleLoginAvailable(): Promise<boolean> {
    return (await this.googleLoginButton.count() > 0);
  }

  /**
   * Check if Facebook login is available.
   */
  async isFacebookLoginAvailable(): Promise<boolean> {
    return (await this.facebookLoginButton.count() > 0);
  }
}
