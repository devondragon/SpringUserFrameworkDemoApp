package com.digitalsanctuary.spring.user.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Example of properly disabling tests with meaningful documentation
 */
public class DisabledTestExample {
    
    @Test
    @Disabled("FRAMEWORK-ISSUE: Spring Security returns empty 401 response instead of JSON with error message. " +
              "Expected: {\"success\":false,\"message\":\"User Not Logged In!\"} " +
              "Actual: Empty response with 401 status. " +
              "See: https://github.com/devondragon/SpringUserFramework/issues/XXX")
    void testRequiringJsonErrorResponse() {
        // Test expects JSON error but gets empty 401
    }
    
    @Test
    @Disabled("TEST-INFRA: Requires OAuth2 mock server setup. " +
              "Test validates OAuth2 flow but no mock authorization server configured. " +
              "TODO: Add spring-security-test OAuth2 mock support")
    void testOAuth2Flow() {
        // Test needs OAuth2 infrastructure
    }
    
    @Test
    @Disabled("TIMING-ISSUE: Audit logger writes asynchronously causing race condition. " +
              "Test expects immediate audit log entry but async write may not complete. " +
              "Consider: Add synchronous mode for tests or await mechanism")
    void testAuditLogging() {
        // Test has timing issues with async operations
    }
}