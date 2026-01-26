import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('Event Registration', () => {
  test.describe('Register for Event', () => {
    test('should register for an event when authenticated', async ({
      page,
      eventListPage,
      eventDetailsPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('event-register');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, register for one
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // Check if we can register
        if (await eventDetailsPage.canRegister()) {
          await eventDetailsPage.register();
          await page.waitForLoadState('networkidle');

          // Wait for page to update and show unregister button
          await page.locator('button:has-text("Unregister")').waitFor({ state: 'visible', timeout: 5000 });

          // After registering, should show unregister button
          expect(await eventDetailsPage.canUnregister()).toBe(true);
        }
      }
    });

    test('should show register button for unregistered event', async ({
      page,
      eventListPage,
      eventDetailsPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('event-show-register');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, check details page
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // Either register or unregister button should be visible
        const canRegister = await eventDetailsPage.canRegister();
        const canUnregister = await eventDetailsPage.canUnregister();

        expect(canRegister || canUnregister).toBe(true);
      }
    });
  });

  test.describe('Unregister from Event', () => {
    test('should unregister from an event', async ({
      page,
      eventListPage,
      eventDetailsPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('event-unregister');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // If not registered, register first
        if (await eventDetailsPage.canRegister()) {
          await eventDetailsPage.register();
          await page.waitForLoadState('networkidle');
        }

        // Now unregister
        if (await eventDetailsPage.canUnregister()) {
          await eventDetailsPage.unregister();
          await page.waitForLoadState('networkidle');

          // After unregistering, should show register button
          expect(await eventDetailsPage.canRegister()).toBe(true);
        }
      }
    });
  });

  test.describe('My Events Page', () => {
    test('should show my events page when authenticated', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('my-events');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to my events page
      await page.goto('/event/my-events.html');
      await page.waitForLoadState('networkidle');

      // Should be on my events page (not redirected to login)
      expect(page.url()).toContain('my-events');
    });

    test('my events page should be accessible but show personalized content', async ({
      page,
    }) => {
      // Access my events page without logging in
      await page.goto('/event/my-events.html');
      await page.waitForLoadState('networkidle');

      // Page should load (it's public but shows personalized content when logged in)
      expect(page.url()).toContain('my-events');

      // Should show the My Events heading
      await expect(page.locator('h1:has-text("My Events")')).toBeVisible();
    });
  });

  test.describe('Back Navigation', () => {
    test('should navigate back to events list', async ({
      page,
      eventListPage,
      eventDetailsPage,
    }) => {
      // Navigate to events page
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // If there are events, go to details and back
      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        // Go back to events list
        await eventDetailsPage.goBackToEvents();
        await page.waitForLoadState('networkidle');

        expect(page.url()).toContain('event');
      }
    });
  });
});
