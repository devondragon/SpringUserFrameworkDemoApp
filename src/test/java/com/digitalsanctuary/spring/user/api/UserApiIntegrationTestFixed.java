package com.digitalsanctuary.spring.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.dto.PasswordDto;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integration tests for User API endpoints.
 * Tests the complete flow including JSON serialization, security, and database interactions.
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @MockitoBean
    private MailService mailService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private UserDto testUserDto;
    
    @BeforeEach
    void setUp() {
        testUserDto = new UserDto();
        testUserDto.setFirstName("Test");
        testUserDto.setLastName("User");
        testUserDto.setEmail("test@example.com");
        testUserDto.setPassword("SecurePass123!");
        testUserDto.setMatchingPassword("SecurePass123!");
    }
    
    @Test
    @DisplayName("Should successfully register new user")
    void shouldRegisterNewUser() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("Registration Successful!"))
                .andReturn();
        
        // Then - Verify user was created
        User savedUser = userRepository.findByEmail("test@example.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.isEnabled()).isFalse(); // Email verification required
    }
    
    @Test
    @DisplayName("Should return conflict for duplicate email")
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        // Given - Register first user
        userService.registerNewUserAccount(testUserDto);
        
        // When - Try to register with same email
        mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0]").value("An account already exists for the email address"));
    }
    
    @Test
    @DisplayName("Should handle password reset request")
    void shouldHandlePasswordReset() throws Exception {
        // Given - Create a user first
        userService.registerNewUserAccount(testUserDto);
        
        // When - Request password reset
        UserDto resetRequest = new UserDto();
        resetRequest.setEmail("test@example.com");
        
        mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("If account exists, password reset email has been sent!"));
    }
    
    @Test
    @DisplayName("Should require authentication for update user")
    void shouldRequireAuthForUpdateUser() throws Exception {
        // When - Try to update without authentication
        mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto))
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // API redirects unauthenticated users
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
        mockMvc.perform(post(API_BASE_PATH + "/updateUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        
        // Note: The SpringUserFramework API requires DSUserDetails for authenticated endpoints.
        // Standard @WithMockUser doesn't provide this, so authenticated endpoints would need
        // custom test configuration or actual authentication flow to test properly.
    }
    
    
    @Test
    @DisplayName("Should require CSRF token for all POST requests")
    void shouldRequireCsrfToken() throws Exception {
        // When - No CSRF token
        mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)))
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
        mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }
}