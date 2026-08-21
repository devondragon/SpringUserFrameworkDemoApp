package com.digitalsanctuary.spring.demo.controller;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the current session's CSRF token to authenticated, same-origin JavaScript.
 *
 * <p>
 * The step-up ceremony (SUF-02) re-runs {@code /login/webauthn} while the user is already logged in. Spring Security's default
 * session-based CSRF repository rotates the token on that authentication success, but the page is not reloaded, so its {@code <meta>}
 * tags still carry the pre-ceremony token. {@code step-up.js} calls this endpoint afterwards to pick up the rotated token before it
 * retries the original request. See {@code src/main/resources/static/js/user/step-up.js}.
 * </p>
 *
 * <p>
 * This is a GET (no CSRF required to call it) and leans on Spring's {@code CsrfToken} argument resolver, which materializes the
 * deferred token from the request. It returns only to the caller's own session, so it discloses nothing an attacker could not already
 * obtain from a rendered page in that same session.
 * </p>
 */
@RestController
public class CsrfController {

    /**
     * Returns the current CSRF token along with the header and parameter names the client should send it under.
     *
     * @param token the session's CSRF token, resolved by Spring
     * @return a JSON object with {@code token}, {@code headerName}, and {@code parameterName}
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName(), "parameterName", token.getParameterName());
    }
}
