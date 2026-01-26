import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Event List page.
 */
export class EventListPage extends BasePage {
  readonly path = '/event/';

  // Event cards
  readonly eventCards: Locator;

  constructor(page: Page) {
    super(page);
    this.eventCards = page.locator('.card');
  }

  /**
   * Get the number of events displayed.
   */
  async getEventCount(): Promise<number> {
    return this.eventCards.count();
  }

  /**
   * Get all event names.
   */
  async getEventNames(): Promise<string[]> {
    const cards = await this.eventCards.all();
    const names: string[] = [];
    for (const card of cards) {
      const title = await card.locator('.card-title').textContent();
      if (title) {
        names.push(title.trim());
      }
    }
    return names;
  }

  /**
   * Click on an event card by name.
   */
  async clickEvent(eventName: string): Promise<void> {
    const card = this.eventCards.filter({ hasText: eventName });
    await card.locator('a.btn-primary').click();
  }

  /**
   * Click "View Details" for an event by index.
   */
  async clickEventByIndex(index: number): Promise<void> {
    const cards = await this.eventCards.all();
    if (index < cards.length) {
      await cards[index].locator('a.btn-primary').click();
    } else {
      throw new Error(`Event index ${index} out of bounds (${cards.length} events)`);
    }
  }

  /**
   * Check if an event with the given name exists.
   */
  async hasEvent(eventName: string): Promise<boolean> {
    const card = this.eventCards.filter({ hasText: eventName });
    return (await card.count()) > 0;
  }

  /**
   * Get event details from a card.
   */
  async getEventDetails(eventName: string): Promise<{
    name: string;
    description: string;
    date: string;
    location: string;
  } | null> {
    const card = this.eventCards.filter({ hasText: eventName });
    if (await card.count() === 0) {
      return null;
    }

    const name = await card.locator('.card-title').textContent();
    const description = await card.locator('.card-text.text-muted').textContent();

    // Get date and location from the card text
    const cardBody = await card.locator('.card-body').textContent();

    return {
      name: name?.trim() || '',
      description: description?.trim() || '',
      date: '', // Would need more specific parsing
      location: '', // Would need more specific parsing
    };
  }

  /**
   * Navigate to event details page by name.
   */
  async goToEventDetails(eventName: string): Promise<void> {
    await this.clickEvent(eventName);
    await this.page.waitForURL('**/details**');
  }
}
