import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Update User Profile page.
 */
export class UpdateUserPage extends BasePage {
  readonly path = '/user/update-user.html';

  // Form elements
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly submitButton: Locator;

  // Messages
  readonly globalMessage: Locator;

  // Error displays
  readonly firstNameError: Locator;
  readonly lastNameError: Locator;

  // Navigation links
  readonly changePasswordLink: Locator;
  readonly deleteAccountLink: Locator;

  constructor(page: Page) {
    super(page);
    this.firstNameInput = page.locator('#firstName');
    this.lastNameInput = page.locator('#lastName');
    // Use specific button text to avoid matching logout button
    this.submitButton = page.getByRole('button', { name: 'Update Profile' });
    this.globalMessage = page.locator('#globalMessage');
    this.firstNameError = page.locator('#firstNameError');
    this.lastNameError = page.locator('#lastNameError');
    // Use role-based selectors with exact name to get visible buttons/links
    this.changePasswordLink = page.getByRole('link', { name: 'Change Password' }).locator('visible=true').first();
    this.deleteAccountLink = page.getByRole('link', { name: 'Delete Account' }).locator('visible=true').first();
  }

  /**
   * Get current first name value.
   */
  async getFirstName(): Promise<string> {
    return this.firstNameInput.inputValue();
  }

  /**
   * Get current last name value.
   */
  async getLastName(): Promise<string> {
    return this.lastNameInput.inputValue();
  }

  /**
   * Fill in profile form.
   */
  async fillForm(firstName: string, lastName: string): Promise<void> {
    await this.firstNameInput.clear();
    await this.firstNameInput.fill(firstName);
    await this.lastNameInput.clear();
    await this.lastNameInput.fill(lastName);
  }

  /**
   * Submit the form.
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Update profile.
   */
  async updateProfile(firstName: string, lastName: string): Promise<void> {
    await this.fillForm(firstName, lastName);
    await this.submit();
  }

  /**
   * Update profile and wait for success message.
   */
  async updateProfileAndWait(firstName: string, lastName: string): Promise<void> {
    await this.updateProfile(firstName, lastName);
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
    return classes?.includes('alert-success') || classes?.includes('alert-info') || false;
  }

  /**
   * Navigate to change password page.
   */
  async goToChangePassword(): Promise<void> {
    await this.changePasswordLink.click();
    await this.page.waitForURL('**/update-password**');
  }

  /**
   * Navigate to delete account page.
   */
  async goToDeleteAccount(): Promise<void> {
    await this.deleteAccountLink.click();
    await this.page.waitForURL('**/delete-account**');
  }
}
