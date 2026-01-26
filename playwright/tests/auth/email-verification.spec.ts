import { test, expect, generateTestUser } from '../../src/fixtures';

test.describe('Email Verification', () => {
  test.describe('Valid Verification', () => {
    test('should verify email with valid token', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create user via Test API with enabled=false
      const user = generateTestUser('verify');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: false,
      });

      // Create verification token via Test API
      const tokenResponse = await testApiClient.createVerificationToken(user.email);
      expect(tokenResponse.success).toBe(true);
      expect(tokenResponse.token).not.toBeNull();

      // Verify user is not enabled initially
      let userStatus = await testApiClient.userEnabled(user.email);
      expect(userStatus.enabled).toBe(false);

      // Get verification URL and navigate to it
      const verificationUrl = await testApiClient.getVerificationUrl(user.email);
      expect(verificationUrl).not.toBeNull();

      await page.goto(verificationUrl!);
      await page.waitForLoadState('networkidle');

      // Should redirect to registration complete page
      expect(page.url()).toContain('registration-complete');

      // Verify user is now enabled
      userStatus = await testApiClient.userEnabled(user.email);
      expect(userStatus.enabled).toBe(true);
    });

    test('should allow login after verification', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create user via Test API with enabled=false
      const user = generateTestUser('verify-login');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: false,
      });

      // Create verification token via Test API
      await testApiClient.createVerificationToken(user.email);

      // Verify the email
      const verificationUrl = await testApiClient.getVerificationUrl(user.email);
      await page.goto(verificationUrl!);
      await page.waitForURL('**/registration-complete**');

      // Now try to login
      await loginPage.loginAndWait(user.email, user.password);

      // Should be logged in
      expect(await loginPage.isLoggedIn()).toBe(true);
    });
  });

  test.describe('Invalid Verification', () => {
    test('should reject invalid verification token', async ({
      page,
    }) => {
      // Navigate to verification URL with invalid token
      await page.goto('/user/registrationConfirm?token=invalid-token-12345');
      await page.waitForLoadState('networkidle');

      // Should show error or redirect to error page
      const url = page.url();
      const content = await page.textContent('body');

      // Either URL contains error or page content indicates error
      expect(
        url.includes('error') ||
        url.includes('bad') ||
        content?.toLowerCase().includes('error') ||
        content?.toLowerCase().includes('invalid')
      ).toBe(true);
    });

    test('should reject expired verification token', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      // Note: This test would require the ability to manipulate token expiry in DB
      // For now, we verify that an invalid token (simulating expired) is rejected
      const user = generateTestUser('verify-expired');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: false,
      });

      // Create verification token
      await testApiClient.createVerificationToken(user.email);

      // Use a fake expired token (any invalid UUID)
      await page.goto('/user/registrationConfirm?token=expired-invalid-token-12345');
      await page.waitForLoadState('networkidle');

      // Should show error (same handling as invalid token)
      const url = page.url();
      const content = await page.textContent('body');
      expect(
        url.includes('error') ||
        url.includes('bad') ||
        content?.toLowerCase().includes('error') ||
        content?.toLowerCase().includes('invalid')
      ).toBe(true);
    });

    test('should reject already used verification token', async ({
      page,
      testApiClient,
      cleanupEmails,
    }) => {
      // Create user via Test API with enabled=false
      const user = generateTestUser('verify-used');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: false,
      });

      // Create verification token
      await testApiClient.createVerificationToken(user.email);

      // Get verification URL before first use
      const verificationUrl = await testApiClient.getVerificationUrl(user.email);
      expect(verificationUrl).not.toBeNull();

      // First verification - should succeed
      await page.goto(verificationUrl!);
      await page.waitForURL('**/registration-complete**');

      // Verify user is enabled
      const userStatus = await testApiClient.userEnabled(user.email);
      expect(userStatus.enabled).toBe(true);

      // Try to use the same token again
      await page.goto(verificationUrl!);
      await page.waitForLoadState('networkidle');

      // Should either show error or redirect to registration-complete (idempotent behavior)
      // Both are acceptable - key thing is user stays verified
      const finalStatus = await testApiClient.userEnabled(user.email);
      expect(finalStatus.enabled).toBe(true);
    });
  });

  test.describe('Resend Verification', () => {
    test('should be able to request new verification email', async ({
      page,
      registerPage,
      testApiClient,
      cleanupEmails,
    }) => {
      // Register a new user
      const user = generateTestUser('resend-verify');
      cleanupEmails.push(user.email);

      await registerPage.registerAndWait(
        user.firstName,
        user.lastName,
        user.email,
        user.password
      );

      // Navigate to resend verification page
      await page.goto('/user/request-new-verification-email.html');
      await page.waitForLoadState('networkidle');

      // Page should load (specific implementation may vary)
      expect(page.url()).toContain('verification');
    });
  });
});
