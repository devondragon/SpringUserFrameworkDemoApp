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
 * As of the framework release containing the #313 fix, the framework auto-unprotects the configured MFA factor
 * entry-point URIs ({@code user.mfa.passwordEntryPointUri} / {@code user.mfa.webauthnEntryPointUri}), so the demo no
 * longer has to list the WebAuthn challenge page in {@code user.security.unprotectedURIs} by hand. These tests now only
 * assert what is still the demo's own responsibility: MFA must be opt-in (disabled by default), and the passkey
 * <em>enrollment</em> endpoints (which are not factor entry points) must be reachable by partially-authenticated users.
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
    @DisplayName("application.yml leaves MFA disabled by default (opt-in via the mfa profile)")
    void baseConfigLeavesMfaDisabled() {
        // A user who logs in with a password but has no registered passkey cannot satisfy the WEBAUTHN
        // factor and is locked out of every protected page, so the demo must not force MFA on by default.
        Map<String, Object> mfa = section(loadYaml("/application.yml"), "user", "mfa");
        assertThat(mfa.get("enabled")).isEqualTo(Boolean.FALSE);
    }

    @Test
    @DisplayName("mfa profile enables MFA and unprotects the passkey enrollment endpoints")
    void mfaProfileEnablesMfaAndUnprotectsEnrollmentEndpoints() {
        Map<String, Object> yaml = loadYaml("/application-mfa.yml");
        Map<String, Object> mfa = section(yaml, "user", "mfa");
        assertThat(mfa.get("enabled")).isEqualTo(Boolean.TRUE);

        // The WebAuthn challenge page (the configured factor entry point) is now auto-unprotected by the
        // framework, so it no longer needs to be listed here. The passkey ENROLLMENT endpoints are not factor
        // entry points, so partially-authenticated users still need them unprotected to enroll a first passkey.
        List<String> uris = unprotectedUris(yaml);
        assertThat(uris).contains("/webauthn/register/options", "/webauthn/register");
    }
}
