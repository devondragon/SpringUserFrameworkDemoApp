package com.digitalsanctuary.spring.demo.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.digitalsanctuary.spring.user.registration.RegistrationContext;
import com.digitalsanctuary.spring.user.registration.RegistrationDecision;
import com.digitalsanctuary.spring.user.registration.RegistrationSource;

class DomainRegistrationGuardTest {

    private final DomainRegistrationGuard guard = new DomainRegistrationGuard("@example.com");

    @Test
    void formRegistrationWithAllowedDomainIsAllowed() {
        RegistrationContext context = new RegistrationContext("user@example.com", RegistrationSource.FORM, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertTrue(decision.allowed());
    }

    @Test
    void formRegistrationWithDisallowedDomainIsDenied() {
        RegistrationContext context = new RegistrationContext("user@other.com", RegistrationSource.FORM, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertFalse(decision.allowed());
        assertTrue(decision.reason().contains("@example.com"));
    }

    @Test
    void passwordlessRegistrationWithAllowedDomainIsAllowed() {
        RegistrationContext context = new RegistrationContext("user@example.com", RegistrationSource.PASSWORDLESS, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertTrue(decision.allowed());
    }

    @Test
    void passwordlessRegistrationWithDisallowedDomainIsDenied() {
        RegistrationContext context = new RegistrationContext("user@other.com", RegistrationSource.PASSWORDLESS, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertFalse(decision.allowed());
    }

    @Test
    void oauth2RegistrationIsAlwaysAllowed() {
        RegistrationContext context = new RegistrationContext("user@other.com", RegistrationSource.OAUTH2, "google");
        RegistrationDecision decision = guard.evaluate(context);
        assertTrue(decision.allowed());
    }

    @Test
    void oidcRegistrationIsAlwaysAllowed() {
        RegistrationContext context = new RegistrationContext("user@other.com", RegistrationSource.OIDC, "keycloak");
        RegistrationDecision decision = guard.evaluate(context);
        assertTrue(decision.allowed());
    }

    @Test
    void nullEmailIsDenied() {
        RegistrationContext context = new RegistrationContext(null, RegistrationSource.FORM, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertFalse(decision.allowed());
    }

    @Test
    void domainCheckIsCaseInsensitive() {
        RegistrationContext context = new RegistrationContext("user@EXAMPLE.COM", RegistrationSource.FORM, null);
        RegistrationDecision decision = guard.evaluate(context);
        assertTrue(decision.allowed());
    }

    @Test
    void customDomainIsRespected() {
        DomainRegistrationGuard customGuard = new DomainRegistrationGuard("@mycompany.org");
        RegistrationContext allowed = new RegistrationContext("user@mycompany.org", RegistrationSource.FORM, null);
        RegistrationContext denied = new RegistrationContext("user@example.com", RegistrationSource.FORM, null);
        assertTrue(customGuard.evaluate(allowed).allowed());
        assertFalse(customGuard.evaluate(denied).allowed());
    }

    @Test
    void configuredDomainIsCaseInsensitive() {
        DomainRegistrationGuard upperGuard = new DomainRegistrationGuard("@EXAMPLE.COM");
        RegistrationContext context = new RegistrationContext("user@example.com", RegistrationSource.FORM, null);
        assertTrue(upperGuard.evaluate(context).allowed());
    }
}
