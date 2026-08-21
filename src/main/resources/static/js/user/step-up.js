/**
 * Step-up re-authentication for sensitive operations (SUF-02).
 *
 * When the server runs with `user.security.stepUp.enabled=true`, credential-altering operations on a
 * passkey-only account require a recent WebAuthn assertion. Without one the server replies HTTP 401 in
 * one of two shapes:
 *
 *   - POST /user/setPassword ............ framework JSONResponse, `{ success: false, code: 6 }`
 *   - passkey delete / rename ........... GenericResponse,        `{ error: "step-up-required" }`
 *
 * The remedy is the same for both: re-run the ordinary passkey login ceremony while still logged in,
 * which refreshes the session's `FACTOR_WEBAUTHN` freshness, then retry the original request once.
 * There is no dedicated step-up endpoint or token; the ceremony is the login flow the app already has.
 *
 * Re-running `/login/webauthn` triggers Spring Security's authentication-success handling: the session
 * id changes (fixation protection) and, with the default session-based repository, the CSRF token
 * rotates. The browser follows the new session cookie automatically, but the page still holds the old
 * CSRF token in its meta tags, so this module refreshes them from `/csrf` before retrying. (The normal
 * login path avoids this by navigating to a fresh page; step-up deliberately stays put.)
 */
import { authenticateWithPasskey } from '/js/user/webauthn-authenticate.js';
import { getCsrfToken, getCsrfHeaderName } from '/js/user/webauthn-utils.js';

/** Raised when the user dismisses the step-up prompt instead of verifying. */
export class StepUpCancelledError extends Error {
    constructor() {
        super('Step-up verification was cancelled.');
        this.name = 'StepUpCancelledError';
    }
}

const DEFAULT_MESSAGE = "This is a sensitive change. Confirm with your passkey to continue.";

/**
 * Run a request that may require step-up. `requestFn` is a thunk returning a `fetch` Promise; it MUST
 * read its CSRF header/token live (e.g. via getCsrfToken/getCsrfHeaderName) rather than closing over a
 * captured value, because the retry runs after the token has been refreshed.
 *
 * If the server asks for step-up, the user is prompted, the passkey ceremony runs, and the request is
 * retried once. The final Response is returned so the caller handles success and error exactly as it
 * would without step-up.
 *
 * @param {() => Promise<Response>} requestFn the original request, callable more than once
 * @param {{ message?: string }} [opts] optional prompt copy
 * @returns {Promise<Response>} the final response (first response if no step-up was needed)
 * @throws {StepUpCancelledError} if the user dismisses the prompt
 * @throws {Error} if the passkey ceremony itself fails (e.g. the authenticator dialog is cancelled)
 */
export async function withStepUp(requestFn, { message } = {}) {
    const response = await requestFn();
    if (!(await isStepUpRequired(response))) {
        return response;
    }

    await confirmStepUp(message || DEFAULT_MESSAGE);
    await authenticateWithPasskey(); // refreshes FACTOR_WEBAUTHN; its redirectUrl is intentionally ignored
    await refreshCsrfToken();
    return requestFn();
}

/**
 * Detect the step-up-required 401 from either endpoint family. Reads a clone so the caller's response
 * body stays unconsumed.
 */
async function isStepUpRequired(response) {
    if (response.status !== 401) {
        return false;
    }
    try {
        const data = await response.clone().json();
        // code 6: setPassword step-up (JSONResponse). error "step-up-required": passkey delete/rename.
        return data.code === 6 || data.error === 'step-up-required';
    } catch {
        return false;
    }
}

/**
 * Pull the current CSRF token from the server and update the page's meta tags, so the retry (and any
 * later request reading getCsrfToken/getCsrfHeaderName) uses the token issued after re-authentication.
 * Best-effort: a failure here just means the retry may surface a CSRF error, which the caller reports.
 */
async function refreshCsrfToken() {
    try {
        const response = await fetch('/csrf', { headers: { 'Accept': 'application/json' } });
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        setMeta('_csrf', data.token);
        setMeta('_csrf_header', data.headerName);
    } catch (error) {
        console.warn('Failed to refresh CSRF token after step-up:', error);
    }
}

function setMeta(name, content) {
    if (content == null) {
        return;
    }
    let meta = document.querySelector(`meta[name="${name}"]`);
    if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute('name', name);
        document.head.appendChild(meta);
    }
    meta.setAttribute('content', content);
}

// --- Step-up prompt modal -------------------------------------------------------------------------

let modalInstance;

const MODAL_HTML = `
<div class="modal fade" id="stepUpModal" tabindex="-1" aria-labelledby="stepUpModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="stepUpModalLabel"><i class="bi bi-shield-lock me-2"></i>Verify it's you</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <p id="stepUpModalMessage" class="mb-0"></p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-primary" id="stepUpVerifyBtn"><i class="bi bi-fingerprint me-1"></i>Verify with passkey</button>
      </div>
    </div>
  </div>
</div>`;

function ensureModal() {
    let el = document.getElementById('stepUpModal');
    if (!el) {
        const template = document.createElement('template');
        template.innerHTML = MODAL_HTML.trim();
        el = template.content.firstElementChild;
        document.body.appendChild(el);
    }
    if (!modalInstance) {
        modalInstance = new bootstrap.Modal(el);
    }
    return el;
}

/**
 * Show the step-up modal and resolve when the user clicks Verify, or reject with StepUpCancelledError
 * when they dismiss it (Cancel button, close icon, backdrop click, or Escape).
 */
function confirmStepUp(message) {
    return new Promise((resolve, reject) => {
        const el = ensureModal();
        el.querySelector('#stepUpModalMessage').textContent = message;
        const verifyBtn = el.querySelector('#stepUpVerifyBtn');

        let verified = false;
        const onVerify = () => {
            verified = true;
            modalInstance.hide();
        };
        const onHidden = () => {
            verifyBtn.removeEventListener('click', onVerify);
            el.removeEventListener('hidden.bs.modal', onHidden);
            if (verified) {
                resolve();
            } else {
                reject(new StepUpCancelledError());
            }
        };
        verifyBtn.addEventListener('click', onVerify);
        el.addEventListener('hidden.bs.modal', onHidden);
        modalInstance.show();
    });
}
