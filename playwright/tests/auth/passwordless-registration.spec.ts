import { test, expect, generateTestUser } from '../../src/fixtures';

test.describe('Passwordless Registration', () => {
  test.describe('Registration Mode Toggle', () => {
    test('should show passwordless toggle on registration page', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      // The toggle should be visible (WebAuthn is supported in Chromium)
      const toggle = registerPage.page.locator('#registrationModeToggle');
      await expect(toggle).toBeVisible();

      // Both mode buttons should be present
      const passwordBtn = registerPage.page.locator('#modePassword');
      const passwordlessBtn = registerPage.page.locator('#modePasswordless');
      await expect(passwordBtn).toBeVisible();
      await expect(passwordlessBtn).toBeVisible();

      // Password mode should be active by default
      await expect(passwordBtn).toHaveClass(/active/);
      await expect(passwordlessBtn).not.toHaveClass(/active/);
    });

    test('should hide password fields when passwordless mode is selected', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      // Password fields should be visible initially
      const passwordFields = registerPage.page.locator('#passwordFields');
      await expect(passwordFields).toBeVisible();
      await expect(registerPage.passwordInput).toBeVisible();
      await expect(registerPage.confirmPasswordInput).toBeVisible();

      // Click passwordless mode button
      await registerPage.page.locator('#modePasswordless').click();

      // Password fields should be hidden
      await expect(passwordFields).toBeHidden();

      // Passwordless info alert should be visible
      const passwordlessInfo = registerPage.page.locator('#passwordlessInfo');
      await expect(passwordlessInfo).toBeVisible();
    });

    test('should show password fields when switching back to password mode', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      // Switch to passwordless
      await registerPage.page.locator('#modePasswordless').click();
      const passwordFields = registerPage.page.locator('#passwordFields');
      await expect(passwordFields).toBeHidden();

      // Switch back to password mode
      await registerPage.page.locator('#modePassword').click();

      // Password fields should be visible again
      await expect(passwordFields).toBeVisible();
      await expect(registerPage.passwordInput).toBeVisible();
      await expect(registerPage.confirmPasswordInput).toBeVisible();

      // Passwordless info alert should be hidden
      const passwordlessInfo = registerPage.page.locator('#passwordlessInfo');
      await expect(passwordlessInfo).toBeHidden();
    });

    test('should toggle active state on mode buttons', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      const passwordBtn = registerPage.page.locator('#modePassword');
      const passwordlessBtn = registerPage.page.locator('#modePasswordless');

      // Switch to passwordless
      await passwordlessBtn.click();
      await expect(passwordlessBtn).toHaveClass(/active/);
      await expect(passwordBtn).not.toHaveClass(/active/);

      // Switch back to password
      await passwordBtn.click();
      await expect(passwordBtn).toHaveClass(/active/);
      await expect(passwordlessBtn).not.toHaveClass(/active/);
    });

    test('should keep name and email fields visible in passwordless mode', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      // Switch to passwordless
      await registerPage.page.locator('#modePasswordless').click();

      // Name and email fields should still be visible
      await expect(registerPage.firstNameInput).toBeVisible();
      await expect(registerPage.lastNameInput).toBeVisible();
      await expect(registerPage.emailInput).toBeVisible();

      // Terms checkbox should still be visible
      await expect(registerPage.termsCheckbox).toBeVisible();
    });

    test('should remove required attribute from password fields in passwordless mode', async ({
      registerPage,
    }) => {
      await registerPage.goto();
      await registerPage.page.waitForLoadState('networkidle');

      // Password fields should be required initially
      await expect(registerPage.passwordInput).toHaveAttribute('required', '');
      await expect(registerPage.confirmPasswordInput).toHaveAttribute('required', '');

      // Switch to passwordless
      await registerPage.page.locator('#modePasswordless').click();

      // Password fields should no longer be required
      await expect(registerPage.passwordInput).not.toHaveAttribute('required', '');
      await expect(registerPage.confirmPasswordInput).not.toHaveAttribute('required', '');

      // Switch back - should be required again
      await registerPage.page.locator('#modePassword').click();
      await expect(registerPage.passwordInput).toHaveAttribute('required', '');
      await expect(registerPage.confirmPasswordInput).toHaveAttribute('required', '');
    });
  });

  test.describe('Passwordless Form Submission', () => {
    test('should send passwordless registration request to correct endpoint', async ({
      page,
      registerPage,
    }) => {
      await registerPage.goto();
      await page.waitForLoadState('networkidle');

      // Switch to passwordless mode
      await page.locator('#modePasswordless').click();

      // Fill name and email
      await registerPage.firstNameInput.fill('Test');
      await registerPage.lastNameInput.fill('User');
      await registerPage.emailInput.fill('test-pwless-endpoint@example.com');
      await registerPage.acceptTerms();

      // Intercept the fetch request to verify it goes to the right endpoint
      const requestPromise = page.waitForRequest(
        request => request.url().includes('/user/registration/passwordless') && request.method() === 'POST'
      );

      await registerPage.submit();

      // Verify the request was sent to the passwordless endpoint
      const request = await requestPromise;
      expect(request.url()).toContain('/user/registration/passwordless');

      // Verify the payload contains only name and email (no password)
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.firstName).toBe('Test');
      expect(postData.lastName).toBe('User');
      expect(postData.email).toBe('test-pwless-endpoint@example.com');
      expect(postData.password).toBeUndefined();
      expect(postData.matchingPassword).toBeUndefined();
    });

    test('should send standard registration request when in password mode', async ({
      page,
      registerPage,
    }) => {
      await registerPage.goto();
      await page.waitForLoadState('networkidle');

      // Stay in password mode (default)
      await registerPage.fillForm('Test', 'User', 'test-standard@example.com', 'Test@Pass123!');
      await registerPage.acceptTerms();

      // Intercept the fetch request
      const requestPromise = page.waitForRequest(
        request => request.url().includes('/user/registration') && request.method() === 'POST'
      );

      await registerPage.submit();

      // Verify the request goes to the standard endpoint (not passwordless)
      const request = await requestPromise;
      expect(request.url()).not.toContain('passwordless');
    });
  });
});
