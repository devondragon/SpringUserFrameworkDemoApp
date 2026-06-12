package com.digitalsanctuary.spring.demo.mfa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;

/**
 * Verifies the demo's MFA challenge flow wiring against the framework's Spring Security 7 MFA support.
 *
 * The critical contract: the WebAuthn challenge page (the configured webauthnEntryPointUri) MUST be reachable by a
 * partially-authenticated user (PASSWORD factor only). If it is not in the unprotected URIs list, the access-denied
 * handler redirects the user to the page they were just denied access to, producing an infinite redirect loop.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "user.mfa.enabled=true",
        "user.mfa.factors=PASSWORD,WEBAUTHN",
        "user.mfa.passwordEntryPointUri=/user/login.html",
        "user.mfa.webauthnEntryPointUri=/user/mfa/webauthn-challenge.html"
})
@DisplayName("MFA Challenge Flow Integration Tests")
class MfaChallengeFlowIntegrationTest {

    private static final String CHALLENGE_PAGE = "/user/mfa/webauthn-challenge.html";

    @Autowired
    private MockMvc mockMvc;

    private static List<GrantedAuthority> passwordOnlyAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));
    }

    private static List<GrantedAuthority> allFactorAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY),
                FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.WEBAUTHN_AUTHORITY));
    }

    @Test
    @DisplayName("partially-authenticated user is redirected to the WebAuthn challenge page")
    void partiallyAuthenticatedUserIsRedirectedToChallengePage() throws Exception {
        mockMvc.perform(get("/api/events").with(user("user@test.com").authorities(passwordOnlyAuthorities())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern(CHALLENGE_PAGE + "**"));
    }

    @Test
    @DisplayName("challenge page is accessible to a partially-authenticated user (no redirect loop)")
    void challengePageIsAccessibleToPartiallyAuthenticatedUser() throws Exception {
        // Regression guard: if the challenge page is not in unprotectedURIs, this returns 302 back to
        // itself and real browsers fail with ERR_TOO_MANY_REDIRECTS.
        mockMvc.perform(get(CHALLENGE_PAGE).with(user("user@test.com").authorities(passwordOnlyAuthorities())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MFA status reports the missing WEBAUTHN factor inside the JSONResponse data envelope")
    void mfaStatusReportsMissingFactorInDataEnvelope() throws Exception {
        // The UI (updateMfaStatusUI in webauthn-manage.js) consumes this exact shape: fields live under
        // $.data, not at the top level.
        mockMvc.perform(get("/user/mfa/status").with(user("user@test.com").authorities(passwordOnlyAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mfaEnabled").value(true))
                .andExpect(jsonPath("$.data.fullyAuthenticated").value(false))
                .andExpect(jsonPath("$.data.satisfiedFactors[0]").value("PASSWORD"))
                .andExpect(jsonPath("$.data.missingFactors[0]").value("WEBAUTHN"));
    }

    @Test
    @DisplayName("fully-authenticated user can access protected endpoints and reports fullyAuthenticated")
    void fullyAuthenticatedUserCanAccessProtectedPages() throws Exception {
        mockMvc.perform(get("/api/events").with(user("user@test.com").authorities(allFactorAuthorities())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/mfa/status").with(user("user@test.com").authorities(allFactorAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyAuthenticated").value(true))
                .andExpect(jsonPath("$.data.missingFactors").isEmpty());
    }
}
