import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('Access Control', () => {
  test.describe('Protected Pages', () => {
    test('should redirect unauthenticated users to login for protected page', async ({
      page,
      protectedPage,
    }) => {
      // Try to access protected page without logging in
      await protectedPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });

    test('should allow authenticated users to access protected page', async ({
      page,
      protectedPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('protected-access');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Access protected page
      await protectedPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on protected page
      expect(page.url()).toContain('protected');
    });

    test('should redirect to login for user profile page', async ({
      page,
      updateUserPage,
    }) => {
      // Try to access user profile without logging in
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });

    test('should redirect to login for password change page', async ({
      page,
      updatePasswordPage,
    }) => {
      // Try to access password change without logging in
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });

    test('should redirect to login for delete account page', async ({
      page,
      deleteAccountPage,
    }) => {
      // Try to access delete account without logging in
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });

  test.describe('Public Pages', () => {
    test('should allow access to home page without authentication', async ({
      page,
    }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Should stay on home page
      expect(page.url()).not.toContain('login');
    });

    test('should allow access to login page without authentication', async ({
      page,
      loginPage,
    }) => {
      await loginPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on login page
      expect(page.url()).toContain('login');
    });

    test('should allow access to registration page without authentication', async ({
      page,
      registerPage,
    }) => {
      await registerPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on registration page
      expect(page.url()).toContain('register');
    });

    test('should allow access to forgot password page without authentication', async ({
      page,
      forgotPasswordPage,
    }) => {
      await forgotPasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on forgot password page
      expect(page.url()).toContain('forgot-password');
    });

    test('should allow access to events list without authentication', async ({
      page,
      eventListPage,
    }) => {
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on events page
      expect(page.url()).toContain('event');
    });

    test('should allow access to about page without authentication', async ({
      page,
    }) => {
      await page.goto('/about.html');
      await page.waitForLoadState('networkidle');

      // Should be on about page (not redirected)
      expect(page.url()).toContain('about');
    });
  });

  test.describe('Role-Based Access', () => {
    test('should restrict admin page to admin users', async ({
      page,
      adminActionsPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a regular user
      const user = generateTestUser('admin-restrict');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Try to access admin page
      await adminActionsPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be denied (403 or error page)
      // With @PreAuthorize, the URL stays the same but shows error page
      const pageContent = await page.textContent('body');

      // Check for error indicators: error page content, or absence of admin content
      const hasError = pageContent?.toLowerCase().includes('something went wrong') ||
                       pageContent?.toLowerCase().includes('access denied') ||
                       pageContent?.toLowerCase().includes('forbidden');

      // Regular user should not have admin access - should see an error
      expect(hasError).toBe(true);
    });
  });

  test.describe('Session Management', () => {
    test('should maintain session across page navigations', async ({
      page,
      loginPage,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('session-maintain');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to multiple protected pages
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');
      expect(page.url()).toContain('update-user');

      await page.goto('/event/my-events.html');
      await page.waitForLoadState('networkidle');
      expect(page.url()).toContain('my-events');

      // Should still be logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    test('should not access protected pages after logout', async ({
      page,
      loginPage,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('session-logout');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Verify logged in
      expect(await loginPage.isLoggedIn()).toBe(true);

      // Logout
      await loginPage.logout();
      await page.waitForLoadState('networkidle');

      // Try to access protected page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });
});
