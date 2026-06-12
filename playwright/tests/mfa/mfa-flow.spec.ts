import { test, expect, generateTestUser, createAndLoginUser } from '../../src/fixtures';

/**
 * Full MFA flow E2E test using Chromium's CDP WebAuthn virtual authenticator.
 *
 * Requires the app to run with MFA enabled:
 *   APP_PROFILES=local,playwright-test,mfa npx playwright test --project=chromium-mfa
 * (or start the server yourself with those profiles; the mfa profile must come last so its
 * overrides win).
 *
 * Tagged @mfa-enabled so the default projects skip it — their specs assume MFA is off.
 */
test.describe('MFA Full Flow @mfa-enabled', () => {
  test('password login requires passkey verification before reaching protected pages', async ({
    page,
    testApiClient,
    cleanupEmails,
  }) => {
    const user = generateTestUser('mfa-e2e');
    cleanupEmails.push(user.email);

    // Set up a virtual authenticator before any WebAuthn ceremony. automaticPresenceSimulation
    // auto-approves create()/get() prompts so no human touch is needed.
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

    // Password login leaves the user partially authenticated: PASSWORD satisfied, WEBAUTHN missing.
    await createAndLoginUser(page, testApiClient, user);

    const partialStatus = await (await page.request.get('/user/mfa/status')).json();
    expect(partialStatus.data.satisfiedFactors).toContain('PASSWORD');
    expect(partialStatus.data.missingFactors).toContain('WEBAUTHN');
    expect(partialStatus.data.fullyAuthenticated).toBe(false);

    // Any protected page redirects to the challenge page (and must not redirect-loop).
    await page.goto('/user/update-user.html');
    await expect(page).toHaveURL(/\/user\/mfa\/webauthn-challenge\.html/);
    await expect(page.locator('#verifyPasskeyBtn')).toBeVisible();

    // Enroll the user's first passkey. The mfa profile unprotects the enrollment endpoints so a
    // partially-authenticated user can register; the virtual authenticator answers the ceremony.
    await page.evaluate(async () => {
      const { registerPasskey } = await import('/js/user/webauthn-register.js');
      await registerPasskey('e2e-virtual-passkey');
    });

    // Complete the challenge with the freshly enrolled passkey.
    await page.locator('#verifyPasskeyBtn').click();
    await page.waitForURL((url) => !url.pathname.includes('webauthn-challenge'), {
      timeout: 15000,
    });

    // Both factors satisfied now.
    const fullStatus = await (await page.request.get('/user/mfa/status')).json();
    expect(fullStatus.data.fullyAuthenticated).toBe(true);
    expect(fullStatus.data.missingFactors).toEqual([]);

    // Protected pages are reachable again.
    await page.goto('/user/update-user.html');
    await expect(page).toHaveURL(/\/user\/update-user\.html/);

    // And the profile page renders the MFA badges from the status endpoint's data envelope.
    const badges = page.locator('#mfaStatusBadges');
    await expect(badges).toContainText('MFA Active');
    await expect(badges).toContainText('Fully Authenticated');
    await expect(badges).not.toContainText('Additional Factor Required');
  });
});
