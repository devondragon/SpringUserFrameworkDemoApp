/**
 * Auth methods utility - fetches and caches the user's authentication methods.
 */
import { getCsrfToken, getCsrfHeaderName } from '/js/user/webauthn-utils.js';

let cachedAuthMethods = null;

/**
 * Fetch the user's authentication methods from the server.
 * Caches the result for the page load unless forceRefresh is true.
 */
export async function getAuthMethods(forceRefresh = false) {
    if (cachedAuthMethods && !forceRefresh) {
        return cachedAuthMethods;
    }

    const response = await fetch('/user/auth-methods', {
        headers: {
            [getCsrfHeaderName()]: getCsrfToken()
        }
    });

    if (!response.ok) {
        throw new Error('Failed to fetch auth methods');
    }

    const json = await response.json();
    cachedAuthMethods = json.data;
    return cachedAuthMethods;
}

/**
 * Invalidate the cached auth methods so the next call fetches fresh data.
 */
export function invalidateAuthMethodsCache() {
    cachedAuthMethods = null;
}
