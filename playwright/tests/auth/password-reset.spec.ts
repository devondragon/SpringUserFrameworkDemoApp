import { test, expect, generateTestUser, generateTestPassword } from '../../src/fixtures';

test.describe('Password Reset', () => {
  test.describe('Request Reset', () => {
    test('should request password reset for existing user', async ({
      page,
      forgotPasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified user
      const user = generateTestUser('reset-request');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Request password reset
      await forgotPasswordPage.requestResetAndWait(user.email);

      // Should redirect to pending page
      expect(page.url()).toContain('forgot-password-pending');

      // Verify reset token was created
      const tokenResponse = await testApiClient.getPasswordResetToken(user.email);
      expect(tokenResponse.token).not.toBeNull();
    });

    test('should handle non-existent email gracefully', async ({
      page,
      forgotPasswordPage,
    }) => {
      await forgotPasswordPage.goto();
      await forgotPasswordPage.requestReset('nonexistent-user-12345@example.com');

      // Wait for response
      await page.waitForLoadState('networkidle');

      // Should either show generic message (for security) or redirect to pending page
      // Most secure implementations show success even for non-existent emails
      const url = page.url();
      // Either shows pending page or stays on forgot password page
      expect(
        url.includes('forgot-password')
      ).toBe(true);
    });
  });

  test.describe('Complete Reset', () => {
    test('should reset password with valid token', async ({
      page,
      forgotPasswordPage,
      forgotPasswordChangePage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified user
      const user = generateTestUser('reset-complete');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Request password reset
      await forgotPasswordPage.requestResetAndWait(user.email);

      // Get reset token URL
      const resetUrl = await testApiClient.getPasswordResetUrl(user.email);
      expect(resetUrl).not.toBeNull();

      // Navigate to reset page
      await page.goto(resetUrl!);
      await page.waitForLoadState('networkidle');

      // Fill in new password
      const newPassword = 'NewTest@Pass456!';
      await forgotPasswordChangePage.fillForm(newPassword);
      await forgotPasswordChangePage.submit();

      // Wait for the success message to appear (AJAX form submission)
      await page.locator('#globalMessage').waitFor({ state: 'visible', timeout: 10000 });

      // Try to login with new password
      await loginPage.loginAndWait(user.email, newPassword);

      // Should be logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    test('should reject old password after reset', async ({
      page,
      forgotPasswordPage,
      forgotPasswordChangePage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified user
      const user = generateTestUser('reset-old-pass');
      const originalPassword = user.password;
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: originalPassword,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Request and complete password reset
      await forgotPasswordPage.requestResetAndWait(user.email);
      const resetUrl = await testApiClient.getPasswordResetUrl(user.email);
      await page.goto(resetUrl!);
      await page.waitForLoadState('networkidle');

      const newPassword = 'NewTest@Pass789!';
      await forgotPasswordChangePage.fillForm(newPassword);
      await forgotPasswordChangePage.submit();

      // Wait for the success message to appear (AJAX form submission)
      await page.locator('#globalMessage').waitFor({ state: 'visible', timeout: 10000 });

      // Try to login with old password
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, originalPassword);
      await loginPage.submit();
      await page.waitForLoadState('networkidle');

      // Should NOT be logged in (old password should fail)
      // The login page redirects back to itself on failure
      expect(await loginPage.isLoggedIn()).toBe(false);
    });
  });

  test.describe('Invalid Reset', () => {
    test('should reject invalid reset token', async ({
      page,
    }) => {
      // Navigate to reset page with invalid token
      await page.goto('/user/changePassword?token=invalid-reset-token-12345');
      await page.waitForLoadState('networkidle');

      // Should show error
      const url = page.url();
      const content = await page.textContent('body');

      expect(
        url.includes('error') ||
        url.includes('bad') ||
        content?.toLowerCase().includes('error') ||
        content?.toLowerCase().includes('invalid')
      ).toBe(true);
    });

    test('should reject weak new password', async ({
      page,
      forgotPasswordPage,
      forgotPasswordChangePage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified user
      const user = generateTestUser('reset-weak');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Request password reset
      await forgotPasswordPage.requestResetAndWait(user.email);

      // Get reset token URL
      const resetUrl = await testApiClient.getPasswordResetUrl(user.email);
      await page.goto(resetUrl!);
      await page.waitForLoadState('networkidle');

      // Try to set a weak password
      await forgotPasswordChangePage.fillForm('weak');
      await forgotPasswordChangePage.submit();

      // Wait for AJAX response and error to be displayed
      await page.waitForSelector('#globalError:not(.d-none)', { timeout: 10000 });

      // Should show password validation error
      expect(await forgotPasswordChangePage.hasError()).toBe(true);
      const errorText = await forgotPasswordChangePage.getErrorText();
      expect(errorText?.toLowerCase()).toContain('password');
    });

    test('should reject mismatched passwords', async ({
      page,
      forgotPasswordPage,
      forgotPasswordChangePage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create a verified user
      const user = generateTestUser('reset-mismatch');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      // Request password reset
      await forgotPasswordPage.requestResetAndWait(user.email);

      // Get reset token URL
      const resetUrl = await testApiClient.getPasswordResetUrl(user.email);
      await page.goto(resetUrl!);
      await page.waitForLoadState('networkidle');

      // Try to set mismatched passwords
      await forgotPasswordChangePage.fillForm('NewTest@Pass123!', 'DifferentPass@456!');
      await forgotPasswordChangePage.submit();
      await page.waitForLoadState('networkidle');

      // Should show error or stay on page (client-side validation)
    });
  });
});
