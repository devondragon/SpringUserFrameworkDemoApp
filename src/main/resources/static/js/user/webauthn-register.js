/**
 * WebAuthn passkey registration for authenticated users.
 */
import { getCsrfToken, getCsrfHeaderName, base64urlToBuffer, bufferToBase64url } from '/js/user/webauthn-utils.js';

/**
 * Raised when passkey enrollment is refused because the session lacks a recent authentication (SUF-02).
 *
 * With step-up enabled the framework gates POST /webauthn/register on a factor issued within
 * `enrollmentTtlSeconds`, enforced as an authorization rule that returns a plain 403 (not a
 * `step-up-required` 401). Re-running the passkey ceremony cannot satisfy it: the user may have no passkey
 * yet, and enrollment accepts any factor, so the remedy is a fresh login, not a ceremony retry.
 */
export class PasskeyEnrollmentStepUpError extends Error {
    constructor() {
        super('For your security, adding a passkey needs a recent sign-in. Please sign out and sign in again, then add the passkey.');
        this.name = 'PasskeyEnrollmentStepUpError';
    }
}

/**
 * Register a new passkey for the authenticated user.
 */
export async function registerPasskey(labelInput) {
    const credentialName = labelInput || 'My Passkey';
    const csrfHeader = getCsrfHeaderName();
    const csrfToken = getCsrfToken();

    // 1. Request registration options (challenge) from Spring Security
    const optionsResponse = await fetch('/webauthn/register/options', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    });

    if (!optionsResponse.ok) {
        throw new Error('Failed to start registration');
    }

    const options = await optionsResponse.json();

    // 2. Convert base64url fields to ArrayBuffer
    // Spring Security 7 returns options directly (not wrapped in publicKey)
    options.challenge = base64urlToBuffer(options.challenge);
    options.user.id = base64urlToBuffer(options.user.id);

    if (options.excludeCredentials) {
        options.excludeCredentials = options.excludeCredentials.map(cred => ({
            ...cred,
            id: base64urlToBuffer(cred.id)
        }));
    }

    // 3. Call browser WebAuthn API
    const credential = await navigator.credentials.create({
        publicKey: options
    });

    if (!credential) {
        throw new Error('No credential returned from authenticator');
    }

    // 4. Build the registration request in Spring Security's expected format:
    // { publicKey: { credential: {...}, label: "..." } }
    const registrationRequest = {
        publicKey: {
            credential: {
                id: credential.id,
                rawId: bufferToBase64url(credential.rawId),
                type: credential.type,
                response: {
                    attestationObject: bufferToBase64url(credential.response.attestationObject),
                    clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
                    transports: credential.response.getTransports ? credential.response.getTransports() : []
                },
                clientExtensionResults: credential.getClientExtensionResults(),
                authenticatorAttachment: credential.authenticatorAttachment
            },
            label: credentialName
        }
    };

    // 5. Send credential to Spring Security
    const finishResponse = await fetch('/webauthn/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(registrationRequest)
    });

    if (!finishResponse.ok) {
        // The enrollment step-up gate is an authorization rule, so a stale/factorless session is refused here
        // with a bare 403 rather than the step-up-required 401 the other operations return. Surface it as its
        // own error so the UI can tell the user to sign in again instead of offering a passkey retry.
        if (finishResponse.status === 403) {
            throw new PasskeyEnrollmentStepUpError();
        }
        let msg = 'Registration failed';
        try {
            const data = await finishResponse.json();
            msg = data.message || msg;
        } catch {
            const text = await finishResponse.text();
            if (text) msg = text;
        }
        throw new Error(msg);
    }

    return credential;
}
