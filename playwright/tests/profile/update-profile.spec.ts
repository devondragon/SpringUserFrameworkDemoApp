import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('Update Profile', () => {
  test.describe('Valid Updates', () => {
    test('should update first name', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('update-first');
      cleanupEmails.push(user.email);

      const createdUser = await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Update first name
      const newFirstName = 'UpdatedFirst';
      await updateUserPage.updateProfileAndWait(newFirstName, user.lastName);

      // Verify success message
      expect(await updateUserPage.isSuccessMessage()).toBe(true);

      // Verify in database
      const details = await testApiClient.getUserDetails(user.email);
      expect(details.firstName).toBe(newFirstName);
    });

    test('should update last name', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('update-last');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Update last name
      const newLastName = 'UpdatedLast';
      await updateUserPage.updateProfileAndWait(user.firstName, newLastName);

      // Verify success message
      expect(await updateUserPage.isSuccessMessage()).toBe(true);

      // Verify in database
      const details = await testApiClient.getUserDetails(user.email);
      expect(details.lastName).toBe(newLastName);
    });

    test('should update both names', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('update-both');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Update both names
      const newFirstName = 'NewFirst';
      const newLastName = 'NewLast';
      await updateUserPage.updateProfileAndWait(newFirstName, newLastName);

      // Verify success message
      expect(await updateUserPage.isSuccessMessage()).toBe(true);

      // Verify in database
      const details = await testApiClient.getUserDetails(user.email);
      expect(details.firstName).toBe(newFirstName);
      expect(details.lastName).toBe(newLastName);
    });
  });

  test.describe('Form Pre-population', () => {
    test('should show current name values', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = {
        ...generateTestUser('update-prepop'),
        firstName: 'CurrentFirst',
        lastName: 'CurrentLast',
      };
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Verify current values are shown
      const currentFirst = await updateUserPage.getFirstName();
      const currentLast = await updateUserPage.getLastName();

      expect(currentFirst).toBe(user.firstName);
      expect(currentLast).toBe(user.lastName);
    });
  });

  test.describe('Navigation', () => {
    test('should navigate to change password page', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('update-nav-pass');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Navigate to change password
      await updateUserPage.goToChangePassword();

      expect(page.url()).toContain('update-password');
    });

    test('should navigate to delete account page', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create and login as a verified user
      const user = generateTestUser('update-nav-delete');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      // Navigate to update user page
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Navigate to delete account
      await updateUserPage.goToDeleteAccount();

      expect(page.url()).toContain('delete-account');
    });
  });

  test.describe('Access Control', () => {
    test('should require authentication to access update page', async ({
      page,
      updateUserPage,
    }) => {
      // Try to access update page without logging in
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });
});
