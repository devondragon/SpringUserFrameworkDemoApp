package com.digitalsanctuary.spring.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.persistence.repository.VerificationTokenRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integration tests for User API endpoints. Tests the complete flow including JSON serialization, security, and database interactions.
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User API Integration Tests - Fixed")
class UserApiIntegrationTestFixed {

    private static final String API_BASE_PATH = "/user";

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private MailService mailService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String TEST_EMAIL = "test@example.com";

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // Start from a clean slate. Registration (via the API and via UserService.registerNewUserAccount)
        // commits the new user in its own transaction as of 4.4.0 — it does NOT roll back with the test's
        // @Transactional. Without a committed cleanup, the user persisted by one test method collides with
        // the next (UserAlreadyExistException / 409 Conflict).
        deleteTestUserCommitted();

        testUserDto = new UserDto();
        testUserDto.setFirstName("Test");
        testUserDto.setLastName("User");
        testUserDto.setEmail(TEST_EMAIL);
        testUserDto.setPassword("SecurePass123!");
        testUserDto.setMatchingPassword("SecurePass123!");
    }

    @AfterEach
    void tearDown() {
        // Remove the user committed by this test so it cannot leak into other tests.
        deleteTestUserCommitted();
    }

    /**
     * Deletes the test user (and any verification token) in its own committed transaction. A
     * REQUIRES_NEW transaction is required because the class is {@code @Transactional}: a plain delete here
     * would roll back with the test and never actually remove the committed registration row.
     */
    private void deleteTestUserCommitted() {
        // Registration creates the verification token via an @Async listener (the demo app enables @Async on
        // UserDemoApplication), so the token can be written shortly AFTER the registration request returns. A
        // single committed delete can race that write: deleteByUser runs before the token row exists, then the
        // user delete trips FK_VERIFY_USER. Retry the committed cleanup until the async token has settled and
        // the delete succeeds (bounded so a genuine failure still surfaces).
        DataAccessException last = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.executeWithoutResult(status -> {
                    User existing = userRepository.findByEmail(TEST_EMAIL);
                    if (existing != null) {
                        verificationTokenRepository.deleteByUser(existing);
                        userRepository.delete(existing);
                    }
                });
                return;
            } catch (DataAccessException ex) {
                last = ex;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while cleaning up the committed test user", ie);
                }
            }
        }
        throw new IllegalStateException("Failed to delete the committed test user after retries", last);
    }

    @Test
    @DisplayName("Should successfully register new user")
    void shouldRegisterNewUser() throws Exception {
        // When
        MvcResult result = mockMvc
                .perform(post(API_BASE_PATH + "/registration").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserDto)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("If your email address is eligible, you will receive a verification email shortly.")).andReturn();

        // Then - Verify user was created
        User savedUser = userRepository.findByEmail("test@example.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.isEnabled()).isFalse(); // Email verification required
    }

    @Test
    @DisplayName("Should not reveal account existence for duplicate email (anti-enumeration)")
    void shouldNotRevealAccountExistenceForDuplicateEmail() throws Exception {
        // Given - Register first user
        userService.registerNewUserAccount(testUserDto);

        // When - Try to register with same email
        // Then - anti-enumeration: a duplicate email returns the SAME generic 200 success body as a new
        // registration, so a caller cannot distinguish an already-registered address from a new one.
        mockMvc.perform(post(API_BASE_PATH + "/registration").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("If your email address is eligible, you will receive a verification email shortly."));
    }

    @Test
    @DisplayName("Should handle password reset request")
    void shouldHandlePasswordReset() throws Exception {
        // Given - Create a user first
        userService.registerNewUserAccount(testUserDto);

        // When - Request password reset
        UserDto resetRequest = new UserDto();
        resetRequest.setEmail("test@example.com");

        mockMvc.perform(post(API_BASE_PATH + "/resetPassword").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("If account exists, password reset email has been sent!"));
    }

    @Test
    @DisplayName("Should require authentication for update user")
    void shouldRequireAuthForUpdateUser() throws Exception {
        // When - Try to update without authentication
        mockMvc.perform(post(API_BASE_PATH + "/updateUser").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)).with(csrf())).andExpect(status().is3xxRedirection()); // API redirects
                                                                                                                             // unauthenticated users
    }

    @Test
    @DisplayName("Should demonstrate that authenticated endpoints require proper DSUserDetails")
    void shouldDemonstrateAuthRequirement() throws Exception {
        // Given - Create user first
        userService.registerNewUserAccount(testUserDto);

        // When - Try to update without proper authentication
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        updateDto.setEmail("test@example.com");

        // Then - Expect redirect to login (no authentication provided)
        mockMvc.perform(post(API_BASE_PATH + "/updateUser").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)).with(csrf())).andExpect(status().is3xxRedirection());

        // Note: The SpringUserFramework API requires DSUserDetails for authenticated endpoints.
        // Standard @WithMockUser doesn't provide this, so authenticated endpoints would need
        // custom test configuration or actual authentication flow to test properly.
    }


    @Test
    @DisplayName("Should require CSRF token for all POST requests")
    void shouldRequireCsrfToken() throws Exception {
        // When - No CSRF token
        mockMvc.perform(
                post(API_BASE_PATH + "/registration").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(testUserDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate required fields on registration")
    void shouldValidateRequiredFields() throws Exception {
        // Given - Invalid user data (missing required fields)
        UserDto invalidUser = new UserDto();
        invalidUser.setEmail("invalid@example.com");
        // Missing firstName, lastName, password

        // When
        mockMvc.perform(post(API_BASE_PATH + "/registration").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)).with(csrf())).andExpect(status().isBadRequest());
    }
}
