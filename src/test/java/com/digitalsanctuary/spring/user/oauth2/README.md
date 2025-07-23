# OAuth2 Test Infrastructure Documentation

This directory contains the OAuth2 test infrastructure for the SpringUserFrameworkDemoApp. It provides comprehensive mocking capabilities for testing OAuth2/OIDC authentication flows without requiring real OAuth2 providers.

## Overview

The OAuth2 test infrastructure uses WireMock to simulate OAuth2 providers (Google, Facebook, Keycloak) and provides utilities for testing OAuth2 authentication flows in integration tests.

## Components

### 1. OAuth2TestConfiguration

The main configuration class that sets up WireMock servers for each OAuth2 provider:
- Google OAuth2 on port 9001
- Facebook OAuth2 on port 9002
- Keycloak OIDC on port 9003

**Usage:**
```java
@SpringBootTest
@Import(OAuth2TestConfiguration.class)
@ActiveProfiles({"test", "oauth2test"})
class MyOAuth2Test {
    // Your test code
}
```

### 2. OAuth2TestUtils

Utility class providing helper methods for OAuth2 testing:

- Creating mock OAuth2 authentication tokens
- Generating JWT tokens
- Creating RequestPostProcessors for MockMvc tests

**Example:**
```java
// Test with Google OAuth2 authentication
mockMvc.perform(get("/protected")
    .with(OAuth2TestUtils.googleLogin("user@gmail.com", "Test User")))
    .andExpect(status().isOk());

// Test with Keycloak OIDC authentication
mockMvc.perform(get("/protected")
    .with(OAuth2TestUtils.keycloakLogin("user@example.com", "testuser", "Test User")))
    .andExpect(status().isOk());
```

### 3. MockOAuth2Server

Advanced mock server for simulating various OAuth2 scenarios:

**Supported scenarios:**
- Successful authorization
- User denied access
- Invalid client credentials
- Expired/invalid tokens
- Network timeouts
- Rate limiting

**Example:**
```java
@Autowired
private WireMockServer googleOAuth2MockServer;

@Test
void testOAuth2AccessDenied() {
    MockOAuth2Server mockServer = new MockOAuth2Server.Builder()
        .withWireMockServer(googleOAuth2MockServer)
        .withProvider("google")
        .withPort(9001)
        .build();
    
    mockServer.simulateAuthorizationDenied();
    
    // Test your OAuth2 error handling
}
```

### 4. Test Properties

The `application-oauth2test.properties` file contains all necessary OAuth2 configuration for testing.

## Testing OAuth2 Flows

### 1. Testing Successful OAuth2 Login

```java
@Test
void testGoogleOAuth2Login() {
    // The default configuration simulates successful login
    mockMvc.perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection());
    
    // Verify user was created in database
    User user = userRepository.findByEmail("testuser@gmail.com");
    assertThat(user).isNotNull();
    assertThat(user.getProvider()).isEqualTo(Provider.GOOGLE);
}
```

### 2. Testing OAuth2 Error Scenarios

```java
@Test
void testOAuth2InvalidToken() {
    MockOAuth2Server mockServer = new MockOAuth2Server.Builder()
        .withWireMockServer(googleOAuth2MockServer)
        .withProvider("google")
        .withPort(9001)
        .build();
    
    mockServer.simulateInvalidToken();
    
    // Test error handling
}
```

### 3. Testing Account Linking

```java
@Test
void testLinkGoogleToExistingAccount() {
    // Create existing user
    User existingUser = createTestUser("existing@example.com");
    
    // Simulate Google OAuth2 with same email
    MockOAuth2Server mockServer = new MockOAuth2Server.Builder()
        .withWireMockServer(googleOAuth2MockServer)
        .withProvider("google")
        .withPort(9001)
        .build();
    
    mockServer.simulateSuccessfulAuthorization("existing@example.com", "Existing User");
    
    // Verify account was linked
}
```

## Adding New OAuth2 Providers

To add a new OAuth2 provider:

1. Add WireMock server bean in `OAuth2TestConfiguration`:
```java
@Bean
public WireMockServer newProviderOAuth2MockServer() {
    return new WireMockServer(
        WireMockConfiguration.options()
            .port(9004)
            .usingFilesUnderClasspath("wiremock/newprovider")
    );
}
```

2. Add provider configuration in test properties
3. Add setup method in `OAuth2MockServerManager`
4. Add utility methods in `OAuth2TestUtils`

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 9001-9003 are available
2. **WireMock not starting**: Check logs for binding errors
3. **Authentication failures**: Verify mock server endpoints match configuration

### Debug Logging

Enable debug logging in your test:
```properties
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.com.github.tomakehurst.wiremock=DEBUG
```

### Verifying Mock Interactions

```java
// Verify authorization was requested
assertTrue(mockServer.verifyAuthorizationRequested());

// Check token exchange count
assertEquals(1, mockServer.getUserInfoRequestCount());
```

## Best Practices

1. **Reset mock servers between tests**: Use `@BeforeEach` to reset
2. **Use specific test users**: Avoid conflicts between tests
3. **Test error scenarios**: Don't just test happy paths
4. **Verify database state**: Check user creation/updates
5. **Test security**: Verify proper authentication and authorization

## Example Test Class

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(OAuth2TestConfiguration.class)
@ActiveProfiles({"test", "oauth2test"})
@Transactional
class OAuth2IntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private OAuth2MockServerManager mockServerManager;
    
    @BeforeEach
    void setUp() {
        mockServerManager.resetAll();
    }
    
    @Test
    void testCompleteOAuth2Flow() {
        // Test implementation
    }
}
```