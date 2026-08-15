package com.digitalsanctuary.spring.demo.web;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

/**
 * Exposes demo-only, non-{@code user.security} template values that cannot come from the framework's
 * {@code ${userSecurity}} view.
 *
 * <p>Specifically {@code devOrLocalProfile}: templates previously used
 * {@code ${@environment.acceptsProfiles('dev','local')}}, which Spring Boot 4.1.0's Thymeleaf 3.1.5
 * rejects as SpEL bean access in the restricted (layout-decorated) expression context. Exposing the
 * boolean as a model attribute keeps the check working as an ordinary variable reference.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class DemoTemplateModelAdvice {

    private final Environment environment;

    /**
     * @return true when the {@code dev} or {@code local} profile is active
     */
    @ModelAttribute("devOrLocalProfile")
    public boolean devOrLocalProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"));
    }
}
