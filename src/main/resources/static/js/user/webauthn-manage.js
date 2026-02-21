/**
 * WebAuthn credential management (list, rename, delete) for the user profile page.
 */
import { getCsrfToken, getCsrfHeaderName, isWebAuthnSupported, escapeHtml } from '/js/user/webauthn-utils.js';
import { registerPasskey } from '/js/user/webauthn-register.js';
import { showMessage } from '/js/shared.js';

const csrfHeader = getCsrfHeaderName();
const csrfToken = getCsrfToken();

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
                        Created: ${new Date(cred.created).toLocaleDateString()}
                        ${cred.lastUsed ? ' | Last used: ' + new Date(cred.lastUsed).toLocaleDateString() : ' | Never used'}
                    </small>
                    <br>
                    ${cred.backupEligible
                        ? '<span class="badge bg-success">Synced</span>'
                        : '<span class="badge bg-warning text-dark">Device-bound</span>'}
                </div>
                <div class="flex-shrink-0">
                    <button class="btn btn-sm btn-outline-secondary me-1" onclick="window.renamePasskey('${escapeHtml(cred.id)}', '${escapeHtml(cred.label || '')}')">
                        <i class="bi bi-pencil"></i> Rename
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="window.deletePasskey('${escapeHtml(cred.id)}')">
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

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('renamePasskeyModal'));
    modal.show();

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
                const data = await response.json();
                throw new Error(data.message || 'Failed to rename passkey');
            }

            modal.hide();
            if (globalMessage) {
                showMessage(globalMessage, 'Passkey renamed successfully.', 'alert-success');
            }
            loadPasskeys();
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
            const data = await response.json();
            throw new Error(data.message || 'Failed to delete passkey');
        }

        if (globalMessage) {
            showMessage(globalMessage, 'Passkey deleted successfully.', 'alert-success');
        }
        loadPasskeys();
    } catch (error) {
        console.error('Failed to delete passkey:', error);
        if (globalMessage) {
            showMessage(globalMessage, error.message, 'alert-danger');
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
        loadPasskeys();
    } catch (error) {
        console.error('Registration error:', error);
        if (globalMessage) {
            showMessage(globalMessage, 'Failed to register passkey: ' + error.message, 'alert-danger');
        }
    }
}

// Expose to global scope for onclick handlers in the credential list
window.renamePasskey = renamePasskey;
window.deletePasskey = deletePasskey;

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    const passkeySection = document.getElementById('passkey-section');
    if (!passkeySection) return;

    if (!isWebAuthnSupported()) {
        passkeySection.innerHTML = '<div class="alert alert-warning">Your browser does not support passkeys.</div>';
        return;
    }

    // Wire up register button
    const registerBtn = document.getElementById('registerPasskeyBtn');
    if (registerBtn) {
        registerBtn.addEventListener('click', handleRegisterPasskey);
    }

    // Load existing passkeys
    loadPasskeys();
});
