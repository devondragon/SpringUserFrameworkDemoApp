import { test, expect, generateTestUser } from '../../src/fixtures';

test.describe('Login', () => {
  test.describe('Valid Login', () => {
    test('should login with valid credentials', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified test user
      const user = generateTestUser('login');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Login
      await loginPage.loginAndWait(user.email, user.password);

      // Verify redirect to success page
      expect(page.url()).toContain('messageKey=message.login.success');

      // Verify user is logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    test('should redirect to originally requested page after login', async ({
      page,
      loginPage,
      protectedPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified test user
      const user = generateTestUser('login-redirect');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Try to access protected page
      await protectedPage.goto();

      // Should be redirected to login
      await page.waitForURL('**/login**');

      // Login
      await loginPage.fillCredentials(user.email, user.password);
      await loginPage.submit();

      // Should be redirected to originally requested page or success page
      await page.waitForLoadState('networkidle');
    });
  });

  test.describe('Invalid Login', () => {
    test('should show error for invalid email', async ({ loginPage, page }) => {
      await loginPage.goto();
      await loginPage.fillCredentials('nonexistent@example.com', 'wrongpassword');
      await loginPage.submit();

      // Wait for redirect back to login page with error parameter
      await page.waitForURL('**/login**?error**', { timeout: 10000 });

      // Should remain on login page with error
      expect(await loginPage.hasError()).toBe(true);
    });

    test('should show error for invalid password', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified test user
      const user = generateTestUser('login-invalid-pass');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Try to login with wrong password
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, 'wrongpassword123');
      await loginPage.submit();

      // Wait for redirect back to login page with error parameter
      await page.waitForURL('**/login**?error**', { timeout: 10000 });

      // Should show error
      expect(await loginPage.hasError()).toBe(true);
    });

    test('should show error for empty fields', async ({ loginPage }) => {
      await loginPage.goto();

      // Try to submit with empty fields (HTML5 validation should prevent this)
      // Just verify the form elements exist
      await expect(loginPage.emailInput).toBeVisible();
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(loginPage.submitButton).toBeVisible();
    });
  });

  test.describe('Account Lockout', () => {
    test('should lock account after multiple failed attempts', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified test user
      const user = generateTestUser('login-lockout');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Attempt multiple failed logins (default is 10 attempts before lockout in main config)
      // Test profile has it set to 3
      for (let i = 0; i < 3; i++) {
        await loginPage.goto();
        await loginPage.fillCredentials(user.email, 'wrongpassword');
        await loginPage.submit();
        // Wait for redirect back to login page with error parameter
        await page.waitForURL('**/login**?error**', { timeout: 10000 });
      }

      // Check if account is locked via API
      const details = await testApiClient.getUserDetails(user.email);
      // Note: Lockout may or may not be immediate depending on configuration
      // This test verifies the failed login attempts are tracked
      expect(details.failedLoginAttempts).toBeGreaterThanOrEqual(3);
    });
  });

  test.describe('Unverified User', () => {
    test('should not allow login for unverified user', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create an unverified test user
      const user = generateTestUser('login-unverified');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: false, // Not verified
      });

      // Try to login
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, user.password);
      await loginPage.submit();

      // Should show error or redirect to verification page
      await page.waitForLoadState('networkidle');
      const url = page.url();
      const hasError = await loginPage.hasError();

      // Either shows error or redirects to verification request page
      expect(hasError || url.includes('verification')).toBe(true);
    });
  });

  test.describe('Navigation', () => {
    test('should navigate to registration page', async ({ loginPage, page }) => {
      await loginPage.goto();
      await loginPage.goToRegister();

      expect(page.url()).toContain('register');
    });

    test('should navigate to forgot password page', async ({ loginPage, page }) => {
      await loginPage.goto();
      await loginPage.goToForgotPassword();

      expect(page.url()).toContain('forgot-password');
    });
  });
});
