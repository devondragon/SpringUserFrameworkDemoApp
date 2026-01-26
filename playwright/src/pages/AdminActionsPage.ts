import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Admin Actions page.
 */
export class AdminActionsPage extends BasePage {
  readonly path = '/admin/actions.html';

  // Page content
  readonly pageTitle: Locator;
  readonly adminContent: Locator;

  constructor(page: Page) {
    super(page);
    this.pageTitle = page.locator('h1');
    this.adminContent = page.locator('#main_content');
  }

  /**
   * Check if admin page is accessible (user has admin privileges).
   */
  async isAccessible(): Promise<boolean> {
    // If we're redirected to login or get a 403, the page is not accessible
    const url = this.page.url();
    return url.includes('/admin/actions') && !url.includes('login');
  }

  /**
   * Get page title.
   */
  async getPageTitle(): Promise<string | null> {
    return this.pageTitle.textContent();
  }
}


/**
 * Page Object for the Protected page (generic protected resource).
 */
export class ProtectedPage extends BasePage {
  readonly path = '/protected.html';

  // Page content
  readonly pageTitle: Locator;
  readonly pageContent: Locator;

  constructor(page: Page) {
    super(page);
    this.pageTitle = page.locator('h1');
    this.pageContent = page.locator('#main_content');
  }

  /**
   * Check if protected page is accessible.
   */
  async isAccessible(): Promise<boolean> {
    const url = this.page.url();
    return url.includes('/protected') && !url.includes('login');
  }

  /**
   * Get page title.
   */
  async getPageTitle(): Promise<string | null> {
    return this.pageTitle.textContent();
  }
}
