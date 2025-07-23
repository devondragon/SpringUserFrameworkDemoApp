package com.digitalsanctuary.spring.user.oauth2;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Utility class for OAuth2 testing.
 * Provides helper methods for creating mock OAuth2 authentication tokens,
 * users, and JWT tokens for testing OAuth2 flows.
 */
public class OAuth2TestUtils {
    
    private static final SecretKey JWT_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    /**
     * Creates a mock OAuth2 authentication token for Google
     */
    public static OAuth2AuthenticationToken createGoogleOAuth2Token(String email, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-user-" + UUID.randomUUID());
        attributes.put("email", email);
        attributes.put("email_verified", true);
        attributes.put("name", name);
        attributes.put("given_name", name.split(" ")[0]);
        attributes.put("family_name", name.split(" ").length > 1 ? name.split(" ")[1] : "User");
        attributes.put("picture", "https://example.com/photo.jpg");
        attributes.put("locale", "en");
        
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );
        
        return new OAuth2AuthenticationToken(
            oauth2User,
            oauth2User.getAuthorities(),
            "google"
        );
    }
    
    /**
     * Creates a mock OAuth2 authentication token for Facebook
     */
    public static OAuth2AuthenticationToken createFacebookOAuth2Token(String email, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "facebook-user-" + UUID.randomUUID());
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("first_name", name.split(" ")[0]);
        attributes.put("last_name", name.split(" ").length > 1 ? name.split(" ")[1] : "User");
        
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "id"
        );
        
        return new OAuth2AuthenticationToken(
            oauth2User,
            oauth2User.getAuthorities(),
            "facebook"
        );
    }
    
    /**
     * Creates a mock OIDC authentication token for Keycloak
     */
    public static OAuth2AuthenticationToken createKeycloakOidcToken(String email, String username, String name) {
        Map<String, Object> idTokenClaims = new HashMap<>();
        idTokenClaims.put("iss", "http://localhost:9003/realms/test");
        idTokenClaims.put("sub", "keycloak-user-" + UUID.randomUUID());
        idTokenClaims.put("aud", List.of("test-keycloak-client-id"));
        idTokenClaims.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        idTokenClaims.put("iat", Instant.now().getEpochSecond());
        idTokenClaims.put("auth_time", Instant.now().getEpochSecond());
        idTokenClaims.put("nonce", UUID.randomUUID().toString());
        idTokenClaims.put("email", email);
        idTokenClaims.put("email_verified", true);
        idTokenClaims.put("name", name);
        idTokenClaims.put("preferred_username", username);
        idTokenClaims.put("given_name", name.split(" ")[0]);
        idTokenClaims.put("family_name", name.split(" ").length > 1 ? name.split(" ")[1] : "User");
        
        OidcIdToken idToken = new OidcIdToken(
            generateJwtToken(idTokenClaims),
            Instant.now(),
            Instant.now().plusSeconds(300),
            idTokenClaims
        );
        
        OidcUserInfo userInfo = new OidcUserInfo(idTokenClaims);
        
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("SCOPE_openid"),
            new SimpleGrantedAuthority("SCOPE_email"),
            new SimpleGrantedAuthority("SCOPE_profile")
        );
        
        OidcUser oidcUser = new DefaultOidcUser(
            authorities,
            idToken,
            userInfo,
            "preferred_username"
        );
        
        return new OAuth2AuthenticationToken(
            oidcUser,
            authorities,
            "keycloak"
        );
    }
    
    /**
     * Creates a RequestPostProcessor for OAuth2 authentication
     */
    public static RequestPostProcessor oauth2Login(OAuth2AuthenticationToken token) {
        return authentication(token);
    }
    
    /**
     * Creates a RequestPostProcessor for Google OAuth2 login
     */
    public static RequestPostProcessor googleLogin(String email, String name) {
        return oauth2Login(createGoogleOAuth2Token(email, name));
    }
    
    /**
     * Creates a RequestPostProcessor for Facebook OAuth2 login
     */
    public static RequestPostProcessor facebookLogin(String email, String name) {
        return oauth2Login(createFacebookOAuth2Token(email, name));
    }
    
    /**
     * Creates a RequestPostProcessor for Keycloak OIDC login
     */
    public static RequestPostProcessor keycloakLogin(String email, String username, String name) {
        return oauth2Login(createKeycloakOidcToken(email, username, name));
    }
    
    /**
     * Generates a JWT token for testing
     */
    public static String generateJwtToken(Map<String, Object> claims) {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 300000)) // 5 minutes
            .signWith(JWT_KEY)
            .compact();
    }
    
    /**
     * Creates test OAuth2 error response
     */
    public static Map<String, String> createOAuth2ErrorResponse(String error, String errorDescription) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("error_description", errorDescription);
        return errorResponse;
    }
    
    /**
     * Creates test OAuth2 access token response
     */
    public static Map<String, Object> createAccessTokenResponse(String provider) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "test-" + provider + "-access-token");
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        response.put("scope", provider.equals("keycloak") ? "openid email profile" : "email profile");
        
        if (provider.equals("google") || provider.equals("keycloak")) {
            response.put("id_token", generateJwtToken(Map.of(
                "iss", provider.equals("google") ? "https://accounts.google.com" : "http://localhost:9003/realms/test",
                "sub", provider + "-user-" + UUID.randomUUID(),
                "email", "test@example.com",
                "email_verified", true
            )));
        }
        
        if (provider.equals("keycloak")) {
            response.put("refresh_token", "test-keycloak-refresh-token");
        }
        
        return response;
    }
    
    /**
     * Test data for different OAuth2 scenarios
     */
    public static class OAuth2TestData {
        public static final String GOOGLE_USER_EMAIL = "googleuser@gmail.com";
        public static final String GOOGLE_USER_NAME = "Google Test User";
        
        public static final String FACEBOOK_USER_EMAIL = "facebookuser@facebook.com";
        public static final String FACEBOOK_USER_NAME = "Facebook Test User";
        
        public static final String KEYCLOAK_USER_EMAIL = "keycloakuser@example.com";
        public static final String KEYCLOAK_USER_USERNAME = "keycloakuser";
        public static final String KEYCLOAK_USER_NAME = "Keycloak Test User";
        
        public static final String EXISTING_USER_EMAIL = "existing@example.com";
        public static final String NEW_USER_EMAIL = "newuser@example.com";
    }
}