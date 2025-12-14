package com.digitalsanctuary.spring.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.digitalsanctuary.spring.user.api.config.ApiTestConfiguration;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.persistence.model.PasswordResetToken;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.model.VerificationToken;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordHistoryRepository;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordResetTokenRepository;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.persistence.repository.VerificationTokenRepository;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for User REST API endpoints.
 *
 * This test class verifies the complete API behavior including: - User
 * registration with validation - Password reset flow - User profile updates -
 * Password changes - Account deletion - Security (CSRF, authentication) - Error
 * handling
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(ApiTestConfiguration.class)
@ContextConfiguration(classes = com.digitalsanctuary.spring.demo.UserDemoApplication.class)
@DisplayName("User API Integration Tests")
@org.junit.jupiter.api.Disabled("This test deletes all users and roles, breaking the application")
class UserApiIntegrationTest {

    private static final String API_BASE_PATH = "/user";

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @MockitoBean
    private MailService mailService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clean up
        verificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        passwordHistoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create default role
        userRole = new Role("ROLE_USER");
        userRole = roleRepository.save(userRole);
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register new user with valid data")
        void shouldRegisterNewUser() throws Exception {
            // Given
            String requestBody = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john.doe@example.com",
                        "password": "SecurePass123!",
                        "matchingPassword": "SecurePass123!"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration Successful!"));

            // Verify user created
            User user = userRepository.findByEmail("john.doe@example.com");
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo("John");
            assertThat(user.getLastName()).isEqualTo("Doe");
            assertThat(user.isEnabled()).isFalse(); // Email verification required

            // Verify verification token created
            VerificationToken token = verificationTokenRepository.findByUser(user);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("Should return conflict when email already exists")
        void shouldReturnConflictForDuplicateEmail() throws Exception {
            // Given
            User existingUser = UserTestDataBuilder.aUser().withEmail("existing@example.com").build();
            userRepository.save(existingUser);

            String requestBody = """
                    {
                        "firstName": "Jane",
                        "lastName": "Doe",
                        "email": "existing@example.com",
                        "password": "SecurePass123!",
                        "matchingPassword": "SecurePass123!"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("An account already exists for the email address"))
                    .andExpect(jsonPath("$.code").value(2));
        }

        @Test
        @DisplayName("Should return bad request for missing required fields")
        void shouldReturnBadRequestForMissingFields() throws Exception {
            // Given - Missing email
            String requestBody = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "password": "SecurePass123!",
                        "matchingPassword": "SecurePass123!"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request for password mismatch")
        void shouldReturnBadRequestForPasswordMismatch() throws Exception {
            // Given
            String requestBody = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john@example.com",
                        "password": "SecurePass123!",
                        "matchingPassword": "DifferentPass123!"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle very long input values")
        void shouldHandleVeryLongInputValues() throws Exception {
            // Given
            String longString = "a".repeat(300);
            String requestBody = String.format("""
                    {
                        "firstName": "%s",
                        "lastName": "%s",
                        "email": "long@example.com",
                        "password": "SecurePass123!",
                        "matchingPassword": "SecurePass123!"
                    }
                    """, longString, longString);

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then - Should either succeed with truncation or fail with validation
            // The actual behavior depends on the validation constraints
            result.andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should reject SQL injection attempts")
        void shouldRejectSqlInjectionAttempts() throws Exception {
            // Given
            String requestBody = """
                    {
                        "firstName": "John'; DROP TABLE users; --",
                        "lastName": "Doe",
                        "email": "test@example.com",
                        "password": "SecurePass123!",
                        "matchingPassword": "SecurePass123!"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then - Should succeed but safely handle the input
            result.andExpect(status().isOk());

            // Verify the malicious input was safely stored
            User user = userRepository.findByEmail("test@example.com");
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo("John'; DROP TABLE users; --");
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should initiate password reset for existing user")
        void shouldInitiatePasswordResetForExistingUser() throws Exception {
            // Given
            User user = UserTestDataBuilder.aVerifiedUser().withEmail("reset@example.com").build();
            userRepository.save(user);

            String requestBody = """
                    {
                        "email": "reset@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("If account exists, password reset email has been sent!"));

            // Verify token created
            PasswordResetToken token = passwordResetTokenRepository.findByUser(user);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("Should return same response for non-existent email (security)")
        void shouldReturnSameResponseForNonExistentEmail() throws Exception {
            // Given
            String requestBody = """
                    {
                        "email": "nonexistent@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then - Same response as for existing user
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("If account exists, password reset email has been sent!"));

            // Verify no token created
            assertThat(passwordResetTokenRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Resend Registration Token Tests")
    class ResendTokenTests {

        @Test
        @DisplayName("Should resend token for unverified user")
        void shouldResendTokenForUnverifiedUser() throws Exception {
            // Given
            User user = UserTestDataBuilder.aUser().withEmail("unverified@example.com").build();
            user.setEnabled(false);
            user = userRepository.save(user);

            String requestBody = """
                    {
                        "email": "unverified@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(
                    post(API_BASE_PATH + "/resendRegistrationToken").contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Verification Email Resent Successfully!"));
        }

        @Test
        @DisplayName("Should return error for already verified user")
        void shouldReturnErrorForVerifiedUser() throws Exception {
            // Given
            User user = UserTestDataBuilder.aVerifiedUser().withEmail("verified@example.com").build();
            userRepository.save(user);

            String requestBody = """
                    {
                        "email": "verified@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(
                    post(API_BASE_PATH + "/resendRegistrationToken").contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Account is already verified."));
        }
    }

    @Nested
    @DisplayName("Authenticated User Tests")
    class AuthenticatedUserTests {

        private User testUser;

        @BeforeEach
        void setUpUser() {
            testUser = UserTestDataBuilder.aVerifiedUser().withEmail("auth@example.com").withFirstName("Original")
                    .withLastName("Name")
                    .withPassword("OldPassword123!").build();
            testUser = userRepository.save(testUser);
        }

        @Test
        @DisplayName("Should update user profile when authenticated")
        @WithUserDetails("auth@example.com")
        void shouldUpdateUserProfile() throws Exception {
            // Given
            String requestBody = """
                    {
                        "firstName": "Updated",
                        "lastName": "Username",
                        "email": "auth@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

            // Verify user updated
            User updated = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(updated.getFirstName()).isEqualTo("Updated");
            assertThat(updated.getLastName()).isEqualTo("Username");
        }

        @Test
        @DisplayName("Should return unauthorized when not authenticated")
        void shouldReturnUnauthorizedForUpdateUser() throws Exception {
            // Given
            String requestBody = """
                    {
                        "firstName": "Updated",
                        "lastName": "Username",
                        "email": "auth@example.com"
                    }
                    """;

            // When
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should update password with correct old password")
        @WithUserDetails("auth@example.com")
        void shouldUpdatePasswordWithCorrectOldPassword() throws Exception {
            // Given
            String requestBody = """
                    {
                        "oldPassword": "OldPassword123!",
                        "newPassword": "NewSecurePass456!"
                    }
                    """;

            // When
            ResultActions result = mockMvc
                    .perform(post(API_BASE_PATH + "/updatePassword").contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return error for incorrect old password")
        @WithUserDetails("auth@example.com")
        void shouldReturnErrorForIncorrectOldPassword() throws Exception {
            // Given
            String requestBody = """
                    {
                        "oldPassword": "WrongPassword123!",
                        "newPassword": "NewSecurePass456!"
                    }
                    """;

            // When
            ResultActions result = mockMvc
                    .perform(post(API_BASE_PATH + "/updatePassword").contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(1));
        }

        @Test
        @DisplayName("Should delete account when authenticated")
        @WithUserDetails("auth@example.com")
        void shouldDeleteAccountWhenAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete(API_BASE_PATH + "/deleteAccount").with(csrf()));

            // Then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account Deleted"));

            // Verify user disabled (soft delete by default)
            User deleted = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(deleted.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should require CSRF token for POST requests")
        void shouldRequireCsrfTokenForPost() throws Exception {
            // Given
            String requestBody = """
                    {
                        "email": "test@example.com"
                    }
                    """;

            // When - No CSRF token
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should require CSRF token for DELETE requests")
        @WithMockUser
        void shouldRequireCsrfTokenForDelete() throws Exception {
            // When - No CSRF token
            ResultActions result = mockMvc.perform(delete(API_BASE_PATH + "/deleteAccount"));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should accept valid CSRF token")
        void shouldAcceptValidCsrfToken() throws Exception {
            // Given
            String requestBody = """
                    {
                        "email": "test@example.com"
                    }
                    """;

            // When - With CSRF token
            ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody).with(csrf()));

            // Then
            result.andExpect(status().isOk());
        }
    }
}
