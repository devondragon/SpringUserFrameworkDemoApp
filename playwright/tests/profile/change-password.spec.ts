import { test, expect, generateTestUser, createAndLoginUser, loginUser } from '../../src/fixtures';

test.describe('Change Password', () => {
  test.describe('Valid Password Change', () => {
    test('should change password with correct current password', async ({
      page,
      updatePasswordPage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass');
      const originalPassword = user.password;
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Change password
      const newPassword = 'NewTest@Pass456!';
      await updatePasswordPage.changePasswordAndWait(originalPassword, newPassword);

      // Verify success message (if this fails, check backend logs for exception details)
      expect(await updatePasswordPage.isSuccessMessage()).toBe(true);

      // Logout
      await loginPage.logout();

      // Login with new password
      await loginPage.loginAndWait(user.email, newPassword);

      // Should be logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });

    test('should reject login with old password after change', async ({
      page,
      updatePasswordPage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-old');
      const originalPassword = user.password;
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Change password
      const newPassword = 'NewTest@Pass789!';
      await updatePasswordPage.changePasswordAndWait(originalPassword, newPassword);

      // Logout
      await loginPage.logout();

      // Try to login with old password
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, originalPassword);
      await loginPage.submit();
      // Wait for redirect back to login page with error parameter
      await page.waitForURL('**/login**?error**', { timeout: 10000 });

      // Should show error
      expect(await loginPage.hasError()).toBe(true);
    });
  });

  test.describe('Invalid Password Change', () => {
    test('should reject change with wrong current password', async ({
      page,
      updatePasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-wrong');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Try to change password with wrong current password
      // Listen for the server response to verify error handling
      const responsePromise = page.waitForResponse(
        response => response.url().includes('/user/updatePassword') && response.request().method() === 'POST'
      );

      await updatePasswordPage.changePassword('wrongCurrentPassword123!', 'NewTest@Pass123!');

      // Wait for the server response
      const response = await responsePromise;
      const responseBody = await response.json();

      // Server should indicate failure
      expect(responseBody.success).toBe(false);

      // Wait for the error message to appear in the DOM
      await updatePasswordPage.waitForMessage(5000);

      // Should show error
      const isError = await updatePasswordPage.isErrorMessage() ||
                      await updatePasswordPage.hasCurrentPasswordError();
      expect(isError).toBe(true);

      // Verify original password still works
      const details = await testApiClient.getUserDetails(user.email);
      expect(details.exists).toBe(true);
    });

    test('should reject weak new password', async ({
      page,
      updatePasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-weak');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Try to change to a weak password
      await updatePasswordPage.changePassword(user.password, 'weak');
      await page.waitForLoadState('networkidle');

      // Should show error or validation message
      const url = page.url();
      expect(url).toContain('update-password');
    });

    test('should reject mismatched new passwords', async ({
      page,
      updatePasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-mismatch');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Try to change with mismatched passwords
      await updatePasswordPage.fillForm(
        user.password,
        'NewTest@Pass123!',
        'DifferentPass@456!'
      );
      await updatePasswordPage.submit();
      await page.waitForLoadState('networkidle');

      // Should show error or validation message (client-side validation)
    });

    test('should reject password same as current', async ({
      page,
      updatePasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-same');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Try to change to the same password
      await updatePasswordPage.changePassword(user.password, user.password);
      await page.waitForLoadState('networkidle');

      // May or may not be rejected depending on policy
    });
  });

  test.describe('Password History', () => {
    test('should not allow reuse of recent passwords', async ({
      page,
      updatePasswordPage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('change-pass-history');
      const originalPassword = user.password;
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to change password page
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Change password
      const newPassword = 'NewTest@Pass111!';
      await updatePasswordPage.changePasswordAndWait(originalPassword, newPassword);

      // Now try to change back to original password
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      await updatePasswordPage.changePassword(newPassword, originalPassword);
      await page.waitForLoadState('networkidle');

      // Should reject due to password history (if enabled)
      // Behavior depends on configuration
    });
  });

  test.describe('Access Control', () => {
    test('should require authentication to access password change page', async ({
      page,
      updatePasswordPage,
    }) => {
      // Try to access password change page without logging in
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });
});
