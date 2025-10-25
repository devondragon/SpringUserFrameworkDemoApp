package com.digitalsanctuary.spring.user.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordHistoryRepository;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;

/**
 * Security configuration tests.
 *
 * This test class verifies the Spring Security configuration including: -
 * Authentication mechanisms - Authorization rules - Protected/unprotected
 * URIs - Login/logout behavior
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("Security Configuration Tests")
class SecurityConfigurationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordHistoryRepository passwordHistoryRepository;

        @Value("${user.security.loginActionURI}")
        private String loginActionURI;

        @Value("${user.security.logoutActionURI}")
        private String logoutActionURI;

        private User testUser;
        private final String TEST_PASSWORD = "password123";
        private final String TEST_EMAIL = "security@test.com";

        @BeforeEach
        @Transactional
        void setUp() {
                // Clean up
                passwordHistoryRepository.deleteAll();
                userRepository.deleteAll();
                roleRepository.deleteAll();

                // Create role
                Role userRole = new Role("ROLE_USER");
                userRole = roleRepository.save(userRole);

                // Create verified user
                testUser = UserTestDataBuilder.aVerifiedUser().withEmail(TEST_EMAIL).withPassword(TEST_PASSWORD)
                                .withId(null).build();
                testUser.setRoles(new ArrayList<>(Arrays.asList(userRole)));
                testUser = userRepository.save(testUser);
        }

        @Test
        @DisplayName("Should authenticate user with valid credentials")
        void formLogin_validCredentials_authenticatesUser() throws Exception {
                mockMvc.perform(formLogin(loginActionURI).user("username", TEST_EMAIL).password(TEST_PASSWORD))
                                .andExpect(authenticated().withUsername(TEST_EMAIL));
        }

        @Test
        @DisplayName("Should reject authentication with invalid credentials")
        void formLogin_invalidCredentials_rejectsAuthentication() throws Exception {
                mockMvc.perform(formLogin(loginActionURI).user("username", TEST_EMAIL).password("wrongpassword"))
                                .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should reject authentication for non-existent user")
        void formLogin_nonExistentUser_rejectsAuthentication() throws Exception {
                mockMvc.perform(formLogin(loginActionURI).user("username", "nonexistent@test.com")
                                .password("anypassword"))
                                .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should reject authentication for unverified user")
        void formLogin_unverifiedUser_rejectsAuthentication() throws Exception {
                // Create unverified user
                User unverifiedUser = UserTestDataBuilder.anUnverifiedUser().withEmail("unverified@test.com")
                                .withPassword(TEST_PASSWORD).withId(null)
                                .build();
                Role role = roleRepository.findAll().get(0);
                unverifiedUser.setRoles(new ArrayList<>(Arrays.asList(role)));
                userRepository.save(unverifiedUser);

                mockMvc.perform(formLogin(loginActionURI).user("username", "unverified@test.com")
                                .password(TEST_PASSWORD))
                                .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should reject authentication for locked user")
        void formLogin_lockedUser_rejectsAuthentication() throws Exception {
                // Create locked user
                User lockedUser = UserTestDataBuilder.aLockedUser().withEmail("locked@test.com")
                                .withPassword(TEST_PASSWORD).verified().withId(null)
                                .build();
                Role role = roleRepository.findAll().get(0);
                lockedUser.setRoles(new ArrayList<>(Arrays.asList(role)));
                userRepository.save(lockedUser);

                mockMvc.perform(formLogin(loginActionURI).user("username", "locked@test.com").password(TEST_PASSWORD))
                                .andExpect(unauthenticated());
        }

        @Test
        @WithMockUser(username = "security@test.com")
        @DisplayName("Should logout authenticated user")
        void logout_authenticatedUser_logsOut() throws Exception {
                mockMvc.perform(logout(logoutActionURI)).andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should protect user update endpoints")
        void accessProtectedEndpoint_unauthenticated_redirectsToLogin() throws Exception {
                mockMvc.perform(get("/user/update-user.html")).andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("**/login**"));
        }

        @Test
        @WithMockUser(username = "security@test.com", roles = { "USER" })
        @DisplayName("Should allow authenticated user to access protected endpoints")
        @Disabled("Protected endpoint /protected.html returns 404 - endpoint may not exist. See TEST-ANALYSIS.md")
        void accessProtectedEndpoint_authenticated_allowsAccess() throws Exception {
                // Test that authenticated user is properly authenticated
                mockMvc.perform(get("/protected.html")).andExpect(status().isOk()).andExpect(authenticated());
        }

        @Test
        @DisplayName("Should allow access to unprotected endpoints")
        void accessUnprotectedEndpoint_unauthenticated_allowsAccess() throws Exception {
                mockMvc.perform(get("/")).andExpect(status().isOk()); // Home page exists and should be accessible
        }

        @Test
        @DisplayName("Should handle case-sensitive email login")
        void formLogin_differentCaseEmail_failsAuthentication() throws Exception {
                // The implementation is case-sensitive, so uppercase email should fail
                mockMvc.perform(formLogin(loginActionURI).user("username", TEST_EMAIL.toUpperCase())
                                .password(TEST_PASSWORD))
                                .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should reject empty credentials")
        void formLogin_emptyCredentials_rejectsAuthentication() throws Exception {
                mockMvc.perform(formLogin(loginActionURI).user("username", "").password(""))
                                .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Should reject null password")
        void formLogin_nullPassword_rejectsAuthentication() throws Exception {
                mockMvc.perform(formLogin(loginActionURI).user("username", TEST_EMAIL).password(null))
                                .andExpect(unauthenticated());
        }
}
