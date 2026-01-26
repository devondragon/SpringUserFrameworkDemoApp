import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('Delete Account', () => {
  test.describe('Valid Deletion', () => {
    test('should delete account with correct confirmation', async ({
      page,
      deleteAccountPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-account');
      // Add to cleanupEmails in case deletion fails
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Delete account
      await deleteAccountPage.deleteAccountAndWait();

      // Verify user no longer exists or is disabled
      const userExists = await testApiClient.userExists(user.email);
      const userEnabled = await testApiClient.userEnabled(user.email);

      // Depending on configuration, account is either deleted or disabled
      expect(!userExists.exists || !userEnabled.enabled).toBe(true);
    });

    test('should logout after account deletion', async ({
      page,
      deleteAccountPage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-logout');
      // Add to cleanupEmails in case deletion fails
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Delete account and wait for success message
      await deleteAccountPage.deleteAccountAndWait();

      // The page shows success message but session is invalidated server-side.
      // Navigate to a protected page to verify we're logged out.
      await page.goto('/user/update-user.html');
      await page.waitForLoadState('networkidle');

      // Should be redirected to login (session was invalidated)
      expect(page.url()).toContain('login');
    });

    test('should not allow login after account deletion', async ({
      page,
      deleteAccountPage,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-no-login');
      // Add to cleanupEmails in case deletion fails
      cleanupEmails.push(user.email);
      const password = user.password;

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Delete account
      await deleteAccountPage.deleteAccountAndWait();

      // Try to login with deleted account
      await loginPage.goto();
      await loginPage.fillCredentials(user.email, password);
      await loginPage.submit();
      await page.waitForLoadState('networkidle');

      // Should show error
      const hasError = await loginPage.hasError();
      const isLoggedIn = await loginPage.isLoggedIn();

      expect(hasError || !isLoggedIn).toBe(true);
    });
  });

  test.describe('Confirmation Modal', () => {
    test('should show confirmation modal when delete button is clicked', async ({
      page,
      deleteAccountPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-modal');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Click delete button
      await deleteAccountPage.clickDelete();
      await deleteAccountPage.waitForModal();

      // Verify modal is visible
      expect(await deleteAccountPage.isModalVisible()).toBe(true);
    });

    test('should cancel deletion when modal is dismissed', async ({
      page,
      deleteAccountPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-cancel');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Open modal and cancel
      await deleteAccountPage.clickDelete();
      await deleteAccountPage.waitForModal();
      await deleteAccountPage.cancelDeletion();

      // Modal should close
      await page.waitForTimeout(500); // Wait for modal animation
      expect(await deleteAccountPage.isModalVisible()).toBe(false);

      // User should still exist
      const userExists = await testApiClient.userExists(user.email);
      expect(userExists.exists).toBe(true);
    });

    test('should require correct confirmation text', async ({
      page,
      deleteAccountPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('delete-wrong-text');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to delete account page
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Open modal
      await deleteAccountPage.clickDelete();
      await deleteAccountPage.waitForModal();

      // Type wrong confirmation text
      await deleteAccountPage.typeConfirmation('WRONG');
      await deleteAccountPage.confirmDeletion();

      await page.waitForTimeout(500);

      // User should still exist (button may be disabled or nothing happens)
      const userExists = await testApiClient.userExists(user.email);
      expect(userExists.exists).toBe(true);
    });
  });

  test.describe('Access Control', () => {
    test('should require authentication to access delete page', async ({
      page,
      deleteAccountPage,
    }) => {
      // Try to access delete account page without logging in
      await deleteAccountPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });
});
