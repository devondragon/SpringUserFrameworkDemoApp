import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Forgot Password page.
 */
export class ForgotPasswordPage extends BasePage {
  readonly path = '/user/forgot-password.html';

  // Form elements
  readonly emailInput: Locator;
  readonly submitButton: Locator;

  // Error displays
  readonly globalError: Locator;
  readonly emailError: Locator;

  // Links
  readonly loginLink: Locator;

  constructor(page: Page) {
    super(page);
    this.emailInput = page.locator('#email');
    this.submitButton = page.locator('button[type="submit"]');
    this.globalError = page.locator('#globalError');
    this.emailError = page.locator('#emailError');
    this.loginLink = page.locator('a[href*="login"]');
  }

  /**
   * Fill in email address.
   */
  async fillEmail(email: string): Promise<void> {
    await this.emailInput.fill(email);
  }

  /**
   * Submit the form.
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Request password reset.
   */
  async requestReset(email: string): Promise<void> {
    await this.fillEmail(email);
    await this.submit();
  }

  /**
   * Request reset and wait for result page.
   */
  async requestResetAndWait(email: string): Promise<void> {
    await this.goto();
    await this.requestReset(email);
    // Wait for redirect to pending verification page
    await this.page.waitForURL('**/forgot-password-pending**', { timeout: 10000 });
  }

  /**
   * Check if global error is displayed.
   */
  async hasGlobalError(): Promise<boolean> {
    return this.globalError.isVisible();
  }

  /**
   * Get global error text.
   */
  async getGlobalErrorText(): Promise<string | null> {
    if (await this.hasGlobalError()) {
      return this.globalError.textContent();
    }
    return null;
  }

  /**
   * Navigate to login page.
   */
  async goToLogin(): Promise<void> {
    await this.loginLink.click();
    await this.page.waitForURL('**/login**');
  }
}


/**
 * Page Object for the Forgot Password Change page (after clicking reset link).
 */
export class ForgotPasswordChangePage extends BasePage {
  readonly path = '/user/forgot-password-change.html';

  // Form elements
  readonly newPasswordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly submitButton: Locator;

  // Messages
  readonly globalMessage: Locator;
  readonly globalError: Locator;

  constructor(page: Page) {
    super(page);
    this.newPasswordInput = page.locator('#password');
    this.confirmPasswordInput = page.locator('#matchPassword');
    this.submitButton = page.locator('button[type="submit"]');
    this.globalMessage = page.locator('#globalMessage');
    this.globalError = page.locator('#globalError');
  }

  /**
   * Navigate to this page with a reset token.
   */
  async gotoWithToken(token: string): Promise<void> {
    await this.page.goto(`/user/changePassword?token=${token}`);
  }

  /**
   * Fill in new password form.
   */
  async fillForm(newPassword: string, confirmPassword?: string): Promise<void> {
    await this.newPasswordInput.fill(newPassword);
    await this.confirmPasswordInput.fill(confirmPassword || newPassword);
  }

  /**
   * Submit the form.
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Change password with token.
   */
  async changePassword(
    token: string,
    newPassword: string
  ): Promise<void> {
    await this.gotoWithToken(token);
    await this.fillForm(newPassword);
    await this.submit();
  }

  /**
   * Check if error is displayed.
   */
  async hasError(): Promise<boolean> {
    return this.globalError.isVisible();
  }

  /**
   * Get error text.
   */
  async getErrorText(): Promise<string | null> {
    if (await this.hasError()) {
      return this.globalError.textContent();
    }
    return null;
  }
}
