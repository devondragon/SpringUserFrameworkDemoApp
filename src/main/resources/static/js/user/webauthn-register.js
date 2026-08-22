/**
 * WebAuthn passkey registration for authenticated users.
 */
import { getCsrfToken, getCsrfHeaderName, base64urlToBuffer, bufferToBase64url } from '/js/user/webauthn-utils.js';

/**
 * Raised when passkey enrollment is refused because the session lacks a recent authentication (SUF-02).
 *
 * With step-up enabled the framework gates POST /webauthn/register on a factor issued within
 * `enrollmentTtlSeconds`. Framework versions before 5.3.4 denied with a plain 403; 5.3.4+ denies with a
 * 401 carrying error code `step-up-required` (StepUpEnrollmentAccessDeniedHandler), matching the sibling
 * credential-management endpoints. Re-running the passkey ceremony cannot satisfy it either way: the user
 * may have no passkey yet, and enrollment accepts any factor, so the remedy is a fresh login, not a
 * ceremony retry.
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
        // A stale/factorless session is refused by the enrollment step-up gate. Framework versions before
        // 5.3.4 denied with a bare 403 (an authorization rule with no body); 5.3.4+ denies with a 401
        // carrying error code "step-up-required". Recognize both, and surface it as its own error so the UI
        // can tell the user to sign in again instead of offering a passkey retry.
        let msg = 'Registration failed';
        let errorCode;
        try {
            const data = await finishResponse.json();
            msg = data.message || msg;
            errorCode = data.error;
        } catch {
            const text = await finishResponse.text();
            if (text) msg = text;
        }
        if (finishResponse.status === 403 || (finishResponse.status === 401 && errorCode === 'step-up-required')) {
            throw new PasskeyEnrollmentStepUpError();
        }
        throw new Error(msg);
    }

    return credential;
}
