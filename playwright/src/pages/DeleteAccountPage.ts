import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Delete Account page.
 */
export class DeleteAccountPage extends BasePage {
  readonly path = '/user/delete-account.html';

  // Form elements
  readonly deleteButton: Locator;

  // Modal elements
  readonly confirmationModal: Locator;
  readonly confirmationInput: Locator;
  readonly confirmButton: Locator;
  readonly cancelButton: Locator;

  // Messages
  readonly globalMessage: Locator;
  readonly globalError: Locator;

  constructor(page: Page) {
    super(page);
    this.deleteButton = page.locator('#deleteAccountForm button[type="submit"]');
    this.confirmationModal = page.locator('#deleteConfirmationModal');
    this.confirmationInput = page.locator('#deleteConfirmationInput');
    this.confirmButton = page.locator('#confirmDeletionButton');
    this.cancelButton = page.locator('.modal-footer button[data-bs-dismiss="modal"]');
    this.globalMessage = page.locator('#globalMessage');
    this.globalError = page.locator('#globalError');
  }

  /**
   * Click delete account button to show confirmation modal.
   */
  async clickDelete(): Promise<void> {
    await this.deleteButton.click();
  }

  /**
   * Wait for modal to be visible.
   */
  async waitForModal(): Promise<void> {
    await this.confirmationModal.waitFor({ state: 'visible' });
  }

  /**
   * Type confirmation text in modal.
   */
  async typeConfirmation(text: string): Promise<void> {
    await this.confirmationInput.fill(text);
  }

  /**
   * Confirm deletion in modal.
   */
  async confirmDeletion(): Promise<void> {
    await this.confirmButton.click();
  }

  /**
   * Cancel deletion and close modal.
   */
  async cancelDeletion(): Promise<void> {
    await this.cancelButton.click();
  }

  /**
   * Complete the account deletion flow.
   */
  async deleteAccount(): Promise<void> {
    await this.clickDelete();
    await this.waitForModal();
    await this.typeConfirmation('DELETE');
    await this.confirmDeletion();
  }

  /**
   * Delete account and wait for success message.
   * The app uses AJAX and shows a success message instead of redirecting.
   * The session is invalidated server-side.
   */
  async deleteAccountAndWait(): Promise<void> {
    await this.deleteAccount();
    // Wait for the success message to appear (AJAX response)
    await this.globalMessage.waitFor({ state: 'visible', timeout: 10000 });
    // Also wait for the form to be hidden (confirmation of success)
    await this.page.waitForFunction(() => {
      const form = document.querySelector('#deleteAccountForm');
      return form?.classList.contains('d-none');
    }, { timeout: 5000 });
  }

  /**
   * Check if modal is visible.
   */
  async isModalVisible(): Promise<boolean> {
    return this.confirmationModal.isVisible();
  }

  /**
   * Check if error message is displayed.
   */
  async hasError(): Promise<boolean> {
    return this.globalError.isVisible();
  }

  /**
   * Get error message text.
   */
  async getErrorText(): Promise<string | null> {
    if (await this.hasError()) {
      return this.globalError.textContent();
    }
    return null;
  }

  /**
   * Check if success message is displayed.
   */
  async hasSuccessMessage(): Promise<boolean> {
    return this.globalMessage.isVisible();
  }
}
