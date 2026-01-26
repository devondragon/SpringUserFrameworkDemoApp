import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Update Password page.
 */
export class UpdatePasswordPage extends BasePage {
  readonly path = '/user/update-password.html';

  // Form elements
  readonly currentPasswordInput: Locator;
  readonly newPasswordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly submitButton: Locator;

  // Messages
  readonly globalMessage: Locator;

  // Error displays
  readonly currentPasswordError: Locator;
  readonly newPasswordError: Locator;
  readonly confirmPasswordError: Locator;

  // Password strength indicator
  readonly passwordStrength: Locator;
  readonly strengthLevel: Locator;

  constructor(page: Page) {
    super(page);
    this.currentPasswordInput = page.locator('#currentPassword');
    this.newPasswordInput = page.locator('#newPassword');
    this.confirmPasswordInput = page.locator('#confirmPassword');
    this.submitButton = page.getByRole('button', { name: 'Change Password' });
    this.globalMessage = page.locator('#globalMessage');
    this.currentPasswordError = page.locator('#currentPasswordError');
    this.newPasswordError = page.locator('#newPasswordError');
    this.confirmPasswordError = page.locator('#confirmPasswordError');
    this.passwordStrength = page.locator('#password-strength');
    this.strengthLevel = page.locator('#strengthLevel');
  }

  /**
   * Fill in password change form.
   */
  async fillForm(
    currentPassword: string,
    newPassword: string,
    confirmPassword?: string
  ): Promise<void> {
    await this.currentPasswordInput.fill(currentPassword);
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
   * Change password.
   */
  async changePassword(
    currentPassword: string,
    newPassword: string,
    confirmPassword?: string
  ): Promise<void> {
    await this.fillForm(currentPassword, newPassword, confirmPassword);
    await this.submit();
  }

  /**
   * Change password and wait for result.
   */
  async changePasswordAndWait(
    currentPassword: string,
    newPassword: string
  ): Promise<void> {
    await this.changePassword(currentPassword, newPassword);
    await this.waitForMessage();
  }

  /**
   * Wait for message to appear.
   */
  async waitForMessage(timeout = 5000): Promise<void> {
    await this.globalMessage.waitFor({ state: 'visible', timeout });
  }

  /**
   * Get message text.
   */
  async getMessageText(): Promise<string | null> {
    if (await this.globalMessage.isVisible()) {
      return this.globalMessage.textContent();
    }
    return null;
  }

  /**
   * Check if message indicates success.
   */
  async isSuccessMessage(): Promise<boolean> {
    const classes = await this.globalMessage.getAttribute('class');
    return classes?.includes('alert-success') || false;
  }

  /**
   * Check if message indicates error.
   */
  async isErrorMessage(): Promise<boolean> {
    const classes = await this.globalMessage.getAttribute('class');
    return classes?.includes('alert-danger') || false;
  }

  /**
   * Check if current password error is displayed.
   */
  async hasCurrentPasswordError(): Promise<boolean> {
    return this.currentPasswordError.isVisible();
  }

  /**
   * Check if new password error is displayed.
   */
  async hasNewPasswordError(): Promise<boolean> {
    return this.newPasswordError.isVisible();
  }

  /**
   * Get current password error text.
   */
  async getCurrentPasswordErrorText(): Promise<string | null> {
    if (await this.hasCurrentPasswordError()) {
      return this.currentPasswordError.textContent();
    }
    return null;
  }
}
