package com.digitalsanctuary.spring.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.dto.PasswordDto;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.DSUserDetails;
import com.digitalsanctuary.spring.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Simplified integration tests for authenticated user API endpoints.
 * Uses manual authentication setup to avoid transaction visibility issues.
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authenticated User API Tests - Simplified")
@Disabled("Authentication setup issues with DSUserDetails. See TEST-ANALYSIS.md")
class AuthenticatedUserApiTestSimplified {

    private static final String API_BASE_PATH = "/user";
    private static final String TEST_USER_EMAIL = "auth.test@example.com";
    private static final String TEST_USER_PASSWORD = "TestPassword123!";
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Value("${user.actuallyDeleteAccount:false}")
    private boolean actuallyDeleteAccount;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();
        
        // Create test user through service to ensure proper setup
        UserDto userDto = new UserDto();
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail(TEST_USER_EMAIL);
        userDto.setPassword(TEST_USER_PASSWORD);
        userDto.setMatchingPassword(TEST_USER_PASSWORD);
        
        testUser = userService.registerNewUserAccount(userDto);
        
        // Enable user manually to bypass email verification
        entityManager.createNativeQuery("UPDATE user_account SET enabled = true WHERE email = :email")
                .setParameter("email", TEST_USER_EMAIL)
                .executeUpdate();
        entityManager.flush();
        
        // Refresh user
        testUser = userRepository.findByEmail(TEST_USER_EMAIL);
    }
    
    /**
     * Creates a RequestPostProcessor that adds DSUserDetails authentication
     */
    private RequestPostProcessor withDSUserDetails() {
        return request -> {
            DSUserDetails userDetails = new DSUserDetails(testUser, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            return user(userDetails).postProcessRequest(request);
        };
    }
    
    @Nested
    @DisplayName("Update User Profile Tests")
    class UpdateUserTests {
        
        @Test
        @DisplayName("Should successfully update user profile when authenticated")
        void shouldUpdateUserProfile() throws Exception {
            // Given
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("Updated");
            updateRequest.setLastName("Name");
            updateRequest.setEmail(TEST_USER_EMAIL);
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(0));
            
            // Verify database update
            User updatedUser = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
            assertThat(updatedUser.getLastName()).isEqualTo("Name");
        }
        
        @Test
        @DisplayName("Should update profile with special characters in names")
        void shouldUpdateProfileWithSpecialCharacters() throws Exception {
            // Given
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("José-María");
            updateRequest.setLastName("O'Connor-Smith");
            updateRequest.setEmail(TEST_USER_EMAIL);
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            
            // Verify special characters are preserved
            User updatedUser = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(updatedUser.getFirstName()).isEqualTo("José-María");
            assertThat(updatedUser.getLastName()).isEqualTo("O'Connor-Smith");
        }
        
        @Test
        @DisplayName("Should return unauthorized when not authenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            // Given
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("Updated");
            updateRequest.setLastName("Name");
            updateRequest.setEmail(TEST_USER_EMAIL);
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
        
        @Test
        @DisplayName("Should require CSRF token")
        void shouldRequireCsrfToken() throws Exception {
            // Given
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("Updated");
            updateRequest.setLastName("Name");
            updateRequest.setEmail(TEST_USER_EMAIL);
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
                    .with(withDSUserDetails()))
                    .andExpect(status().isForbidden());
        }
    }
    
    @Nested
    @DisplayName("Update Password Tests")
    class UpdatePasswordTests {
        
        @Test
        @DisplayName("Should successfully update password with correct old password")
        void shouldUpdatePasswordWithCorrectOldPassword() throws Exception {
            // Given
            PasswordDto passwordDto = new PasswordDto();
            passwordDto.setOldPassword(TEST_USER_PASSWORD);
            passwordDto.setNewPassword("NewPassword123!");
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updatePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordDto))
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(0));
            
            // Verify password was changed
            User updatedUser = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(passwordEncoder.matches("NewPassword123!", updatedUser.getPassword())).isTrue();
            assertThat(passwordEncoder.matches(TEST_USER_PASSWORD, updatedUser.getPassword())).isFalse();
        }
        
        @Test
        @DisplayName("Should return error for incorrect old password")
        void shouldReturnErrorForIncorrectOldPassword() throws Exception {
            // Given
            PasswordDto passwordDto = new PasswordDto();
            passwordDto.setOldPassword("WrongPassword123!");
            passwordDto.setNewPassword("NewPassword123!");
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updatePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordDto))
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(1));
            
            // Verify password was NOT changed
            User updatedUser = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(passwordEncoder.matches(TEST_USER_PASSWORD, updatedUser.getPassword())).isTrue();
        }
        
        @Test
        @DisplayName("Should handle password with special characters")
        void shouldHandlePasswordWithSpecialCharacters() throws Exception {
            // Given
            String complexPassword = "P@$$w0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
            PasswordDto passwordDto = new PasswordDto();
            passwordDto.setOldPassword(TEST_USER_PASSWORD);
            passwordDto.setNewPassword(complexPassword);
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updatePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordDto))
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            
            // Verify complex password works
            User updatedUser = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(passwordEncoder.matches(complexPassword, updatedUser.getPassword())).isTrue();
        }
        
        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            // Given
            PasswordDto passwordDto = new PasswordDto();
            passwordDto.setOldPassword(TEST_USER_PASSWORD);
            passwordDto.setNewPassword("NewPassword123!");
            
            // When & Then
            mockMvc.perform(post(API_BASE_PATH + "/updatePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordDto))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
    
    @Nested
    @DisplayName("Delete Account Tests")
    class DeleteAccountTests {
        
        @Test
        @DisplayName("Should successfully delete/disable account when authenticated")
        void shouldDeleteAccountWhenAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(delete(API_BASE_PATH + "/deleteAccount")
                    .with(csrf())
                    .with(withDSUserDetails()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account Deleted"));
            
            // Verify account handling based on configuration
            if (actuallyDeleteAccount) {
                // Should be deleted
                assertThat(userRepository.findByEmail(TEST_USER_EMAIL)).isNull();
            } else {
                // Should be disabled
                User disabledUser = userRepository.findByEmail(TEST_USER_EMAIL);
                assertThat(disabledUser).isNotNull();
                assertThat(disabledUser.isEnabled()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should return unauthorized when not authenticated")
        void shouldReturnUnauthorizedForDeleteAccount() throws Exception {
            // When & Then
            mockMvc.perform(delete(API_BASE_PATH + "/deleteAccount")
                    .with(csrf()))
                    .andExpect(status().isForbidden());
            
            // Verify account still exists and is enabled
            User user = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(user).isNotNull();
            assertThat(user.isEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("Should require CSRF token for delete")
        void shouldRequireCsrfForDelete() throws Exception {
            // When & Then
            mockMvc.perform(delete(API_BASE_PATH + "/deleteAccount")
                    .with(withDSUserDetails()))
                    .andExpect(status().isForbidden());
            
            // Verify account was NOT deleted
            User user = userRepository.findByEmail(TEST_USER_EMAIL);
            assertThat(user).isNotNull();
            assertThat(user.isEnabled()).isTrue();
        }
    }
}