/**
 * WebAuthn passkey authentication (login).
 */
import { getCsrfToken, getCsrfHeaderName, base64urlToBuffer, bufferToBase64url } from '/js/user/webauthn-utils.js';

/**
 * Authenticate with passkey (discoverable credential / usernameless).
 */
export async function authenticateWithPasskey() {
    const csrfHeader = getCsrfHeaderName();
    const csrfToken = getCsrfToken();

    // 1. Request authentication options (challenge) from Spring Security
    const optionsResponse = await fetch('/webauthn/authenticate/options', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    });

    if (!optionsResponse.ok) {
        throw new Error('Failed to start authentication');
    }

    const options = await optionsResponse.json();

    // 2. Convert base64url fields to ArrayBuffer
    // Spring Security 7 returns options directly (not wrapped in publicKey)
    options.challenge = base64urlToBuffer(options.challenge);

    if (options.allowCredentials) {
        options.allowCredentials = options.allowCredentials.map(cred => ({
            ...cred,
            id: base64urlToBuffer(cred.id)
        }));
    }

    // 3. Call browser WebAuthn API
    const assertion = await navigator.credentials.get({
        publicKey: options
    });

    if (!assertion) {
        throw new Error('No assertion returned from authenticator');
    }

    // 4. Convert assertion to JSON in Spring Security's expected format
    const assertionJSON = {
        id: assertion.id,
        rawId: bufferToBase64url(assertion.rawId),
        credType: assertion.type,
        response: {
            authenticatorData: bufferToBase64url(assertion.response.authenticatorData),
            clientDataJSON: bufferToBase64url(assertion.response.clientDataJSON),
            signature: bufferToBase64url(assertion.response.signature),
            userHandle: assertion.response.userHandle
                ? bufferToBase64url(assertion.response.userHandle)
                : null
        },
        clientExtensionResults: assertion.getClientExtensionResults(),
        authenticatorAttachment: assertion.authenticatorAttachment
    };

    // 5. Send assertion to Spring Security
    const finishResponse = await fetch('/login/webauthn', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(assertionJSON)
    });

    if (!finishResponse.ok) {
        let msg = 'Authentication failed';
        try {
            const data = await finishResponse.json();
            msg = data.message || msg;
        } catch {
            const text = await finishResponse.text();
            if (text) msg = text;
        }
        throw new Error(msg);
    }

    // Spring Security returns { authenticated: true, redirectUrl: "..." }
    const authResponse = await finishResponse.json();
    if (!authResponse || !authResponse.authenticated || !authResponse.redirectUrl) {
        throw new Error('Authentication failed');
    }

    return authResponse.redirectUrl;
}
