import { test, expect, generateTestUser, TestUser } from '../../src/fixtures';
import { setupVirtualAuthenticator, addVirtualAuthenticator, getCredentialIds } from '../../src/utils';
import type { Page } from '@playwright/test';

/**
 * WebAuthn step-up (SUF-02) E2E, using Chromium's CDP virtual authenticator.
 *
 * Requires the app to run with step-up enabled and the E2E override profile:
 *   APP_PROFILES=local,playwright-test,step-up,step-up-e2e npx playwright test --project=chromium-step-up
 * step-up-e2e shrinks ttlSeconds and enables dev login so the timing- and factor-dependent cases run
 * deterministically; the later profiles must come last so their overrides win.
 *
 * Tagged @step-up-enabled so the default and MFA projects skip it: those servers run with step-up off.
 *
 * The account under test is passkey-only (no password). Its session, right after registration, carries
 * a fresh FACTOR_PASSWORD but no FACTOR_WEBAUTHN, so credential-altering operations are refused with 401
 * until the passkey ceremony stamps a fresh WEBAUTHN factor. That makes both paths deterministic with no
 * reliance on the TTL elapsing.
 */

/**
 * Register a passkey-only account and enroll its first passkey, leaving the browser session
 * authenticated with no fresh WEBAUTHN factor. Returns once a passkey is enrolled.
 */
async function createPasswordlessUserWithPasskey(page: Page, user: TestUser): Promise<void> {
  // An unauthenticated page first, for its CSRF token and a session to auto-login into.
  await page.goto('/user/register.html');
  await page.waitForLoadState('domcontentloaded');

  const registered = await page.evaluate(async (u) => {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
    const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
    const response = await fetch('/user/registration/passwordless', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify({ firstName: u.firstName, lastName: u.lastName, email: u.email }),
    });
    return { ok: response.ok, body: await response.json() };
  }, user);

  expect(registered.ok, `passwordless registration failed: ${JSON.stringify(registered.body)}`).toBe(true);

  // Passwordless registration auto-logs-in (enabled account). Reload an authenticated page so its CSRF
  // meta reflects the logged-in session, then enroll the first passkey via the app's own module.
  await page.goto('/user/update-user.html');
  await page.waitForLoadState('domcontentloaded');
  await page.evaluate(async () => {
    const { registerPasskey } = await import('/js/user/webauthn-register.js');
    await registerPasskey('e2e-step-up-passkey');
  });
}

/** Return the servlet session cookie value, used to prove the id rotates across step-up (fixation). */
async function getSessionCookie(page: Page): Promise<string | undefined> {
  const cookies = await page.context().cookies();
  const session = cookies.find((c) => c.name === 'JSESSIONID') || cookies.find((c) => /session/i.test(c.name));
  return session?.value;
}

test.describe('WebAuthn Step-Up @step-up-enabled', () => {
  // Run serially: each test creates a passwordless account, and concurrent inserts into user_account
  // deadlock in MariaDB under the framework's registration path. Serial execution keeps these
  // account-creating flows deterministic (the same reason auth-flow specs avoid racing registration).
  test.describe.configure({ mode: 'serial' });


  test('setting a password on a passkey-only account requires step-up, then succeeds after the ceremony', async ({
    page,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-setpw');
    cleanupEmails.push(user.email);

    await setupVirtualAuthenticator(page);
    await createPasswordlessUserWithPasskey(page, user);

    // Set-password page opens in "set" mode for a passwordless account.
    await page.goto('/user/update-password.html');
    await expect(page.locator('#setPasswordInfo')).toBeVisible();

    await page.locator('#newPassword').fill(user.password);
    await page.locator('#confirmPassword').fill(user.password);

    const sessionBefore = await getSessionCookie(page);
    const csrfBefore = await page.evaluate(() => document.querySelector('meta[name="_csrf"]')?.getAttribute('content'));
    await page.locator('#updatePasswordForm button[type="submit"]').click();

    // The server refuses with 401 (code 6); the client shows the step-up modal rather than a raw error.
    const verifyBtn = page.locator('#stepUpVerifyBtn');
    await expect(verifyBtn).toBeVisible();

    // The ceremony (auto-approved by the virtual authenticator) refreshes the WEBAUTHN factor; the client
    // refreshes the rotated CSRF token and retries the setPassword call, which now succeeds.
    await verifyBtn.click();
    await expect(page.locator('#globalMessage')).toHaveClass(/alert-success/, { timeout: 15000 });

    // The account now has a password (auth-methods wraps its fields in a `data` envelope).
    const auth = await page.evaluate(async () => {
      const response = await fetch('/user/auth-methods');
      return response.json();
    });
    expect(auth.data.hasPassword).toBe(true);

    // Browser-only verification the ticket exists to observe (step-up re-runs /login/webauthn mid-flow):
    // - the login JSON landed mid-flow without navigating the page away, and the retry fired;
    await expect(page).toHaveURL(/\/user\/update-password\.html/);
    // - factor merging reuses the existing session rather than starting a fresh one, so the session id is
    //   preserved and the user stays logged in (no fixation rotation: the principal is unchanged, so no
    //   fixation vector is introduced);
    const sessionAfter = await getSessionCookie(page);
    expect(sessionAfter).toBeTruthy();
    expect(sessionAfter).toBe(sessionBefore);
    // - but Spring still rotates the CSRF token on the re-authentication; the client picked up the new one,
    //   which is exactly what the /csrf refresh handles and why the retry did not fail with a 403;
    const csrfAfter = await page.evaluate(() => document.querySelector('meta[name="_csrf"]')?.getAttribute('content'));
    expect(csrfAfter).toBeTruthy();
    expect(csrfAfter).not.toBe(csrfBefore);
    // - authorities survived the merge: a protected page is still reachable without re-login.
    await page.goto('/user/update-user.html');
    await expect(page).toHaveURL(/\/user\/update-user\.html/);
  });

  test('deleting a passkey on a passkey-only account requires step-up, then succeeds after the ceremony', async ({
    page,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-delete');
    cleanupEmails.push(user.email);

    const cdp = await setupVirtualAuthenticator(page);
    await createPasswordlessUserWithPasskey(page, user);

    // Deleting the last passkey on a passwordless account is blocked (lockout protection), so enroll a second
    // one first. It needs its own (roaming) authenticator, since the first one declines a repeat enrollment.
    await addVirtualAuthenticator(cdp, 'usb');
    await page.goto('/user/update-user.html');
    await page.evaluate(async () => {
      const { registerPasskey } = await import('/js/user/webauthn-register.js');
      await registerPasskey('second-passkey');
    });
    await page.reload();
    await page.locator('#passkeys-list button[data-action="delete"]').first().waitFor();
    expect((await getCredentialIds(page)).length).toBe(2);

    // The delete flow opens a native confirm() first; auto-accept it, then step up.
    page.on('dialog', (dialog) => dialog.accept());
    await page.locator('#passkeys-list button[data-action="delete"]').first().click();

    const verifyBtn = page.locator('#stepUpVerifyBtn');
    await expect(verifyBtn).toBeVisible();
    await verifyBtn.click();

    await expect(page.locator('#passkeyMessage')).toHaveClass(/alert-success/, { timeout: 15000 });
    await expect.poll(async () => (await getCredentialIds(page)).length).toBe(1);
  });

  test('renaming a passkey on a passkey-only account requires step-up, then succeeds after the ceremony', async ({
    page,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-rename');
    cleanupEmails.push(user.email);

    await setupVirtualAuthenticator(page);
    await createPasswordlessUserWithPasskey(page, user);

    await page.goto('/user/update-user.html');
    await page.locator('#passkeys-list button[data-action="rename"]').first().waitFor();

    await page.locator('#passkeys-list button[data-action="rename"]').first().click();
    await page.locator('#renamePasskeyInput').fill('renamed-by-e2e');
    await page.locator('#confirmRenameButton').click();

    // Step-up modal, then ceremony, then the rename retry succeeds.
    const verifyBtn = page.locator('#stepUpVerifyBtn');
    await expect(verifyBtn).toBeVisible();
    await verifyBtn.click();

    await expect(page.locator('#passkeyMessage')).toHaveClass(/alert-success/, { timeout: 15000 });
    await expect(page.locator('#passkeys-list')).toContainText('renamed-by-e2e');
  });

  test('sensitive operations are refused with 401 step-up and run no ceremony when the WEBAUTHN factor is absent', async ({
    page,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-negative');
    cleanupEmails.push(user.email);

    await setupVirtualAuthenticator(page);
    await createPasswordlessUserWithPasskey(page, user);

    await page.goto('/user/update-user.html');
    // Enrollment shortly after a real login succeeds: createPasswordlessUserWithPasskey enrolled a passkey
    // on the fresh post-registration session (FACTOR_PASSWORD within enrollmentTtlSeconds), so one exists.
    const credentialIds = await getCredentialIds(page);
    expect(credentialIds.length).toBe(1);
    const [credentialId] = credentialIds;

    // Raw calls with no ceremony: the server gate alone must produce the two documented 401 shapes.
    const results = await page.evaluate(async (credId) => {
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
      const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
      const headers = { 'Content-Type': 'application/json', [csrfHeader]: csrfToken };

      const setPassword = await fetch('/user/setPassword', {
        method: 'POST',
        headers,
        body: JSON.stringify({ newPassword: 'Test@Pass123!', confirmPassword: 'Test@Pass123!' }),
      });
      const rename = await fetch(`/user/webauthn/credentials/${credId}/label`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({ label: 'should-not-apply' }),
      });
      const del = await fetch(`/user/webauthn/credentials/${credId}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken },
      });

      return {
        setPassword: { status: setPassword.status, body: await setPassword.json() },
        rename: { status: rename.status, body: await rename.json() },
        del: { status: del.status, body: await del.json() },
      };
    }, credentialId);

    // setPassword: JSONResponse code 6.
    expect(results.setPassword.status).toBe(401);
    expect(results.setPassword.body.code).toBe(6);

    // delete / rename: GenericResponse error "step-up-required".
    expect(results.rename.status).toBe(401);
    expect(results.rename.body.error).toBe('step-up-required');
    expect(results.del.status).toBe(401);
    expect(results.del.body.error).toBe('step-up-required');
  });

  test('a WEBAUTHN factor aged past ttlSeconds no longer authorizes a sensitive operation', async ({
    page,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-ttl');
    cleanupEmails.push(user.email);

    await setupVirtualAuthenticator(page);
    await createPasswordlessUserWithPasskey(page, user);

    // Make the WEBAUTHN factor fresh by running the passkey ceremony (a login assertion while already
    // logged in), then let it age past the step-up-e2e window (ttlSeconds=2).
    await page.goto('/user/update-user.html');
    await page.evaluate(async () => {
      const { authenticateWithPasskey } = await import('/js/user/webauthn-authenticate.js');
      await authenticateWithPasskey();
    });
    await page.waitForTimeout(3000);
    // The assertion rotated the CSRF token; reload to pick up the current one (the factor's age is server
    // state, unaffected by a page GET).
    await page.reload();

    const result = await page.evaluate(async () => {
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
      const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
      const response = await fetch('/user/setPassword', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify({ newPassword: 'Test@Pass123!', confirmPassword: 'Test@Pass123!' }),
      });
      return { status: response.status, body: await response.json() };
    });

    // The aged factor is refused exactly like an absent one: 401 code 6, before any mutation.
    expect(result.status).toBe(401);
    expect(result.body.code).toBe(6);
    const auth = await page.evaluate(async () => (await fetch('/user/auth-methods')).json());
    expect(auth.data.hasPassword).toBe(false);
  });

  test('enrollment from a factorless (stale) session is refused with 403 and an actionable message', async ({
    page,
    testApiClient,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-stale-enroll');
    cleanupEmails.push(user.email);
    await testApiClient.createUser({
      email: user.email,
      password: user.password,
      firstName: user.firstName,
      lastName: user.lastName,
      enabled: true,
    });

    await setupVirtualAuthenticator(page);

    // Dev login stamps no authentication factor, so the session cannot satisfy the enrollment gate. This is
    // the deterministic stand-in for a session aged past enrollmentTtlSeconds.
    const devLogin = await page.request.get(`/dev/login-as/${encodeURIComponent(user.email)}`);
    expect(devLogin.ok()).toBeTruthy();

    await page.goto('/user/update-user.html');
    await page.locator('#registerPasskeyBtn').waitFor();
    await page.locator('#passkeyLabel').fill('should-be-refused');
    await page.locator('#registerPasskeyBtn').click();

    // POST /webauthn/register returns a bare 403 (an authorization rule, not a step-up-required 401), which
    // the client surfaces as its own "sign in again" message rather than offering a passkey retry.
    await expect(page.locator('#passkeyMessage')).toHaveClass(/alert-danger/, { timeout: 15000 });
    await expect(page.locator('#passkeyMessage')).toContainText(/sign in again/i);
    // No passkey was added.
    expect((await getCredentialIds(page)).length).toBe(0);
  });

  test('first-passkey enrollment works when the only session came from the verification link', async ({
    page,
    testApiClient,
    cleanupEmails,
  }) => {
    const user = generateTestUser('stepup-verify-enroll');
    cleanupEmails.push(user.email);
    // A registered-but-unverified account; confirming the emailed link both enables it and logs it in.
    await testApiClient.createUser({
      email: user.email,
      password: user.password,
      firstName: user.firstName,
      lastName: user.lastName,
      enabled: false,
    });
    await testApiClient.createVerificationToken(user.email);

    await setupVirtualAuthenticator(page);

    // Confirming the verification link auto-logs-in with FACTOR_OTT (not WEBAUTHN), which is a factor and so
    // satisfies the enrollment gate: the user can register a first passkey right after verifying.
    const verificationUrl = await testApiClient.getVerificationUrl(user.email);
    await page.goto(verificationUrl!);
    await expect(page).toHaveURL(/registration-complete/);

    await page.goto('/user/update-user.html');
    await page.evaluate(async () => {
      const { registerPasskey } = await import('/js/user/webauthn-register.js');
      await registerPasskey('verify-link-passkey');
    });
    await page.reload();
    expect((await getCredentialIds(page)).length).toBe(1);
  });

  test('registering a passkey notifies the account owner by email', async ({ page, cleanupEmails }) => {
    const user = generateTestUser('stepup-notify');
    cleanupEmails.push(user.email);

    await setupVirtualAuthenticator(page);
    // createPasswordlessUserWithPasskey enrolls a passkey, which (notifyOnRegistration is on by default)
    // publishes a PasskeyRegistration audit event and emails the owner. Mail is redirected to Mailpit by the
    // step-up-e2e profile; poll its REST API for the notification (delivery is asynchronous).
    await createPasswordlessUserWithPasskey(page, user);

    await expect
      .poll(
        async () => {
          const response = await page.request.get(
            `http://localhost:8025/api/v1/search?query=${encodeURIComponent(`to:${user.email}`)}`
          );
          if (!response.ok()) return 0;
          const data = await response.json();
          return (data.messages ?? []).filter((m: { Subject?: string }) =>
            (m.Subject ?? '').includes('New passkey added')
          ).length;
        },
        { timeout: 10000 }
      )
      .toBeGreaterThan(0);
  });
});
