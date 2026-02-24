import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

test.describe('Authentication Methods', () => {
  test.describe('Profile Page Auth Methods Card', () => {
    test('should display auth methods section for logged-in user', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('auth-methods-display');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Auth methods section should become visible after JS loads
      const authSection = page.locator('#auth-methods-section');
      await authSection.waitFor({ state: 'visible', timeout: 5000 });
      await expect(authSection).toBeVisible();
    });

    test('should show password badge for user with password', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('auth-methods-badges');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Wait for auth methods to load
      const badgesContainer = page.locator('#auth-method-badges');
      await badgesContainer.locator('.badge').first().waitFor({ state: 'visible', timeout: 5000 });

      // Should show a "Password" badge
      const passwordBadge = badgesContainer.locator('.badge:has-text("Password")');
      await expect(passwordBadge).toBeVisible();
    });

    test('should hide remove password button when user has no passkeys', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('auth-methods-no-passkey');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Wait for auth methods section to load
      const authSection = page.locator('#auth-methods-section');
      await authSection.waitFor({ state: 'visible', timeout: 5000 });

      // Remove password button should be hidden (user has no passkeys)
      const removePasswordContainer = page.locator('#removePasswordContainer');
      await expect(removePasswordContainer).toBeHidden();
    });

    test('should hide set password link for user with password', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('auth-methods-set-pass');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Wait for auth methods section to load
      const authSection = page.locator('#auth-methods-section');
      await authSection.waitFor({ state: 'visible', timeout: 5000 });

      // Set password container should be hidden (user already has password)
      const setPasswordContainer = page.locator('#setPasswordContainer');
      await expect(setPasswordContainer).toBeHidden();
    });
  });

  test.describe('Passkey Management Section', () => {
    test('should display passkey management section', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('passkey-section');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Passkey section should be visible
      const passkeySection = page.locator('#passkey-section');
      await expect(passkeySection).toBeVisible();

      // Add passkey button should be present
      const registerBtn = page.locator('#registerPasskeyBtn');
      await expect(registerBtn).toBeVisible();

      // Label input should be present
      const labelInput = page.locator('#passkeyLabel');
      await expect(labelInput).toBeVisible();
    });

    test('should show empty passkeys message when user has no passkeys', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('passkey-empty');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Wait for passkeys to load
      const passkeysList = page.locator('#passkeys-list');
      await passkeysList.locator('p, .card').first().waitFor({ state: 'visible', timeout: 5000 });

      // Should show "No passkeys registered yet" message
      const emptyMessage = passkeysList.locator('text=No passkeys registered yet');
      await expect(emptyMessage).toBeVisible();
    });
  });

  test.describe('Change Password Page Adaptation', () => {
    test('should show current password field for user with password', async ({
      page,
      updatePasswordPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('change-pass-has-pass');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updatePasswordPage.goto();
      await page.waitForLoadState('networkidle');

      // Current password section should be visible
      const currentPasswordSection = page.locator('#currentPasswordSection');
      await expect(currentPasswordSection).toBeVisible();

      // Page title should say "Update" (not "Set")
      const pageTitle = page.locator('#pageTitle');
      const titleText = await pageTitle.textContent();
      expect(titleText).not.toContain('Set a Password');

      // Set password info alert should be hidden
      const setPasswordInfo = page.locator('#setPasswordInfo');
      await expect(setPasswordInfo).toBeHidden();
    });
  });

  test.describe('Navigation Links', () => {
    test('should show change password link on profile page', async ({
      page,
      updateUserPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('auth-nav-links');
      cleanupEmails.push(user.email);

      await createAndLoginUser(page, testApiClient, user);
      await updateUserPage.goto();
      await page.waitForLoadState('networkidle');

      // Wait for auth methods to load so link text updates
      const authSection = page.locator('#auth-methods-section');
      await authSection.waitFor({ state: 'visible', timeout: 5000 });

      // Change password link should be visible
      const changePasswordLink = page.locator('#changePasswordLink');
      await expect(changePasswordLink).toBeVisible();

      // For a user with a password, it should say "Change Password"
      const linkText = await changePasswordLink.textContent();
      expect(linkText).toContain('Change Password');
    });
  });
});
