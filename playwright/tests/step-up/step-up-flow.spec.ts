import { test, expect, generateTestUser, TestUser } from '../../src/fixtures';
import type { Page } from '@playwright/test';

/**
 * WebAuthn step-up (SUF-02) E2E, using Chromium's CDP virtual authenticator.
 *
 * Requires the app to run with step-up enabled:
 *   APP_PROFILES=local,playwright-test,step-up npx playwright test --project=chromium-step-up
 * (the step-up profile must come last so its overrides win).
 *
 * Tagged @step-up-enabled so the default and MFA projects skip it: those servers run with step-up off.
 *
 * The account under test is passkey-only (no password). Its session, right after registration, carries
 * a fresh FACTOR_PASSWORD but no FACTOR_WEBAUTHN, so credential-altering operations are refused with 401
 * until the passkey ceremony stamps a fresh WEBAUTHN factor. That makes both paths deterministic with no
 * reliance on the TTL elapsing.
 */

/**
 * Enable a CDP WebAuthn virtual authenticator that auto-approves create()/get() so no human touch is
 * needed. Mirrors playwright/tests/mfa/mfa-flow.spec.ts.
 */
async function setupVirtualAuthenticator(page: Page): Promise<void> {
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('WebAuthn.enable');
  await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport: 'internal',
      hasResidentKey: true,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true,
    },
  });
}

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

/** Read the current credential id list via the management API. */
async function getCredentialIds(page: Page): Promise<string[]> {
  return page.evaluate(async () => {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
    const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
    const response = await fetch('/user/webauthn/credentials', { headers: { [csrfHeader]: csrfToken } });
    const creds = await response.json();
    return creds.map((c: { id: string }) => c.id);
  });
}

test.describe('WebAuthn Step-Up @step-up-enabled', () => {
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
    const [credentialId] = await getCredentialIds(page);
    expect(credentialId).toBeTruthy();

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
});
