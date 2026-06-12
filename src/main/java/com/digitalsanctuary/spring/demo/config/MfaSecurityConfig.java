package com.digitalsanctuary.spring.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;

/**
 * Enables Spring Security 7's MFA filter support when {@code user.mfa.enabled=true}.
 *
 * <p>
 * {@code @EnableMultiFactorAuthentication} registers a {@code BeanPostProcessor} that sets {@code mfaEnabled} on the
 * authentication filters. With it, a second login in the same session (e.g. a WebAuthn assertion after a password
 * login) <em>merges</em> the new authentication's authorities with the current ones, accumulating
 * {@code FactorGrantedAuthority}s. Without it, the passkey verification <em>replaces</em> the session authentication,
 * dropping the PASSWORD factor — the user can then never satisfy both factors and bounces between the two challenge
 * pages forever.
 * </p>
 *
 * <p>
 * The {@code authorities} attribute is left empty on purpose: the Spring User Framework already configures the
 * required-factor authorization rules from the {@code user.mfa.factors} property.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "user.mfa.enabled", havingValue = "true", matchIfMissing = false)
@EnableMultiFactorAuthentication(authorities = {})
public class MfaSecurityConfig {
}
