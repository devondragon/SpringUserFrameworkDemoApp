package com.digitalsanctuary.spring.user.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import java.util.UUID;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Mock OAuth2 Server for testing different OAuth2 scenarios. Provides methods to simulate various OAuth2 flows and error conditions.
 */
public class MockOAuth2Server {

    private final WireMockServer wireMockServer;
    private final String provider;
    private final int port;

    public MockOAuth2Server(WireMockServer wireMockServer, String provider, int port) {
        this.wireMockServer = wireMockServer;
        this.provider = provider;
        this.port = port;
    }

    /**
     * Simulates successful OAuth2 authorization
     */
    public void simulateSuccessfulAuthorization(String email, String name) {
        WireMock.configureFor("localhost", port);

        switch (provider) {
            case "google":
                setupSuccessfulGoogleFlow(email, name);
                break;
            case "facebook":
                setupSuccessfulFacebookFlow(email, name);
                break;
            case "keycloak":
                setupSuccessfulKeycloakFlow(email, name, email.split("@")[0]);
                break;
        }
    }

    /**
     * Simulates user denying OAuth2 authorization
     */
    public void simulateAuthorizationDenied() {
        WireMock.configureFor("localhost", port);

        String authPath = getAuthorizationPath();

        stubFor(get(urlPathMatching(authPath + ".*")).willReturn(aResponse().withStatus(302).withHeader("Location",
                "http://localhost:8080/login/oauth2/code/" + provider + "?error=access_denied&error_description=User+denied+access")));
    }

    /**
     * Simulates invalid client credentials
     */
    public void simulateInvalidClientCredentials() {
        WireMock.configureFor("localhost", port);

        String tokenPath = getTokenPath();

        stubFor(post(urlEqualTo(tokenPath)).willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                .withBody("{" + "\"error\": \"invalid_client\"," + "\"error_description\": \"Client authentication failed\"" + "}")));
    }

    /**
     * Simulates expired or invalid token
     */
    public void simulateInvalidToken() {
        WireMock.configureFor("localhost", port);

        String userInfoPath = getUserInfoPath();

        stubFor(get(urlEqualTo(userInfoPath)).willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                .withBody("{" + "\"error\": \"invalid_token\"," + "\"error_description\": \"The access token is invalid or has expired\"" + "}")));
    }

    /**
     * Simulates network timeout
     */
    public void simulateNetworkTimeout() {
        WireMock.configureFor("localhost", port);

        String tokenPath = getTokenPath();

        stubFor(post(urlEqualTo(tokenPath)).willReturn(aResponse().withFixedDelay(30000) // 30 second delay to trigger timeout
                .withStatus(504)));
    }

    /**
     * Simulates rate limiting
     */
    public void simulateRateLimiting() {
        WireMock.configureFor("localhost", port);

        String tokenPath = getTokenPath();

        stubFor(post(urlEqualTo(tokenPath))
                .willReturn(aResponse().withStatus(429).withHeader("Content-Type", "application/json").withHeader("Retry-After", "60").withBody("{"
                        + "\"error\": \"rate_limit_exceeded\"," + "\"error_description\": \"Too many requests. Please try again later.\"" + "}")));
    }

    /**
     * Verifies that authorization was requested
     */
    public boolean verifyAuthorizationRequested() {
        WireMock.configureFor("localhost", port);

        String authPath = getAuthorizationPath();

        return !findAll(getRequestedFor(urlPathMatching(authPath + ".*"))).isEmpty();
    }

    /**
     * Verifies that token exchange was attempted
     */
    public boolean verifyTokenExchangeAttempted() {
        WireMock.configureFor("localhost", port);

        String tokenPath = getTokenPath();

        return !findAll(postRequestedFor(urlEqualTo(tokenPath))).isEmpty();
    }

    /**
     * Gets the number of times user info was requested
     */
    public int getUserInfoRequestCount() {
        WireMock.configureFor("localhost", port);

        String userInfoPath = getUserInfoPath();

        return findAll(getRequestedFor(urlEqualTo(userInfoPath))).size();
    }

    /**
     * Resets the mock server
     */
    public void reset() {
        wireMockServer.resetAll();
    }

    private void setupSuccessfulGoogleFlow(String email, String name) {
        String userId = "google-user-" + UUID.randomUUID();

        // UserInfo response
        stubFor(get(urlEqualTo("/oauth2/v3/userinfo")).withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{" + "\"sub\": \"" + userId + "\"," + "\"name\": \"" + name + "\"," + "\"given_name\": \"" + name.split(" ")[0]
                                + "\"," + "\"family_name\": \"" + (name.split(" ").length > 1 ? name.split(" ")[1] : "User") + "\"," + "\"email\": \""
                                + email + "\"," + "\"email_verified\": true," + "\"picture\": \"https://example.com/photo.jpg\"" + "}")));
    }

    private void setupSuccessfulFacebookFlow(String email, String name) {
        String userId = "facebook-user-" + UUID.randomUUID();

        // UserInfo response
        stubFor(get(urlPathMatching("/me.*")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{" + "\"id\": \"" + userId + "\"," + "\"name\": \"" + name + "\"," + "\"first_name\": \"" + name.split(" ")[0] + "\","
                        + "\"last_name\": \"" + (name.split(" ").length > 1 ? name.split(" ")[1] : "User") + "\"," + "\"email\": \"" + email + "\""
                        + "}")));
    }

    private void setupSuccessfulKeycloakFlow(String email, String name, String username) {
        String userId = "keycloak-user-" + UUID.randomUUID();

        // UserInfo response
        stubFor(get(urlEqualTo("/realms/test/protocol/openid-connect/userinfo")).withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{" + "\"sub\": \"" + userId + "\"," + "\"name\": \"" + name + "\"," + "\"preferred_username\": \"" + username
                                + "\"," + "\"given_name\": \"" + name.split(" ")[0] + "\"," + "\"family_name\": \""
                                + (name.split(" ").length > 1 ? name.split(" ")[1] : "User") + "\"," + "\"email\": \"" + email + "\","
                                + "\"email_verified\": true" + "}")));
    }

    private String getAuthorizationPath() {
        switch (provider) {
            case "google":
                return "/o/oauth2/v2/auth";
            case "facebook":
                return "/dialog/oauth";
            case "keycloak":
                return "/realms/test/protocol/openid-connect/auth";
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    private String getTokenPath() {
        switch (provider) {
            case "google":
                return "/oauth2/v4/token";
            case "facebook":
                return "/oauth/access_token";
            case "keycloak":
                return "/realms/test/protocol/openid-connect/token";
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    private String getUserInfoPath() {
        switch (provider) {
            case "google":
                return "/oauth2/v3/userinfo";
            case "facebook":
                return "/me";
            case "keycloak":
                return "/realms/test/protocol/openid-connect/userinfo";
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    /**
     * Builder for creating MockOAuth2Server instances
     */
    public static class Builder {
        private WireMockServer wireMockServer;
        private String provider;
        private int port;

        public Builder withWireMockServer(WireMockServer server) {
            this.wireMockServer = server;
            return this;
        }

        public Builder withProvider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public MockOAuth2Server build() {
            if (wireMockServer == null || provider == null || port == 0) {
                throw new IllegalStateException("WireMockServer, provider, and port must be set");
            }
            return new MockOAuth2Server(wireMockServer, provider, port);
        }
    }
}
