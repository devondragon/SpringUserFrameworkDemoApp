package com.digitalsanctuary.spring.user.oauth2;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Simplified OAuth2 mock configuration that starts WireMock servers before Spring context.
 * This prevents Spring from trying to connect to real OAuth2 providers during startup.
 */
@TestConfiguration
@Profile("oauth2-mock")
public class OAuth2MockConfiguration {
    
    // Static servers that start before Spring
    private static final WireMockServer GOOGLE_SERVER = new WireMockServer(
        WireMockConfiguration.options().port(9001)
    );
    
    private static final WireMockServer FACEBOOK_SERVER = new WireMockServer(
        WireMockConfiguration.options().port(9002)
    );
    
    private static final WireMockServer KEYCLOAK_SERVER = new WireMockServer(
        WireMockConfiguration.options().port(9003)
    );
    
    static {
        // Start servers immediately
        startAndConfigureServers();
    }
    
    private static void startAndConfigureServers() {
        // Start Google mock server
        if (!GOOGLE_SERVER.isRunning()) {
            GOOGLE_SERVER.start();
            GOOGLE_SERVER.stubFor(get(urlPathMatching("/\\.well-known/openid-configuration"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"issuer\": \"http://localhost:9001\"," +
                        "\"authorization_endpoint\": \"http://localhost:9001/o/oauth2/v2/auth\"," +
                        "\"token_endpoint\": \"http://localhost:9001/oauth2/v4/token\"," +
                        "\"userinfo_endpoint\": \"http://localhost:9001/oauth2/v3/userinfo\"" +
                        "}")));
        }
        
        // Start Facebook mock server
        if (!FACEBOOK_SERVER.isRunning()) {
            FACEBOOK_SERVER.start();
            FACEBOOK_SERVER.stubFor(get(urlPathMatching("/\\.well-known/openid-configuration"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"issuer\": \"http://localhost:9002\"," +
                        "\"authorization_endpoint\": \"http://localhost:9002/dialog/oauth\"," +
                        "\"token_endpoint\": \"http://localhost:9002/oauth/access_token\"," +
                        "\"userinfo_endpoint\": \"http://localhost:9002/me\"" +
                        "}")));
        }
        
        // Start Keycloak mock server
        if (!KEYCLOAK_SERVER.isRunning()) {
            KEYCLOAK_SERVER.start();
            KEYCLOAK_SERVER.stubFor(get(urlEqualTo("/realms/test/.well-known/openid-configuration"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"issuer\": \"http://localhost:9003/realms/test\"," +
                        "\"authorization_endpoint\": \"http://localhost:9003/realms/test/protocol/openid-connect/auth\"," +
                        "\"token_endpoint\": \"http://localhost:9003/realms/test/protocol/openid-connect/token\"," +
                        "\"userinfo_endpoint\": \"http://localhost:9003/realms/test/protocol/openid-connect/userinfo\"," +
                        "\"jwks_uri\": \"http://localhost:9003/realms/test/protocol/openid-connect/certs\"" +
                        "}")));
            
            // Also stub the jwks endpoint
            KEYCLOAK_SERVER.stubFor(get(urlEqualTo("/realms/test/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"keys\": []}")));
        }
    }
    
    @Bean
    public WireMockServer googleOAuth2MockServer() {
        return GOOGLE_SERVER;
    }
    
    @Bean
    public WireMockServer facebookOAuth2MockServer() {
        return FACEBOOK_SERVER;
    }
    
    @Bean
    public WireMockServer keycloakOAuth2MockServer() {
        return KEYCLOAK_SERVER;
    }
    
    @Bean
    public OAuth2MockHelper oauth2MockHelper() {
        return new OAuth2MockHelper(GOOGLE_SERVER, FACEBOOK_SERVER, KEYCLOAK_SERVER);
    }
    
    /**
     * Helper class to manage OAuth2 mock servers in tests
     */
    public static class OAuth2MockHelper {
        private final WireMockServer googleServer;
        private final WireMockServer facebookServer;
        private final WireMockServer keycloakServer;
        
        public OAuth2MockHelper(WireMockServer googleServer, WireMockServer facebookServer, WireMockServer keycloakServer) {
            this.googleServer = googleServer;
            this.facebookServer = facebookServer;
            this.keycloakServer = keycloakServer;
        }
        
        public void setupGoogleSuccessFlow(String email, String name) {
            googleServer.stubFor(get(urlPathMatching("/o/oauth2/v2/auth.*"))
                .willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location", "http://localhost/login/oauth2/code/google?code=test-google-auth-code&state={{request.query.state}}")));
            
            googleServer.stubFor(post(urlEqualTo("/oauth2/v4/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"access_token\": \"test-google-access-token\"," +
                        "\"token_type\": \"Bearer\"," +
                        "\"expires_in\": 3600," +
                        "\"scope\": \"email profile\"," +
                        "\"id_token\": \"test-google-id-token\"" +
                        "}")));
            
            googleServer.stubFor(get(urlEqualTo("/oauth2/v3/userinfo"))
                .withHeader("Authorization", equalTo("Bearer test-google-access-token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"sub\": \"google-user-123\"," +
                        "\"name\": \"" + name + "\"," +
                        "\"given_name\": \"" + name.split(" ")[0] + "\"," +
                        "\"family_name\": \"" + (name.split(" ").length > 1 ? name.split(" ")[1] : "User") + "\"," +
                        "\"email\": \"" + email + "\"," +
                        "\"email_verified\": true," +
                        "\"picture\": \"https://example.com/photo.jpg\"" +
                        "}")));
        }
        
        public void resetAll() {
            googleServer.resetAll();
            facebookServer.resetAll();
            keycloakServer.resetAll();
            startAndConfigureServers(); // Re-setup base stubs
        }
    }
    
    /**
     * JUnit 5 extension to ensure servers are started before any tests
     */
    public static class WireMockExtension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            startAndConfigureServers();
        }
    }
}