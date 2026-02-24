import { test, expect, generateTestUser } from '../../src/fixtures';

/**
 * Complete End-to-End User Journey Test
 *
 * This test validates the entire user lifecycle:
 * 1. Register a new account
 * 2. Verify email
 * 3. Login
 * 4. Update profile
 * 5. Change password
 * 6. Register for an event
 * 7. Unregister from event
 * 8. Delete account
 */
test.describe('Complete User Journey', () => {
  test('should complete full user lifecycle', async ({
    page,
    registerPage,
    loginPage,
    updateUserPage,
    updatePasswordPage,
    eventListPage,
    eventDetailsPage,
    deleteAccountPage,
    testApiClient,
  }) => {
    // Generate unique test user
    const user = generateTestUser('e2e-journey');
    const newFirstName = 'UpdatedFirst';
    const newLastName = 'UpdatedLast';
    const newPassword = 'NewE2E@Pass123!';

    // ==========================================
    // Step 1: Register a new account
    // ==========================================
    await test.step('Register new account', async () => {
      await registerPage.registerAndWait(
        user.firstName,
        user.lastName,
        user.email,
        user.password
      );

      // Verify redirect to registration complete page (auto-verified in test profile)
      expect(page.url()).toContain('registration-complete');

      // Verify user was created
      const userExists = await testApiClient.userExists(user.email);
      expect(userExists.exists).toBe(true);

      // Verify user is auto-enabled (sendVerificationEmail=false in test profile)
      const userEnabled = await testApiClient.userEnabled(user.email);
      expect(userEnabled.enabled).toBe(true);
    });

    // Note: Email verification step skipped - users are auto-verified in test profile

    // ==========================================
    // Step 3: Login
    // ==========================================
    await test.step('Login', async () => {
      await loginPage.loginAndWait(user.email, user.password);

      // Verify logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    // ==========================================
    // Step 4: Update profile
    // ==========================================
    await test.step('Update profile', async () => {
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Verify current values
      expect(await updateUserPage.getFirstName()).toBe(user.firstName);
      expect(await updateUserPage.getLastName()).toBe(user.lastName);

      // Update profile
      await updateUserPage.updateProfileAndWait(newFirstName, newLastName);

      // Verify success
      expect(await updateUserPage.isSuccessMessage()).toBe(true);

      // Verify in database
      const details = await testApiClient.getUserDetails(user.email);
      expect(details.firstName).toBe(newFirstName);
      expect(details.lastName).toBe(newLastName);
    });

    // ==========================================
    // Step 5: Change password
    // ==========================================
    await test.step('Change password', async () => {
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Change password
      await updatePasswordPage.changePasswordAndWait(user.password, newPassword);

      // Verify success
      expect(await updatePasswordPage.isSuccessMessage()).toBe(true);

      // Logout and verify new password works
      await loginPage.logout();
      await loginPage.loginAndWait(user.email, newPassword);
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    // ==========================================
    // Step 6: Register for an event (if events exist)
    // ==========================================
    let registeredForEvent = false;
    await test.step('Register for event', async () => {
      await eventListPage.goto();
      await page.waitForLoadState('networkidle');

      const eventCount = await eventListPage.getEventCount();
      if (eventCount > 0) {
        await eventListPage.clickEventByIndex(0);
        await page.waitForLoadState('networkidle');

        if (await eventDetailsPage.canRegister()) {
          registeredForEvent = await eventDetailsPage.register();
          if (registeredForEvent) {
            // Server-side rendered page may show stale state under concurrent load;
            // if the API confirmed success but the page hasn't caught up, reload once.
            if (!await eventDetailsPage.canUnregister()) {
              await page.reload({ waitUntil: 'networkidle' });
            }
            expect(await eventDetailsPage.canUnregister()).toBe(true);
          }
        }
      }
    });

    // ==========================================
    // Step 7: Unregister from event (if registered)
    // ==========================================
    await test.step('Unregister from event', async () => {
      if (registeredForEvent && await eventDetailsPage.canUnregister()) {
        const unregistered = await eventDetailsPage.unregister();
        if (unregistered) {
          expect(await eventDetailsPage.canRegister()).toBe(true);
        }
      }
    });

    // ==========================================
    // Step 8: Delete account
    // ==========================================
    await test.step('Delete account', async () => {
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Delete account
      await deleteAccountPage.deleteAccountAndWait();

      // Verify account is deleted/disabled
      const userExists = await testApiClient.userExists(user.email);
      const userEnabled = await testApiClient.userEnabled(user.email);

      // Account should be deleted or disabled
      expect(!userExists.exists || !userEnabled.enabled).toBe(true);

      // Verify cannot login
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, newPassword);
      await loginPage.submit();
      await page.waitForLoadState('networkidle');

      // Should show error or be redirected
      expect(await loginPage.hasError() || !await loginPage.isLoggedIn()).toBe(true);
    });
  });

  test('should handle registration with weak password validation', async ({
    page,
    registerPage,
  }) => {
    const user = generateTestUser('e2e-weak-pass');

    await test.step('Submit registration with weak password', async () => {
      await registerPage.goto();
      await registerPage.fillForm(
        user.firstName,
        user.lastName,
        user.email,
        'weak'  // Too short, no special chars
      );
      await registerPage.acceptTerms();
      await registerPage.submit();
      await page.waitForLoadState('networkidle');

      // Should stay on registration page or show error
      expect(page.url()).toContain('register');
    });
  });

  test('should handle protected page access flow', async ({
    page,
    loginPage,
    updateUserPage,
    testApiClient,
    cleanupEmails,
  }) => {
    const user = generateTestUser('e2e-protected');
    cleanupEmails.push(user.email);

    await test.step('Create verified user', async () => {
      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });
    });

    await test.step('Access protected page without auth redirects to login', async () => {
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Should redirect to login
      expect(page.url()).toContain('login');
    });

    await test.step('Login and verify access to protected page', async () => {
      await loginPage.fillCredentials(user.email, user.password);
      await loginPage.submit();
      await page.waitForLoadState('networkidle');

      // Should be logged in
      expect(await loginPage.isLoggedIn()).toBe(true);

      // Access protected page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be on protected page
      expect(page.url()).toContain('update-user');
    });
  });

  test('should handle password reset flow end-to-end', async ({
    page,
    loginPage,
    forgotPasswordPage,
    forgotPasswordChangePage,
    testApiClient,
    cleanupEmails,
  }) => {
    const user = generateTestUser('e2e-reset');
    const originalPassword = user.password;
    const newPassword = 'ResetE2E@Pass999!';
    cleanupEmails.push(user.email);

    await test.step('Create verified user', async () => {
      await testApiClient.createUser({
        email: user.email,
        password: originalPassword,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });
    });

    await test.step('Request password reset', async () => {
      await forgotPasswordPage.requestResetAndWait(user.email);
      expect(page.url()).toContain('forgot-password-pending');
    });

    await test.step('Complete password reset', async () => {
      const resetUrl = await testApiClient.getPasswordResetUrl(user.email);
      expect(resetUrl).not.toBeNull();

      await page.goto(resetUrl!);
      await page.waitForLoadState('networkidle');

      await forgotPasswordChangePage.fillForm(newPassword);
      await forgotPasswordChangePage.submit();

      // Wait for the success message to appear (AJAX form submission)
      await page.locator('#globalMessage').waitFor({ state: 'visible', timeout: 10000 });
    });

    await test.step('Login with new password', async () => {
      await loginPage.loginAndWait(user.email, newPassword);
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    await test.step('Verify old password no longer works', async () => {
      await loginPage.logout();
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, originalPassword);
      await loginPage.submit();
      await page.waitForLoadState('networkidle');

      expect(await loginPage.hasError()).toBe(true);
    });
  });
});
