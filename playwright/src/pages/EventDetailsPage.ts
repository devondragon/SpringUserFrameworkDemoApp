import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Event Details page.
 */
export class EventDetailsPage extends BasePage {
  readonly path = '/event/';  // Base path, actual path includes event ID

  // Event info
  readonly eventName: Locator;
  readonly eventDescription: Locator;
  readonly eventDate: Locator;
  readonly eventTime: Locator;
  readonly eventLocation: Locator;

  // Action buttons (authenticated users)
  readonly registerButton: Locator;
  readonly unregisterButton: Locator;

  // Back navigation
  readonly backToEventsLink: Locator;

  // Login/Register prompts (non-authenticated users)
  readonly loginPrompt: Locator;

  constructor(page: Page) {
    super(page);
    this.eventName = page.locator('h1.text-center');
    this.eventDescription = page.locator('.card-text:has-text("Description")');
    this.eventDate = page.locator('.card-text:has-text("Date")');
    this.eventTime = page.locator('.card-text:has-text("Time")');
    this.eventLocation = page.locator('.card-text:has-text("Location")');
    this.registerButton = page.locator('button:has-text("Register for Event")');
    this.unregisterButton = page.locator('button:has-text("Unregister")');
    this.backToEventsLink = page.locator('a:has-text("Back to Events")');
    this.loginPrompt = page.locator('text=To register for the event');
  }

  /**
   * Navigate to a specific event's details page.
   */
  async gotoEvent(eventId: number): Promise<void> {
    await this.page.goto(`/event/${eventId}/details.html`);
  }

  /**
   * Get event name.
   */
  async getEventName(): Promise<string | null> {
    return this.eventName.textContent();
  }

  /**
   * Check if user can register (register button is visible).
   */
  async canRegister(): Promise<boolean> {
    return this.registerButton.isVisible();
  }

  /**
   * Check if user can unregister (unregister button is visible).
   */
  async canUnregister(): Promise<boolean> {
    return this.unregisterButton.isVisible();
  }

  /**
   * Check if user is currently registered for the event.
   */
  async isRegistered(): Promise<boolean> {
    return this.canUnregister();
  }

  /**
   * Register for the event.
   */
  async register(): Promise<void> {
    // Set up dialog handler for the alert and verify it appears
    const dialogPromise = this.page.waitForEvent('dialog', { timeout: 5000 });
    await this.registerButton.click();
    const dialog = await dialogPromise;
    await dialog.accept();
    // Wait for page to reload
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Unregister from the event.
   */
  async unregister(): Promise<void> {
    // Set up dialog handler for the alert and verify it appears
    const dialogPromise = this.page.waitForEvent('dialog', { timeout: 5000 });
    await this.unregisterButton.click();
    const dialog = await dialogPromise;
    await dialog.accept();
    // Wait for page to reload
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Check if login prompt is displayed (for non-authenticated users).
   */
  async hasLoginPrompt(): Promise<boolean> {
    return this.loginPrompt.isVisible();
  }

  /**
   * Navigate back to events list.
   */
  async goBackToEvents(): Promise<void> {
    await this.backToEventsLink.click();
    await this.page.waitForURL('**/event/**');
  }
}
