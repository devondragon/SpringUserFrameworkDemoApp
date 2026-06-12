package com.digitalsanctuary.spring.demo.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards the consistency of the MFA configuration files themselves.
 *
 * The MFA entry point pages must be listed in {@code user.security.unprotectedURIs}: the framework's
 * access-denied handler redirects partially-authenticated users to the entry point URI, and if that page is itself
 * protected the redirect loops forever. The framework only auto-unprotects {@code /user/mfa/status}, not the entry
 * point pages, so the demo config has to keep these two settings in sync by hand.
 */
@DisplayName("MFA Config Consistency Tests")
class MfaConfigConsistencyTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String resource) {
        InputStream in = MfaConfigConsistencyTest.class.getResourceAsStream(resource);
        assertThat(in).as("config file %s should exist on the classpath", resource).isNotNull();
        return new Yaml().load(in);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> yaml, String... path) {
        Map<String, Object> current = yaml;
        for (String key : path) {
            Object value = current.get(key);
            assertThat(value).as("section %s should exist", String.join(".", path)).isInstanceOf(Map.class);
            current = (Map<String, Object>) value;
        }
        return current;
    }

    private static List<String> unprotectedUris(Map<String, Object> yaml) {
        Object uris = section(yaml, "user", "security").get("unprotectedURIs");
        assertThat(uris).as("user.security.unprotectedURIs should be set").isNotNull();
        return Arrays.stream(uris.toString().split(",")).map(String::trim).toList();
    }

    @Test
    @DisplayName("application.yml unprotects the configured WebAuthn challenge page")
    void baseConfigUnprotectsWebauthnEntryPoint() {
        Map<String, Object> yaml = loadYaml("/application.yml");
        Map<String, Object> mfa = section(yaml, "user", "mfa");

        String webauthnEntryPoint = String.valueOf(mfa.get("webauthnEntryPointUri"));
        assertThat(webauthnEntryPoint).as("user.mfa.webauthnEntryPointUri should be configured").isNotEqualTo("null");
        assertThat(unprotectedUris(yaml))
                .as("the WebAuthn challenge page must be unprotected or MFA redirects loop forever")
                .contains(webauthnEntryPoint);
    }

    @Test
    @DisplayName("application.yml leaves MFA disabled by default (opt-in via the mfa profile)")
    void baseConfigLeavesMfaDisabled() {
        // A user who logs in with a password but has no registered passkey cannot satisfy the WEBAUTHN
        // factor and is locked out of every protected page, so the demo must not force MFA on by default.
        Map<String, Object> mfa = section(loadYaml("/application.yml"), "user", "mfa");
        assertThat(mfa.get("enabled")).isEqualTo(Boolean.FALSE);
    }

    @Test
    @DisplayName("mfa profile enables MFA and unprotects the challenge page and passkey enrollment endpoints")
    void mfaProfileEnablesMfaAndUnprotectsEntryPoint() {
        Map<String, Object> yaml = loadYaml("/application-mfa.yml");
        Map<String, Object> mfa = section(yaml, "user", "mfa");
        assertThat(mfa.get("enabled")).isEqualTo(Boolean.TRUE);

        List<String> uris = unprotectedUris(yaml);
        assertThat(uris).contains("/user/mfa/webauthn-challenge.html");
        // Partially-authenticated users need to be able to enroll their first passkey, otherwise new
        // accounts can never satisfy the WEBAUTHN factor.
        assertThat(uris).contains("/webauthn/register/options", "/webauthn/register");
    }
}
