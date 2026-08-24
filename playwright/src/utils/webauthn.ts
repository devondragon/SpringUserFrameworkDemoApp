import type { CDPSession, Page } from '@playwright/test';

/**
 * Shared CDP WebAuthn virtual-authenticator helpers for the E2E specs.
 *
 * Chromium's DevTools protocol can host a virtual authenticator that answers create()/get() ceremonies
 * automatically (automaticPresenceSimulation + isUserVerified), so passkey flows run headless with no
 * human touch. The step-up, MFA, and OIDC specs all need this, so it lives here rather than being copied
 * into each spec.
 */

/**
 * Add one virtual authenticator to an already-enabled CDP session.
 *
 * A second authenticator (transport 'usb') is needed to enroll a second passkey: `excludeCredentials`
 * makes the authenticator that already holds a credential decline a repeat enrollment, and Chrome allows
 * only one 'internal' (platform) authenticator per environment, so the second credential must come from a
 * roaming ('usb') one.
 */
export async function addVirtualAuthenticator(
  cdp: CDPSession,
  transport: 'internal' | 'usb' = 'internal'
): Promise<void> {
  await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport,
      hasResidentKey: true,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true,
    },
  });
}

/**
 * Enable a CDP WebAuthn virtual authenticator that auto-approves create()/get(), so a passkey can be
 * enrolled or asserted without a human touch. Returns the CDP session so callers can add a second
 * authenticator (see addVirtualAuthenticator).
 */
export async function setupVirtualAuthenticator(page: Page): Promise<CDPSession> {
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('WebAuthn.enable');
  await addVirtualAuthenticator(cdp);
  return cdp;
}

/** Read the current credential id list via the management API, using the page's CSRF meta tokens. */
export async function getCredentialIds(page: Page): Promise<string[]> {
  return page.evaluate(async () => {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')!.getAttribute('content')!;
    const csrfToken = document.querySelector('meta[name="_csrf"]')!.getAttribute('content')!;
    const response = await fetch('/user/webauthn/credentials', { headers: { [csrfHeader]: csrfToken } });
    const creds = await response.json();
    return creds.map((c: { id: string }) => c.id);
  });
}
