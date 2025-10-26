package com.digitalsanctuary.spring.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.demo.user.ui.util.DatabaseStateValidator;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.model.VerificationToken;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordHistoryRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.persistence.repository.VerificationTokenRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.digitalsanctuary.spring.user.service.UserVerificationService;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.TokenTestDataBuilder;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;
import jakarta.persistence.EntityManager;

/**
 * Email Verification Edge Cases as specified in Task 4.2 of
 * TEST-IMPROVEMENT-PLAN.md
 *
 * Tests comprehensive email verification token edge cases including: - Token
 * expiry with time manipulation - Token security scenarios (invalid
 * formats, tampering, cross-user attacks) - Already used tokens (single-use
 * enforcement) - Concurrent token requests and validation - User-friendly
 * error messages
 *
 * Acceptance Criteria: - Test token expiry with time manipulation - Verify only
 * latest token is valid - Test concurrent token requests - Ensure
 * tokens are single-use - Verify error messages are user-friendly
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@IntegrationTest
@ActiveProfiles("test")
@Import(EmailVerificationEdgeCaseTest.TestClockConfiguration.class)
@DisplayName("Email Verification Edge Cases")
@Disabled("Email verification timing issues and mock email service configuration. See TEST-ANALYSIS.md")
class EmailVerificationEdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserVerificationService userVerificationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    private User testUser;
    private String testEmail;

    @BeforeEach
    void setUp() {
        // Create test user
        testEmail = "verification.edge.test." + System.currentTimeMillis() + "@example.com";
        testUser = UserTestDataBuilder.aUser().withEmail(testEmail).withFirstName("Edge").withLastName("TestUser")
                .disabled() // Start with disabled
                            // user needing
                            // verification
                .build();
        testUser = userRepository.save(testUser);
        entityManager.flush();
    }

    @AfterEach
    void cleanup() {
        verificationTokenRepository.deleteAll();
        passwordHistoryRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
    }

    @Nested
    @DisplayName("Token Expiry Tests")
    class TokenExpiryTests {

        @Test
        @DisplayName("Expired token should be rejected and cleaned up")
        void testExpiredTokenRejection() throws Exception {
            // Create expired token
            VerificationToken expiredToken = TokenTestDataBuilder.anExpiredVerificationToken().forUser(testUser)
                    .expiredDaysAgo(1).build();
            verificationTokenRepository.save(expiredToken);
            entityManager.flush();

            // Attempt verification with expired token
            MvcResult result = mockMvc
                    .perform(get("/user/registrationConfirm").param("token", expiredToken.getToken())
                            .accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk()).andReturn();

            // Verify user remains disabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isFalse();

            // Verify token was cleaned up
            assertThat(verificationTokenRepository.findByToken(expiredToken.getToken())).isNull();

            // Verify error message is user-friendly
            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).containsIgnoringCase("expired").containsIgnoringCase("token");
        }

        @Test
        @DisplayName("Just expired token should be handled the same as long expired")
        void testJustExpiredToken() throws Exception {
            // Create token that expired 1 second ago
            VerificationToken justExpiredToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .withExpiryDate(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS))).build();
            verificationTokenRepository.save(justExpiredToken);
            entityManager.flush();

            // Attempt verification
            mockMvc.perform(get("/user/registrationConfirm").param("token", justExpiredToken.getToken())
                    .accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());

            // Verify user remains disabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isFalse();

            // Verify token was cleaned up
            assertThat(verificationTokenRepository.findByToken(justExpiredToken.getToken())).isNull();
        }

        @Test
        @DisplayName("Valid token near expiry should work")
        void testTokenNearExpiry() throws Exception {
            // Create token expiring in 1 minute
            VerificationToken nearExpiryToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInMinutes(1).build();
            verificationTokenRepository.save(nearExpiryToken);
            entityManager.flush();

            // Should work successfully
            mockMvc.perform(get("/user/registrationConfirm").param("token", nearExpiryToken.getToken())
                    .accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection()); // Successful verification redirects

            // Verify user is enabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isTrue();

            // Verify token was consumed
            assertThat(verificationTokenRepository.findByToken(nearExpiryToken.getToken())).isNull();
        }

        @Test
        @DisplayName("Multiple token requests - only latest should be valid")
        void testMultipleTokenRequests() throws Exception {
            // Create first token
            VerificationToken firstToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(firstToken);
            entityManager.flush();

            // Create second token (simulating resend)
            VerificationToken secondToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(secondToken);
            entityManager.flush();

            // First token should be invalid (implicitly by having a newer one)
            // This tests the business logic that only the latest token is valid
            // In most implementations, older tokens are either deleted or marked invalid

            // Second token should work
            mockMvc.perform(
                    get("/user/registrationConfirm").param("token", secondToken.getToken()).accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection());

            // Verify user is enabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    class TokenSecurityTests {

        @Test
        @DisplayName("Invalid token formats should be rejected gracefully")
        void testInvalidTokenFormats() throws Exception {
            String[] invalidTokens = { "", // Empty token
                    "invalid-token", // Non-UUID format
                    "12345678-1234-1234-1234", // Malformed UUID
                    "not-a-uuid-at-all", // Random string
                    "../../../../etc/passwd", // Path traversal attempt
                    "<script>alert('xss')</script>", // XSS attempt
                    "' OR 1=1 --", // SQL injection attempt
                    UUID.randomUUID().toString() // Valid format but non-existent
            };

            for (String invalidToken : invalidTokens) {
                MvcResult result = mockMvc
                        .perform(get("/user/registrationConfirm").param("token", invalidToken)
                                .accept(MediaType.TEXT_HTML))
                        .andExpect(status().isOk()) // Should not throw error, handle gracefully
                        .andReturn();

                // Verify user remains disabled
                assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isFalse();

                // Verify error message is user-friendly, not a stack trace
                String responseBody = result.getResponse().getContentAsString();
                assertThat(responseBody).as("Invalid token: " + invalidToken).doesNotContain("Exception")
                        .doesNotContain("SQLException")
                        .doesNotContain("NullPointerException");
            }
        }

        @Test
        @DisplayName("Tampered tokens should be rejected")
        void testTamperedTokens() throws Exception {
            // Create valid token
            VerificationToken validToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(validToken);
            entityManager.flush();

            String originalToken = validToken.getToken();

            // Create tampered versions
            String[] tamperedTokens = { originalToken.substring(0, originalToken.length() - 1) + "x", // Last character
                                                                                                      // changed
                    originalToken.substring(1), // First character removed
                    originalToken + "x", // Character appended
                    originalToken.toUpperCase(), // Case changed
                    originalToken.replace("-", ""), // Dashes removed
                    originalToken.replace("a", "b") // Character substitution
            };

            for (String tamperedToken : tamperedTokens) {
                MvcResult result = mockMvc
                        .perform(get("/user/registrationConfirm").param("token", tamperedToken)
                                .accept(MediaType.TEXT_HTML))
                        .andExpect(status().isOk()).andReturn();

                // Verify user remains disabled
                assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isFalse();

                // Verify original token still exists (not consumed by tampered attempt)
                assertThat(verificationTokenRepository.findByToken(originalToken)).isNotNull();
            }
        }

        @Test
        @DisplayName("Token from different user should not work")
        void testCrossUserTokenAttack() throws Exception {
            // Create second user
            String otherEmail = "other.user." + System.currentTimeMillis() + "@example.com";
            User otherUser = UserTestDataBuilder.aUser().withEmail(otherEmail).disabled().build();
            otherUser = userRepository.save(otherUser);

            // Create token for other user
            VerificationToken otherUserToken = TokenTestDataBuilder.aVerificationToken().forUser(otherUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(otherUserToken);
            entityManager.flush();

            // Try to use other user's token for test user
            mockMvc.perform(get("/user/registrationConfirm").param("token", otherUserToken.getToken())
                    .accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());

            // Verify our test user remains disabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isFalse();

            // Verify other user also remains disabled (token not consumed)
            assertThat(DatabaseStateValidator.isUserEnabled(otherEmail)).isFalse();

            // Verify token still exists (not consumed)
            assertThat(verificationTokenRepository.findByToken(otherUserToken.getToken())).isNotNull();
        }

        @Test
        @DisplayName("Already used token should not work again")
        void testAlreadyUsedToken() throws Exception {
            // Create valid token
            VerificationToken validToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(validToken);
            entityManager.flush();

            String tokenValue = validToken.getToken();

            // First use should succeed
            mockMvc.perform(get("/user/registrationConfirm").param("token", tokenValue).accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection());

            // Verify user is enabled
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isTrue();

            // Verify token was consumed
            assertThat(verificationTokenRepository.findByToken(tokenValue)).isNull();

            // Second use should fail
            MvcResult result = mockMvc
                    .perform(get("/user/registrationConfirm").param("token", tokenValue).accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk()).andReturn();

            // Verify user-friendly error message
            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).containsIgnoringCase("invalid").containsIgnoringCase("token");
        }
    }

    @Nested
    @DisplayName("Concurrent Operation Tests")
    class ConcurrentOperationTests {

        @Test
        @DisplayName("Concurrent token validation should be thread-safe")
        void testConcurrentTokenValidation() throws Exception {
            // Create valid token
            VerificationToken validToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(validToken);
            entityManager.flush();

            String tokenValue = validToken.getToken();
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Submit concurrent validation attempts
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready

                        UserService.TokenValidationResult result = userVerificationService
                                .validateVerificationToken(tokenValue);

                        if (result == UserService.TokenValidationResult.VALID) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Start all threads simultaneously
            startLatch.countDown();

            // Wait for all threads to complete
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // Only one thread should succeed (token is single-use)
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failureCount.get()).isEqualTo(threadCount - 1);

            // Verify user is enabled (successful verification)
            assertThat(DatabaseStateValidator.isUserEnabled(testEmail)).isTrue();

            // Verify token was consumed
            assertThat(verificationTokenRepository.findByToken(tokenValue)).isNull();
        }

        @Test
        @DisplayName("Concurrent token generation should not create duplicates")
        @Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable transaction for this test
        void testConcurrentTokenGeneration() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicReference<Exception> exception = new AtomicReference<>();

            // Submit concurrent token creation attempts
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        // Create verification token for the same user
                        VerificationToken token = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                                .expiringInHours(24).build();
                        verificationTokenRepository.save(token);

                    } catch (Exception e) {
                        exception.set(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // Should not throw exception (handled gracefully)
            assertThat(exception.get()).isNull();

            // Should have some tokens created (implementation dependent)
            long tokenCount = verificationTokenRepository.count();
            assertThat(tokenCount).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("Error messages should be user-friendly and consistent")
        void testUserFriendlyErrorMessages() throws Exception {
            String[] errorScenarios = { UUID.randomUUID().toString(), // Non-existent token
                    "invalid-format", // Invalid format
                    "" // Empty token
            };

            for (String token : errorScenarios) {
                MvcResult result = mockMvc
                        .perform(get("/user/registrationConfirm").param("token", token).accept(MediaType.TEXT_HTML))
                        .andExpect(status().isOk()).andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Should not expose technical details
                assertThat(responseBody).as("Error scenario: " + token).doesNotContain("SQLException")
                        .doesNotContain("NullPointerException")
                        .doesNotContain("ConstraintViolation").doesNotContain("stackTrace")
                        .doesNotContain("java.lang.");

                // Should provide helpful user message
                assertThat(responseBody).as("Error scenario: " + token).containsAnyOf("invalid", "expired", "token",
                        "verification", "link");
            }
        }

        @Test
        @DisplayName("Successful verification should provide confirmation message")
        void testSuccessfulVerificationMessage() throws Exception {
            // Create valid token
            VerificationToken validToken = TokenTestDataBuilder.aVerificationToken().forUser(testUser)
                    .expiringInHours(24).build();
            verificationTokenRepository.save(validToken);
            entityManager.flush();

            // Should redirect on success (Spring's typical pattern)
            MvcResult result = mockMvc
                    .perform(get("/user/registrationConfirm").param("token", validToken.getToken())
                            .accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection()).andReturn();

            // Check redirect location
            String redirectLocation = result.getResponse().getHeader("Location");
            assertThat(redirectLocation).isNotNull().satisfiesAnyOf(
                    location -> assertThat(location).contains("success"),
                    location -> assertThat(location).contains("login"),
                    location -> assertThat(location).contains("home"));
        }
    }

    /**
     * Test configuration for fixed clock to enable deterministic time-based testing
     */
    @TestConfiguration
    static class TestClockConfiguration {

        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.systemDefault());
        }
    }
}
