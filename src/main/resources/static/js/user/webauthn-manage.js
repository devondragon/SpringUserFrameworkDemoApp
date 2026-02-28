/**
 * WebAuthn credential management (list, rename, delete) for the user profile page.
 */
import { getCsrfToken, getCsrfHeaderName, isWebAuthnSupported, escapeHtml } from '/js/user/webauthn-utils.js';
import { registerPasskey } from '/js/user/webauthn-register.js';
import { showMessage } from '/js/shared.js';
import { getAuthMethods, invalidateAuthMethodsCache } from '/js/user/auth-methods.js';

const csrfHeader = getCsrfHeaderName();
const csrfToken = getCsrfToken();
let renameModalInstance;
let removePasswordModalInstance;

/**
 * Load and display user's passkeys.
 */
export async function loadPasskeys() {
    const container = document.getElementById('passkeys-list');
    const globalMessage = document.getElementById('passkeyMessage');
    if (!container) return;

    try {
        const response = await fetch('/user/webauthn/credentials', {
            headers: { [csrfHeader]: csrfToken }
        });

        if (!response.ok) {
            throw new Error('Failed to load passkeys');
        }

        const credentials = await response.json();
        displayCredentials(container, credentials);
    } catch (error) {
        console.error('Failed to load passkeys:', error);
        if (globalMessage) {
            showMessage(globalMessage, 'Failed to load passkeys.', 'alert-danger');
        }
    }
}

/**
 * Format a date string safely, returning 'Unknown' for invalid values.
 */
function formatDate(dateStr) {
    if (!dateStr) return 'Unknown';
    const date = new Date(dateStr);
    return isNaN(date) ? 'Unknown' : date.toLocaleDateString();
}

/**
 * Display credentials in UI.
 */
function displayCredentials(container, credentials) {
    if (credentials.length === 0) {
        container.innerHTML = '<p class="text-muted">No passkeys registered yet.</p>';
        return;
    }

    container.innerHTML = credentials.map(cred => `
        <div class="card mb-2" data-id="${escapeHtml(cred.id)}">
            <div class="card-body d-flex justify-content-between align-items-center py-2">
                <div class="me-3" style="min-width: 0;">
                    <strong class="d-inline-block text-truncate" style="max-width: 100%;">${escapeHtml(cred.label || 'Unnamed Passkey')}</strong>
                    <br>
                    <small class="text-muted">
                        Created: ${formatDate(cred.created)}
                        ${cred.lastUsed ? ' | Last used: ' + formatDate(cred.lastUsed) : ' | Never used'}
                    </small>
                    <br>
                    ${cred.backupEligible
                        ? '<span class="badge bg-success">Synced</span>'
                        : '<span class="badge bg-warning text-dark">Device-bound</span>'}
                </div>
                <div class="flex-shrink-0">
                    <button class="btn btn-sm btn-outline-secondary me-1" data-action="rename" data-id="${escapeHtml(cred.id)}" data-label="${escapeHtml(cred.label || '')}">
                        <i class="bi bi-pencil"></i> Rename
                    </button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${escapeHtml(cred.id)}">
                        <i class="bi bi-trash"></i> Delete
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

/**
 * Show the rename passkey modal.
 */
function renamePasskey(credentialId, currentLabel) {
    const input = document.getElementById('renamePasskeyInput');
    const counter = document.getElementById('renamePasskeyCount');
    const errorEl = document.getElementById('renamePasskeyError');
    const confirmBtn = document.getElementById('confirmRenameButton');

    // Pre-fill with current label
    input.value = currentLabel || '';
    counter.textContent = `${input.value.length} / 64`;
    errorEl.classList.add('d-none');
    input.classList.remove('is-invalid');

    // Show modal (reuse cached instance)
    if (!renameModalInstance) {
        renameModalInstance = new bootstrap.Modal(document.getElementById('renamePasskeyModal'));
    }
    renameModalInstance.show();

    // Focus input when modal is shown
    document.getElementById('renamePasskeyModal').addEventListener('shown.bs.modal', () => {
        input.select();
    }, { once: true });

    // Character counter
    const onInput = () => {
        counter.textContent = `${input.value.length} / 64`;
        if (input.value.trim()) {
            errorEl.classList.add('d-none');
            input.classList.remove('is-invalid');
        }
    };
    input.addEventListener('input', onInput);

    // Submit on Enter key
    const onKeydown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            confirmBtn.click();
        }
    };
    input.addEventListener('keydown', onKeydown);

    // Handle confirm click
    const onConfirm = async () => {
        const newLabel = input.value.trim();
        if (!newLabel) {
            errorEl.textContent = 'Please enter a name.';
            errorEl.classList.remove('d-none');
            input.classList.add('is-invalid');
            return;
        }

        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Saving...';

        const globalMessage = document.getElementById('passkeyMessage');

        try {
            const response = await fetch(`/user/webauthn/credentials/${credentialId}/label`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ label: newLabel })
            });

            if (!response.ok) {
                let msg = 'Failed to rename passkey';
                try {
                    const data = await response.json();
                    msg = data.message || msg;
                } catch {
                    const text = await response.text();
                    if (text) msg = text;
                }
                throw new Error(msg);
            }

            renameModalInstance.hide();
            if (globalMessage) {
                showMessage(globalMessage, 'Passkey renamed successfully.', 'alert-success');
            }
            invalidateAuthMethodsCache();
            loadPasskeys();
            updateAuthMethodsUI();
        } catch (error) {
            console.error('Failed to rename passkey:', error);
            errorEl.textContent = error.message;
            errorEl.classList.remove('d-none');
            input.classList.add('is-invalid');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = 'Save';
        }
    };
    confirmBtn.addEventListener('click', onConfirm);

    // Clean up listeners when modal is hidden
    document.getElementById('renamePasskeyModal').addEventListener('hidden.bs.modal', () => {
        input.removeEventListener('input', onInput);
        input.removeEventListener('keydown', onKeydown);
        confirmBtn.removeEventListener('click', onConfirm);
    }, { once: true });
}

/**
 * Delete a passkey with confirmation.
 */
async function deletePasskey(credentialId) {
    if (!confirm('Are you sure you want to delete this passkey? This action cannot be undone.')) {
        return;
    }

    const globalMessage = document.getElementById('passkeyMessage');

    try {
        const response = await fetch(`/user/webauthn/credentials/${credentialId}`, {
            method: 'DELETE',
            headers: { [csrfHeader]: csrfToken }
        });

        if (!response.ok) {
            let msg = 'Failed to delete passkey';
            try {
                const data = await response.json();
                msg = data.message || msg;
            } catch {
                const text = await response.text();
                if (text) msg = text;
            }
            throw new Error(msg);
        }

        if (globalMessage) {
            showMessage(globalMessage, 'Passkey deleted successfully.', 'alert-success');
        }
        invalidateAuthMethodsCache();
        loadPasskeys();
        updateAuthMethodsUI();
    } catch (error) {
        console.error('Failed to delete passkey:', error);
        if (globalMessage) {
            showMessage(globalMessage, error.message || 'Failed to delete passkey. Please try again.', 'alert-danger');
        }
    }
}

/**
 * Handle register passkey button click.
 */
async function handleRegisterPasskey() {
    const globalMessage = document.getElementById('passkeyMessage');
    const labelInput = document.getElementById('passkeyLabel');
    const label = labelInput ? labelInput.value.trim() : '';

    try {
        await registerPasskey(label || 'My Passkey');
        if (globalMessage) {
            showMessage(globalMessage, 'Passkey registered successfully!', 'alert-success');
        }
        if (labelInput) labelInput.value = '';
        invalidateAuthMethodsCache();
        loadPasskeys();
        updateAuthMethodsUI();
    } catch (error) {
        console.error('Registration error:', error);
        if (globalMessage) {
            showMessage(globalMessage, 'Failed to register passkey. Please try again.', 'alert-danger');
        }
    }
}

/**
 * Update the Authentication Methods UI card with current state.
 */
async function updateAuthMethodsUI() {
    const section = document.getElementById('auth-methods-section');
    if (!section) return;

    try {
        const auth = await getAuthMethods(true);
        section.classList.remove('d-none');

        // Build badges
        const badgesContainer = document.getElementById('auth-method-badges');
        let badges = '';
        if (auth.hasPassword) {
            badges += '<span class="badge bg-primary me-2"><i class="bi bi-lock me-1"></i>Password</span>';
        } else {
            badges += '<span class="badge bg-warning text-dark me-2"><i class="bi bi-unlock me-1"></i>No Password</span>';
        }
        if (auth.hasPasskeys) {
            badges += `<span class="badge bg-success me-2"><i class="bi bi-key me-1"></i>Passkeys (${auth.passkeysCount})</span>`;
        }
        if (auth.provider && auth.provider !== 'LOCAL') {
            badges += `<span class="badge bg-info me-2"><i class="bi bi-cloud me-1"></i>${escapeHtml(auth.provider)}</span>`;
        }
        badgesContainer.innerHTML = badges;

        // Show/hide Remove Password button (only if has password AND passkeys)
        const removeContainer = document.getElementById('removePasswordContainer');
        if (removeContainer) {
            removeContainer.classList.toggle('d-none', !(auth.hasPassword && auth.hasPasskeys));
        }

        // Show/hide Set Password link (only if no password)
        const setContainer = document.getElementById('setPasswordContainer');
        if (setContainer) {
            setContainer.classList.toggle('d-none', auth.hasPassword);
        }

        // Update Change Password link text
        const changePasswordLink = document.getElementById('changePasswordLink');
        if (changePasswordLink) {
            changePasswordLink.textContent = auth.hasPassword ? 'Change Password' : 'Set a Password';
        }
    } catch (error) {
        console.error('Failed to update auth methods UI:', error);
        const section = document.getElementById('auth-methods-section');
        if (section) {
            section.innerHTML = '<div class="alert alert-warning">Unable to load authentication methods.</div>';
        }
    }
}

/**
 * Wire up the Remove Password button and modal.
 */
function initRemovePassword() {
    const removeBtn = document.getElementById('removePasswordBtn');
    const confirmInput = document.getElementById('removePasswordConfirmInput');
    const confirmBtn = document.getElementById('confirmRemovePasswordBtn');
    const errorEl = document.getElementById('removePasswordError');

    if (!removeBtn) return;

    removeBtn.addEventListener('click', () => {
        if (!removePasswordModalInstance) {
            removePasswordModalInstance = new bootstrap.Modal(document.getElementById('removePasswordModal'));
        }
        confirmInput.value = '';
        confirmBtn.disabled = true;
        errorEl.classList.add('d-none');
        removePasswordModalInstance.show();
    });

    // Enable confirm button only when "REMOVE" is typed
    confirmInput.addEventListener('input', () => {
        confirmBtn.disabled = confirmInput.value.trim() !== 'REMOVE';
    });

    confirmBtn.addEventListener('click', async () => {
        if (confirmInput.value.trim() !== 'REMOVE') return;

        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Removing...';
        errorEl.classList.add('d-none');

        try {
            const response = await fetch('/user/webauthn/password', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (!response.ok) {
                let msg = 'Failed to remove password';
                try {
                    const data = await response.json();
                    msg = data.message || msg;
                } catch {
                    const text = await response.text();
                    if (text) msg = text;
                }
                throw new Error(msg);
            }

            removePasswordModalInstance.hide();
            const globalMessage = document.getElementById('globalMessage');
            if (globalMessage) {
                showMessage(globalMessage, 'Password removed successfully. You are now passwordless.', 'alert-success');
            }
            invalidateAuthMethodsCache();
            updateAuthMethodsUI();
        } catch (error) {
            console.error('Failed to remove password:', error);
            errorEl.textContent = error.message;
            errorEl.classList.remove('d-none');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = 'Remove Password';
        }
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    const passkeySection = document.getElementById('passkey-section');
    if (!passkeySection) return;

    if (!isWebAuthnSupported()) {
        passkeySection.innerHTML = '<div class="alert alert-warning">Your browser does not support passkeys.</div>';
        return;
    }

    // Event delegation for credential list actions
    const passkeysList = document.getElementById('passkeys-list');
    if (passkeysList) {
        passkeysList.addEventListener('click', (event) => {
            const button = event.target.closest('button[data-action]');
            if (!button) return;

            const { action, id, label } = button.dataset;
            if (action === 'rename') {
                renamePasskey(id, label);
            } else if (action === 'delete') {
                deletePasskey(id);
            }
        });
    }

    // Wire up register button
    const registerBtn = document.getElementById('registerPasskeyBtn');
    if (registerBtn) {
        registerBtn.addEventListener('click', handleRegisterPasskey);
    }

    // Initialize remove password functionality
    initRemovePassword();

    // Load existing passkeys and auth methods
    loadPasskeys();
    updateAuthMethodsUI();
});
