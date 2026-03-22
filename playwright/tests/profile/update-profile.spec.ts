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
      await page.waitForLoadState('domcontentloaded');

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
      await page.waitForLoadState('domcontentloaded');

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
      await page.waitForLoadState('domcontentloaded');

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
      await page.waitForLoadState('domcontentloaded');

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
      await page.waitForLoadState('domcontentloaded');

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
      await page.waitForLoadState('domcontentloaded');

      // Navigate to delete account
      await updateUserPage.goToDeleteAccount();

      expect(page.url()).toContain('delete-account');
    });
  });

  test.describe('Validation', () => {
    test('should handle empty field submission gracefully', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('update-empty');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      await updateUserPage.goto();
      await page.waitForLoadState('domcontentloaded');

      // Clear both fields and attempt to submit
      await updateUserPage.firstNameInput.fill('');
      await updateUserPage.lastNameInput.fill('');
      await updateUserPage.submit();

      // HTML5 required validation should block submission — inputs should be invalid
      const firstNameIsValid = await updateUserPage.firstNameInput.evaluate(
        (el: HTMLInputElement) => el.checkValidity()
      );
      const lastNameIsValid = await updateUserPage.lastNameInput.evaluate(
        (el: HTMLInputElement) => el.checkValidity()
      );
      expect(firstNameIsValid).toBe(false);
      expect(lastNameIsValid).toBe(false);
    });

    test('should handle excessively long names gracefully', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('update-long');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);

      await updateUserPage.goto();
      await page.waitForLoadState('domcontentloaded');

      // Submit with very long names (300+ chars) and wait for server response
      const longName = 'A'.repeat(300);
      await updateUserPage.updateProfileAndWait(longName, longName);

      // App should handle gracefully — success or error message, not crash
      expect(await updateUserPage.globalMessage.isVisible()).toBe(true);
    });
  });

  test.describe('Access Control', () => {
    test('should require authentication to access update page', async ({
      page,
      updateUserPage,
    }) => {
      // Try to access update page without logging in
      await updateUserPage.goto();
      await page.waitForLoadState('domcontentloaded');

      // Should be redirected to login
      expect(page.url()).toContain('login');
    });
  });
});
