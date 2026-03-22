import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('MFA', () => {
  test.describe('Challenge Page', () => {
    test('should render the MFA WebAuthn challenge page structure', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      // Login first so we have a session (page requires auth when MFA is disabled)
      const user = generateTestUser('mfa-page');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to the challenge page
      await page.goto('/user/mfa/webauthn-challenge.html');
      await page.waitForLoadState('domcontentloaded');

      // Verify page structure
      await expect(page.locator('.card-header')).toContainText('Additional Verification Required');
      await expect(page.locator('#verifyPasskeyBtn')).toBeVisible();
    });

    test('should have a cancel/sign out option', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('mfa-cancel');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      await page.goto('/user/mfa/webauthn-challenge.html');
      await page.waitForLoadState('domcontentloaded');

      // Verify cancel/sign out button is present (inside the logout form)
      await page.waitForLoadState('networkidle');
      await expect(
        page.locator('form[action*="logout"] button[type="submit"]')
      ).toBeVisible();
    });
  });

  test.describe('MFA Status Endpoint', () => {
    test('should handle MFA status request for authenticated user', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('mfa-status');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Call the MFA status endpoint
      const response = await page.request.get('/user/mfa/status');

      // MFA is disabled in playwright-test profile, so endpoint returns 404.
      // A separate MFA-enabled test profile would be needed to test the 200 case.
      expect(response.status()).toBe(404);
    });

    test('should require authentication for MFA status endpoint', async ({ page }) => {
      // Call without authentication
      const response = await page.request.get('/user/mfa/status', {
        maxRedirects: 0,
      });

      // MFA is disabled in playwright-test profile, so endpoint returns 404
      expect(response.status()).toBe(404);
    });
  });
});
