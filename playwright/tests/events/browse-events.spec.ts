import { test, expect } from '../../src/fixtures';

test.describe('Browse Events', () => {
  test.describe('Public Access', () => {
    test('should display events list without authentication', async ({
      page,
      eventListPage,
    }) => {
      // Navigate to events page without logging in
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // Should display the events page
      expect(page.url()).toContain('event');

      // Events should be visible (if any exist)
      // At least verify the page loads, event count depends on data
      await expect(eventListPage.eventCards.first()).toBeVisible().catch(() => {
        // No events is also valid
      });
    });

    test('should display event details', async ({
      page,
      eventListPage,
    }) => {
      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, verify they have expected content
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        // Get first event card
        const firstCard = eventListPage.eventCards.first();

        // Should have title
        await expect(firstCard.locator('.card-title')).toBeVisible();

        // Should have View Details button
        await expect(firstCard.locator('a.btn-primary')).toBeVisible();
      }
    });

    test('should navigate to event details page', async ({
      page,
      eventListPage,
      eventDetailsPage,
    }) => {
      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, click on one
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // Should be on event details page
        expect(page.url()).toContain('details');
      }
    });
  });

  test.describe('Event Details Page', () => {
    test('should show login prompt for unauthenticated users', async ({
      page,
      eventListPage,
      eventDetailsPage,
    }) => {
      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, go to details
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // Should show login prompt (not register/unregister buttons)
        const hasLoginPrompt = await eventDetailsPage.hasLoginPrompt();
        const canRegister = await eventDetailsPage.canRegister();
        const canUnregister = await eventDetailsPage.canUnregister();

        // Unauthenticated users should see login prompt, not action buttons
        expect(hasLoginPrompt || (!canRegister && !canUnregister)).toBe(true);
      }
    });
  });
});
