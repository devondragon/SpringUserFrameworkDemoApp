package com.digitalsanctuary.spring.demo.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;

/**
 * Security configuration for Test API endpoints. This configuration is only active when the 'playwright-test' profile
 * is enabled.
 * <p>
 * WARNING: This configuration disables CSRF and authentication for test endpoints. It should NEVER be active in
 * production.
 */
@Slf4j
@Configuration
@Profile("playwright-test")
public class TestApiSecurityConfig {

    /**
     * Configure security for test API endpoints. This filter chain has higher priority (lower order number) than the
     * default security configuration, so it will be applied first for /api/test/** paths.
     * <p>
     * SECURITY: Restricts test API access to localhost only to prevent accidental exposure.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain testApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Test API security - CSRF disabled for /api/test/** (localhost only)");

        http.securityMatcher("/api/test/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(request -> {
                            String remoteAddr = request.getRemoteAddr();
                            return "127.0.0.1".equals(remoteAddr) ||
                                   "0:0:0:0:0:0:0:1".equals(remoteAddr) ||
                                   "localhost".equals(remoteAddr);
                        }).permitAll()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
