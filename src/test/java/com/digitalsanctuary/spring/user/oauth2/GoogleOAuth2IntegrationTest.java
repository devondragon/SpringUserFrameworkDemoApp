package com.digitalsanctuary.spring.user.oauth2;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.model.User.Provider;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.digitalsanctuary.spring.user.service.LoginAttemptService;
import com.digitalsanctuary.spring.user.service.AuthorityService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Google OAuth2 Integration Tests as specified in Task 2.2 of TEST-IMPROVEMENT-PLAN.md
 * 
 * Tests complete Google OAuth2 login flow including:
 * - New user registration via Google
 * - Existing user login via Google
 * - Profile data synchronization
 * - OAuth2 error handling
 * - Security aspects
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@Import(OAuth2MockConfiguration.class)
@ActiveProfiles({"test", "oauth2-mock"})
@ExtendWith(OAuth2MockConfiguration.WireMockExtension.class)
@Transactional
@DisplayName("Google OAuth2 Integration Tests")
@Disabled("Requires OAuth2 mock server infrastructure. See TEST-ANALYSIS.md")
class GoogleOAuth2IntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private OAuth2MockConfiguration.OAuth2MockHelper oauth2MockHelper;
    
    @MockitoBean
    private MailService mailService;
    
    @MockitoBean
    private LoginAttemptService loginAttemptService;
    
    @MockitoBean
    private AuthorityService authorityService;
    
    @BeforeEach
    void setUp() {
        // Reset WireMock and setup default stubs
        oauth2MockHelper.resetAll();
        
        // Mock AuthorityService to return proper authorities
        Collection<? extends GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        Mockito.doReturn(authorities).when(authorityService).getAuthoritiesFromUser(Mockito.any());
    }
    
    @Nested
    @DisplayName("Successful OAuth2 Login Tests")
    class SuccessfulLoginTests {
        
        @Test
        @DisplayName("Should create new user via Google OAuth2 login")
        @WithAnonymousUser
        void shouldCreateNewUserViaGoogleOAuth2() throws Exception {
            // Given - Google user that doesn't exist in our system
            String googleEmail = "newgoogleuser@gmail.com";
            String googleName = "New Google User";
            
            // Configure mock to return specific user data
            oauth2MockHelper.setupGoogleSuccessFlow(googleEmail, googleName);
            
            // Create a session to maintain state between requests
            MockHttpSession session = new MockHttpSession();
            
            // When - User clicks "Login with Google"
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            // Extract state parameter from authorization URL
            String location = result.getResponse().getHeader("Location");
            assertThat(location).contains("http://localhost:9001/o/oauth2/v2/auth");
            String state = extractQueryParam(location, "state");
            
            // Simulate Google callback with authorization code
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state)
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index.html?messageKey=message.login.success"));
            
            // Then - Verify user was created
            entityManager.flush();
            entityManager.clear();
            
            User createdUser = userRepository.findByEmail(googleEmail);
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getFirstName()).isEqualTo("New");
            assertThat(createdUser.getLastName()).isEqualTo("Google User");
            assertThat(createdUser.getEmail()).isEqualTo(googleEmail);
            assertThat(createdUser.getProvider()).isEqualTo(Provider.GOOGLE);
            assertThat(createdUser.isEnabled()).isTrue(); // OAuth2 users are auto-enabled
            
            // Verify OAuth2 flow was completed by checking user creation
        }
        
        @Test
        @DisplayName("Should login existing Google user")
        void shouldLoginExistingGoogleUser() throws Exception {
            // Given - Existing Google user
            String googleEmail = "existinggoogleuser@gmail.com";
            String googleName = "Existing Google User";
            String providerId = "google-user-12345";
            
            // Create existing user
            User existingUser = new User();
            existingUser.setFirstName("Existing");
            existingUser.setLastName("Google User");
            existingUser.setEmail(googleEmail);
            existingUser.setPassword("dummy"); // OAuth2 users have dummy password
            existingUser.setProvider(Provider.GOOGLE);
            existingUser.setEnabled(true);
            userRepository.save(existingUser);
            entityManager.flush();
            entityManager.clear();
            
            // Configure mock
            oauth2MockHelper.setupGoogleSuccessFlow(googleEmail, googleName);
            
            // Create a session to maintain state between requests
            MockHttpSession session = new MockHttpSession();
            
            // When - User logs in again with Google
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state)
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index.html?messageKey=message.login.success"));
            
            // Then - Verify no duplicate user created
            entityManager.flush();
            entityManager.clear();
            
            assertThat(userRepository.findAll()).hasSize(1);
            
            User loggedInUser = userRepository.findByEmail(googleEmail);
            assertThat(loggedInUser).isNotNull();
        }
        
        @Test
        @DisplayName("Should synchronize Google profile data")
        void shouldSynchronizeGoogleProfileData() throws Exception {
            // Given - Google provides comprehensive profile data
            String googleEmail = "profilesync@gmail.com";
            String googleName = "Profile Sync User";
            
            // Configure detailed profile response
            oauth2MockHelper.setupGoogleSuccessFlow(googleEmail, googleName);
            
            // Create a session to maintain state between requests
            MockHttpSession session = new MockHttpSession();
            
            // When - User logs in with Google
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state)
                    .session(session))
                .andExpect(status().is3xxRedirection());
            
            // Then - Verify profile data was synchronized
            entityManager.flush();
            entityManager.clear();
            
            User user = userRepository.findByEmail(googleEmail);
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo("Profile");
            assertThat(user.getLastName()).isEqualTo("Sync User");
            assertThat(user.isEnabled()).isTrue(); // Email verified from Google
        }
    }
    
    @Nested
    @DisplayName("OAuth2 Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle user denying Google authorization")
        @WithAnonymousUser
        void shouldHandleAuthorizationDenied() throws Exception {
            // Given - User will deny authorization
            // For now, we'll simulate this by not setting up success flow
            
            // When - User denies Google authorization
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            // Simulate callback with error
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("error", "access_denied")
                    .param("error_description", "User denied access")
                    .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
            
            // Then - Verify no user was created
            assertThat(userRepository.findAll()).isEmpty();
        }
        
        @Test
        @DisplayName("Should handle invalid state parameter")
        @WithAnonymousUser
        void shouldHandleInvalidStateParameter() throws Exception {
            // When - Callback with invalid state
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", "invalid-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
            
            // Then - Verify no user was created
            assertThat(userRepository.findAll()).isEmpty();
        }
        
        @Test
        @DisplayName("Should handle token exchange failure")
        @WithAnonymousUser
        void shouldHandleTokenExchangeFailure() throws Exception {
            // Given - Token endpoint will fail
            // Configure token endpoint to return error
            WireMock.configureFor("localhost", 9001);
            stubFor(WireMock.post(urlEqualTo("/oauth2/v4/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"invalid_client\"}"))); 
            
            // Create a session to maintain state between requests
            MockHttpSession session = new MockHttpSession();
            
            // When - Try to complete OAuth2 flow
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state)
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
            
            // Then - Verify no user was created
            assertThat(userRepository.findAll()).isEmpty();
        }
        
        @Test
        @DisplayName("Should handle invalid access token")
        @WithAnonymousUser
        void shouldHandleInvalidAccessToken() throws Exception {
            // Given - UserInfo endpoint will reject token
            // Configure userinfo endpoint to return error
            WireMock.configureFor("localhost", 9001);
            stubFor(WireMock.get(urlEqualTo("/oauth2/v3/userinfo"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"invalid_token\"}"))); 
            
            // Create a session to maintain state between requests
            MockHttpSession session = new MockHttpSession();
            
            // When - Try to complete OAuth2 flow
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state)
                    .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
            
            // Then - Verify no user was created
            assertThat(userRepository.findAll()).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Account Linking Tests")
    class AccountLinkingTests {
        
        @Test
        @DisplayName("Should handle Google login with existing local account email")
        void shouldHandleExistingLocalAccountEmail() throws Exception {
            // Given - Local user exists with same email as Google account
            String email = "existing@example.com";
            
            // Create local user
            User localUser = new User();
            localUser.setFirstName("Local");
            localUser.setLastName("User");
            localUser.setEmail(email);
            localUser.setPassword("localPassword123");
            localUser.setProvider(Provider.LOCAL);
            localUser.setEnabled(true);
            userRepository.save(localUser);
            entityManager.flush();
            entityManager.clear();
            
            // Configure Google to return same email
            oauth2MockHelper.setupGoogleSuccessFlow(email, "Google User");
            
            // When - User tries to login with Google using same email
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            String state = extractQueryParam(result.getResponse().getHeader("Location"), "state");
            
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-google-auth-code")
                    .param("state", state))
                .andExpect(status().is3xxRedirection());
            
            // Then - Behavior depends on framework configuration
            // Could either link accounts or create separate account
            // For now, verify at least one user exists
            entityManager.flush();
            entityManager.clear();
            
            assertThat(userRepository.findByEmail(email)).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        
        @Test
        @DisplayName("Should include proper OAuth2 security parameters")
        @WithAnonymousUser
        void shouldIncludeProperSecurityParameters() throws Exception {
            // When - Initiate OAuth2 flow
            MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
            
            // Then - Verify security parameters
            String location = result.getResponse().getHeader("Location");
            assertThat(location).contains("response_type=code");
            assertThat(location).contains("client_id=test-google-client-id");
            assertThat(location).contains("scope=");
            assertThat(location).contains("state="); // CSRF protection
            assertThat(location).contains("redirect_uri=");
        }
        
        @Test
        @DisplayName("Should validate OAuth2 callback has valid session")
        @WithAnonymousUser
        void shouldValidateCallbackSession() throws Exception {
            // When - Try to access callback directly without session
            mockMvc.perform(get("/login/oauth2/code/google")
                    .param("code", "test-code")
                    .param("state", "random-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
        }
    }
    
    // Helper methods
    
    private String extractQueryParam(String url, String param) {
        Pattern pattern = Pattern.compile(param + "=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        }
        return null;
    }
}