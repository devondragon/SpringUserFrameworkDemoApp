import { Page, Locator, expect } from '@playwright/test';

/**
 * Base Page Object class providing common functionality for all pages.
 */
export abstract class BasePage {
  readonly page: Page;
  abstract readonly path: string;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Navigate to this page.
   */
  async goto(): Promise<void> {
    await this.page.goto(this.path);
  }

  /**
   * Wait for page to be fully loaded.
   */
  async waitForLoad(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
  }

  /**
   * Get the current page URL.
   */
  async getCurrentUrl(): Promise<string> {
    return this.page.url();
  }

  /**
   * Check if the current URL contains the expected path.
   */
  async isOnPage(): Promise<boolean> {
    const url = await this.getCurrentUrl();
    return url.includes(this.path);
  }

  /**
   * Wait for a success message to appear.
   */
  async waitForSuccessMessage(timeout = 5000): Promise<void> {
    await this.page.waitForSelector('.alert-success', { timeout });
  }

  /**
   * Wait for an error message to appear.
   */
  async waitForErrorMessage(timeout = 5000): Promise<void> {
    await this.page.waitForSelector('.alert-danger', { timeout });
  }

  /**
   * Get text from an alert message.
   */
  async getAlertText(): Promise<string | null> {
    const alert = this.page.locator('.alert');
    if (await alert.count() > 0) {
      return alert.first().textContent();
    }
    return null;
  }

  /**
   * Check if user is logged in by looking for typical logged-in indicators.
   */
  async isLoggedIn(): Promise<boolean> {
    // Look for Account dropdown or update-user link which only appears when logged in
    const accountButton = this.page.locator('#accountDropdown, button:has-text("Account"), [role="button"]:has-text("Account")');
    const updateUserLink = this.page.locator('a[href*="update-user"]');
    return (await accountButton.count() > 0) || (await updateUserLink.count() > 0);
  }

  /**
   * Logout if currently logged in.
   */
  async logout(): Promise<void> {
    // The logout is in a dropdown menu under "Account"
    // First, open the Account dropdown
    const accountDropdown = this.page.locator('#accountDropdown, button:has-text("Account"), [role="button"]:has-text("Account")');
    if (await accountDropdown.count() > 0) {
      await accountDropdown.first().click();
      // Wait for dropdown to open
      await this.page.waitForTimeout(200);
    }

    // The logout button is a form submit button, not a link
    const logoutButton = this.page.locator('button:has-text("Logout"), form[action*="logout"] button, .dropdown-item:has-text("Logout")');
    if (await logoutButton.count() > 0) {
      await logoutButton.first().click();
      // Wait for logout to complete and redirect
      await this.page.waitForURL('**/index.html**', { timeout: 10000 });
    }
  }

  /**
   * Get the page title.
   */
  async getTitle(): Promise<string> {
    return this.page.title();
  }

  /**
   * Wait for navigation to complete.
   */
  async waitForNavigation(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Take a screenshot for debugging.
   */
  async screenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `reports/screenshots/${name}.png` });
  }
}
