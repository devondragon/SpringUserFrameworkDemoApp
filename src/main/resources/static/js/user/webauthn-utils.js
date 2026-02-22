/**
 * WebAuthn utility functions for base64url encoding/decoding and browser support checks.
 */

/**
 * Get CSRF token from meta tag.
 */
export function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
}

/**
 * Get CSRF header name from meta tag.
 */
export function getCsrfHeaderName() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
}

/**
 * Convert base64url string to ArrayBuffer.
 */
export function base64urlToBuffer(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const padLen = (4 - (base64.length % 4)) % 4;
    const padded = base64 + '='.repeat(padLen);
    const binary = atob(padded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}

/**
 * Convert ArrayBuffer to base64url string.
 */
export function bufferToBase64url(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (const byte of bytes) {
        binary += String.fromCharCode(byte);
    }
    const base64 = btoa(binary);
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

/**
 * Check if WebAuthn is supported in this browser.
 */
export function isWebAuthnSupported() {
    return window.PublicKeyCredential !== undefined &&
        navigator.credentials !== undefined;
}

/**
 * Check if platform authenticator is available (TouchID, FaceID, Windows Hello).
 */
export async function isPlatformAuthenticatorAvailable() {
    if (!isWebAuthnSupported()) {
        return false;
    }
    return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
}

/**
 * Escape HTML to prevent XSS.
 */
export function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
