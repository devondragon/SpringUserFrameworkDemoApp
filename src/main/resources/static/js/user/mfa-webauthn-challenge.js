/**
 * MFA WebAuthn challenge page — prompts the user to verify with their passkey
 * after initial password authentication when MFA is enabled.
 */
import { showMessage } from '/js/shared.js';
import { isWebAuthnSupported } from '/js/user/webauthn-utils.js';
import { authenticateWithPasskey } from '/js/user/webauthn-authenticate.js';

const BUTTON_LABEL = 'Verify with Passkey';
const BUTTON_ICON_CLASS = 'bi bi-key me-2';

function setButtonReady(btn) {
    btn.textContent = '';
    const icon = document.createElement('i');
    icon.className = BUTTON_ICON_CLASS;
    btn.appendChild(icon);
    btn.appendChild(document.createTextNode(' ' + BUTTON_LABEL));
}

function setButtonLoading(btn) {
    btn.textContent = '';
    const spinner = document.createElement('span');
    spinner.className = 'spinner-border spinner-border-sm me-2';
    btn.appendChild(spinner);
    btn.appendChild(document.createTextNode(' Verifying...'));
}

document.addEventListener('DOMContentLoaded', () => {
    const verifyBtn = document.getElementById('verifyPasskeyBtn');
    const errorEl = document.getElementById('challengeError');

    if (!verifyBtn) return;

    if (!isWebAuthnSupported()) {
        verifyBtn.disabled = true;
        showMessage(errorEl,
            'Your browser does not support passkeys. Please use a different browser or contact support.',
            'alert-danger');
        return;
    }

    verifyBtn.addEventListener('click', async () => {
        verifyBtn.disabled = true;
        setButtonLoading(verifyBtn);
        errorEl.classList.add('d-none');

        try {
            const redirectUrl = await authenticateWithPasskey();
            window.location.href = redirectUrl;
        } catch (error) {
            console.error('MFA WebAuthn challenge failed:', error);
            showMessage(errorEl,
                'Verification failed. Please try again or cancel and sign out.',
                'alert-danger');
            verifyBtn.disabled = false;
            setButtonReady(verifyBtn);
        }
    });
});
