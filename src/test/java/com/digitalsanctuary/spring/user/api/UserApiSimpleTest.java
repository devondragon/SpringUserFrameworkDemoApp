package com.digitalsanctuary.spring.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple API test to verify basic functionality works.
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User API Simple Test")
class UserApiSimpleTest {

    private static final String API_BASE_PATH = "/user";
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @MockitoBean
    private MailService mailService;
    
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    
    @BeforeEach
    void setUp() {
        // Clean up - no need to delete users as each test starts fresh
        // Tests should be isolated and not depend on data cleanup
    }
    
    @Test
    @DisplayName("Should successfully register new user")
    void shouldRegisterNewUser() throws Exception {
        // Given
        UserDto userDto = new UserDto();
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setEmail("simple.test@example.com");
        userDto.setPassword("SecurePass123!");
        userDto.setMatchingPassword("SecurePass123!");
        
        // When
        ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
                .with(csrf()));
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("Registration Successful!"));
        
        // Verify user created
        User user = userRepository.findByEmail("simple.test@example.com");
        assertThat(user).isNotNull();
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.isEnabled()).isFalse(); // Email verification required
    }
    
    @Test
    @DisplayName("Should return conflict for duplicate email")
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        // Given - Register first user
        UserDto firstUser = new UserDto();
        firstUser.setFirstName("First");
        firstUser.setLastName("User");
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("Password123!");
        firstUser.setMatchingPassword("Password123!");
        
        mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // When - Try to register with same email
        UserDto duplicateUser = new UserDto();
        duplicateUser.setFirstName("Second");
        duplicateUser.setLastName("User");
        duplicateUser.setEmail("duplicate@example.com");
        duplicateUser.setPassword("Password456!");
        duplicateUser.setMatchingPassword("Password456!");
        
        ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser))
                .with(csrf()));
        
        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0]").value("An account already exists for the email address"));
    }
    
    @Test
    @DisplayName("Should handle password reset request")
    void shouldHandlePasswordReset() throws Exception {
        // Given - Create a user first
        UserDto userData = new UserDto();
        userData.setFirstName("Reset");
        userData.setLastName("User");
        userData.setEmail("reset@example.com");
        userData.setPassword("Password123!");
        userData.setMatchingPassword("Password123!");
        
        mockMvc.perform(post(API_BASE_PATH + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userData))
                .with(csrf()));
        
        // When - Request password reset
        UserDto resetData = new UserDto();
        resetData.setEmail("reset@example.com");
        
        ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetData))
                .with(csrf()));
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("If account exists, password reset email has been sent!"));
    }
    
    @Test
    @DisplayName("Should require CSRF token")
    void shouldRequireCsrfToken() throws Exception {
        // Given
        UserDto resetData = new UserDto();
        resetData.setEmail("test@example.com");
        
        // When - No CSRF token
        ResultActions result = mockMvc.perform(post(API_BASE_PATH + "/resetPassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetData)));
        
        // Then
        result.andExpect(status().isForbidden());
    }
}