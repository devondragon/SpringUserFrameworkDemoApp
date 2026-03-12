package com.digitalsanctuary.spring.demo.registration;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.digitalsanctuary.spring.user.registration.RegistrationContext;
import com.digitalsanctuary.spring.user.registration.RegistrationDecision;
import com.digitalsanctuary.spring.user.registration.RegistrationGuard;
import com.digitalsanctuary.spring.user.registration.RegistrationSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Sample {@link RegistrationGuard} that restricts form and passwordless registration to
 * {@code @example.com} email addresses while allowing all OAuth2/OIDC registrations.
 *
 * <p>This guard is only active when the {@code registration-guard} Spring profile is enabled.
 * To try it out, add {@code registration-guard} to your active profiles:</p>
 *
 * <pre>
 * ./gradlew bootRun --args='--spring.profiles.active=local,registration-guard'
 * </pre>
 *
 * <p>See the
 * <a href="https://github.com/devondragon/SpringUserFramework/blob/main/REGISTRATION-GUARD.md">
 * Registration Guard documentation</a> for the full SPI reference.</p>
 *
 * @see RegistrationGuard
 * @see RegistrationContext
 * @see RegistrationDecision
 */
@Slf4j
@Component
@Profile("registration-guard")
public class DomainRegistrationGuard implements RegistrationGuard {

    private static final String ALLOWED_DOMAIN = "@example.com";

    @Override
    public RegistrationDecision evaluate(RegistrationContext context) {
        log.debug("Evaluating registration for email: {}, source: {}, provider: {}",
                context.email(), context.source(), context.providerName());

        // Allow all OAuth2/OIDC registrations regardless of email domain
        if (context.source() == RegistrationSource.OAUTH2
                || context.source() == RegistrationSource.OIDC) {
            log.debug("Allowing {} registration for: {}", context.source(), context.email());
            return RegistrationDecision.allow();
        }

        // For form/passwordless, restrict to the allowed domain
        if (context.email() != null && context.email().toLowerCase(Locale.ROOT).endsWith(ALLOWED_DOMAIN)) {
            log.debug("Allowing registration for approved domain: {}", context.email());
            return RegistrationDecision.allow();
        }

        log.info("Denied registration for: {} (domain not allowed)", context.email());
        return RegistrationDecision.deny(
                "Registration is restricted to " + ALLOWED_DOMAIN + " email addresses.");
    }
}
