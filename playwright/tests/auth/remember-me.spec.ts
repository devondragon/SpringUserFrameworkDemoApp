import { test, expect, generateTestUser } from '../../src/fixtures';

/**
 * Remember-me cookie flow (issue #79 / framework #351).
 *
 * The demo enables user.security.rememberMe with the framework defaults: the
 * login form posts a "remember-me" checkbox and Spring Security issues a
 * hash-based "remember-me" cookie only when that parameter is present.
 */
test.describe('Remember Me', () => {
  test.describe('Cookie Issuance', () => {
    test('should issue persistent remember-me cookie when checkbox is checked', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('remember-me-on');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      await loginPage.goto();

      // The checkbox label renders from the label.form.login-remember message key
      await expect(page.locator('label[for="remember-me"]')).toHaveText('Remember me');

      await loginPage.fillCredentials(user.email, user.password);
      await loginPage.checkRememberMe();
      await loginPage.submit();
      await page.waitForURL((url) => !url.pathname.includes('login'), { timeout: 10000 });

      const cookies = await page.context().cookies();
      const rememberMeCookie = cookies.find((c) => c.name === 'remember-me');
      expect(rememberMeCookie).toBeDefined();
      expect(rememberMeCookie!.value.length).toBeGreaterThan(0);
      expect(rememberMeCookie!.httpOnly).toBe(true);
      // A persistent cookie has a future expiry; a session cookie reports expires === -1
      expect(rememberMeCookie!.expires).toBeGreaterThan(Date.now() / 1000);
    });

    test('should not issue remember-me cookie when checkbox is unchecked', async ({
      page,
      loginPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('remember-me-off');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      await loginPage.loginAndWait(user.email, user.password);

      const cookies = await page.context().cookies();
      expect(cookies.find((c) => c.name === 'remember-me')).toBeUndefined();
    });
  });

  test.describe('Session Expiry', () => {
    test('should auto-login from remember-me cookie after session cookie is gone', async ({
      page,
      loginPage,
      protectedPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('remember-me-relogin');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      await loginPage.goto();
      await loginPage.fillCredentials(user.email, user.password);
      await loginPage.checkRememberMe();
      await loginPage.submit();
      await page.waitForURL((url) => !url.pathname.includes('login'), { timeout: 10000 });

      const cookies = await page.context().cookies();
      const rememberMeCookie = cookies.find((c) => c.name === 'remember-me');
      expect(rememberMeCookie).toBeDefined();
      expect(rememberMeCookie!.expires).toBeGreaterThan(Date.now() / 1000);

      // Drop the server session cookie, simulating an expired/closed session.
      // The remember-me cookie survives and should re-authenticate the request.
      await page.context().clearCookies({ name: 'JSESSIONID' });

      await protectedPage.goto();
      expect(page.url()).not.toContain('login');
      expect(await protectedPage.isLoggedIn()).toBe(true);
    });

    test('should require fresh login without remember-me once session cookie is gone', async ({
      page,
      loginPage,
      protectedPage,
      testApiClient,
      cleanupEmails,
    }) => {
      const user = generateTestUser('remember-me-baseline');
      cleanupEmails.push(user.email);

      await testApiClient.createUser({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        enabled: true,
      });

      await loginPage.loginAndWait(user.email, user.password);

      await page.context().clearCookies({ name: 'JSESSIONID' });

      await protectedPage.goto();
      await page.waitForURL('**/login**', { timeout: 10000 });
    });
  });
});
