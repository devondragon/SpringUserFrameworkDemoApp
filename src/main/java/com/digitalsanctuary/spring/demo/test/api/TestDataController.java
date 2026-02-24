package com.digitalsanctuary.spring.demo.test.api;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileRepository;
import com.digitalsanctuary.spring.user.persistence.model.PasswordResetToken;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.model.VerificationToken;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordResetTokenRepository;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.persistence.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test-only REST controller for Playwright E2E tests. Provides endpoints to query and manipulate test data directly,
 * bypassing normal application flows.
 * <p>
 * WARNING: This controller is only loaded when the 'playwright-test' profile is active. It should NEVER be available in
 * production.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Profile("playwright-test")
public class TestDataController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoUserProfileRepository demoUserProfileRepository;

    /**
     * Check if a user exists by email.
     */
    @GetMapping("/user/exists")
    public ResponseEntity<Map<String, Object>> userExists(@RequestParam String email) {
        log.debug("Test API: Checking if user exists: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", user != null);
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a user is enabled (email verified).
     */
    @GetMapping("/user/enabled")
    public ResponseEntity<Map<String, Object>> userEnabled(@RequestParam String email) {
        log.debug("Test API: Checking if user is enabled: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("exists", true);
            response.put("enabled", user.isEnabled());
            response.put("email", email);
        } else {
            response.put("exists", false);
            response.put("enabled", false);
            response.put("email", email);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get user details for validation.
     */
    @GetMapping("/user/details")
    public ResponseEntity<Map<String, Object>> userDetails(@RequestParam String email) {
        log.debug("Test API: Getting user details: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("exists", true);
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("enabled", user.isEnabled());
            response.put("locked", user.isLocked());
            response.put("failedLoginAttempts", user.getFailedLoginAttempts());
        } else {
            response.put("exists", false);
            response.put("email", email);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get the verification token for a user. Used to simulate email verification by navigating directly to the
     * verification URL.
     */
    @GetMapping("/user/verification-token")
    public ResponseEntity<Map<String, Object>> getVerificationToken(@RequestParam String email) {
        log.debug("Test API: Getting verification token for: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("exists", false);
            response.put("email", email);
            response.put("token", null);
            return ResponseEntity.ok(response);
        }

        VerificationToken token = verificationTokenRepository.findByUser(user);
        if (token != null) {
            response.put("exists", true);
            response.put("email", email);
            response.put("token", token.getToken());
            response.put("expiryDate", token.getExpiryDate().toString());
        } else {
            response.put("exists", true);
            response.put("email", email);
            response.put("token", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get the password reset token for a user.
     */
    @GetMapping("/user/password-reset-token")
    public ResponseEntity<Map<String, Object>> getPasswordResetToken(@RequestParam String email) {
        log.debug("Test API: Getting password reset token for: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("exists", false);
            response.put("email", email);
            response.put("token", null);
            return ResponseEntity.ok(response);
        }

        PasswordResetToken token = passwordResetTokenRepository.findByUser(user);
        if (token != null) {
            response.put("exists", true);
            response.put("email", email);
            response.put("token", token.getToken());
            response.put("expiryDate", token.getExpiryDate().toString());
        } else {
            response.put("exists", true);
            response.put("email", email);
            response.put("token", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Create a test user directly in the database. Useful for setting up test preconditions.
     */
    @PostMapping("/user")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestUser(@RequestBody CreateUserRequest request) {
        log.info("Test API: Creating test user: {}", request.email());

        // Check if user already exists
        if (userRepository.findByEmail(request.email()) != null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User already exists");
            errorResponse.put("email", request.email());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        // Create user
        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName() != null ? request.firstName() : "Test");
        user.setLastName(request.lastName() != null ? request.lastName() : "User");
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled() != null ? request.enabled() : true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setRegistrationDate(new Date());

        // Assign default role
        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole != null) {
            user.setRoles(Collections.singletonList(userRole));
        } else {
            log.warn("Test API: ROLE_USER not found in database - user will be created without roles");
        }

        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("enabled", savedUser.isEnabled());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Delete a test user by email. Used for cleanup after tests.
     */
    @DeleteMapping("/user")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteTestUser(@RequestParam String email) {
        log.info("Test API: Deleting test user: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "User not found");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Delete related entities first to avoid foreign key constraints
        demoUserProfileRepository.findById(user.getId()).ifPresent(demoUserProfileRepository::delete);

        VerificationToken verificationToken = verificationTokenRepository.findByUser(user);
        if (verificationToken != null) {
            verificationTokenRepository.delete(verificationToken);
        }

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUser(user);
        if (passwordResetToken != null) {
            passwordResetTokenRepository.delete(passwordResetToken);
        }

        // Delete user
        userRepository.delete(user);

        response.put("success", true);
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Enable a user directly (simulate email verification).
     */
    @PostMapping("/user/enable")
    @Transactional
    public ResponseEntity<Map<String, Object>> enableUser(@RequestParam String email) {
        log.info("Test API: Enabling user: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "User not found");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        user.setEnabled(true);
        userRepository.save(user);

        // Delete verification token if exists
        VerificationToken verificationToken = verificationTokenRepository.findByUser(user);
        if (verificationToken != null) {
            verificationTokenRepository.delete(verificationToken);
        }

        response.put("success", true);
        response.put("email", email);
        response.put("enabled", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a verification token for a user. Used to test email verification flow when emails are disabled.
     */
    @PostMapping("/user/verification-token")
    @Transactional
    public ResponseEntity<Map<String, Object>> createVerificationToken(@RequestParam String email) {
        log.info("Test API: Creating verification token for: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "User not found");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Delete existing token if any
        VerificationToken existingToken = verificationTokenRepository.findByUser(user);
        if (existingToken != null) {
            verificationTokenRepository.delete(existingToken);
        }

        // Create new verification token
        String tokenValue = java.util.UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(tokenValue, user);
        verificationTokenRepository.save(token);

        response.put("success", true);
        response.put("email", email);
        response.put("token", tokenValue);
        response.put("expiryDate", token.getExpiryDate().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unlock a user account.
     */
    @PostMapping("/user/unlock")
    @Transactional
    public ResponseEntity<Map<String, Object>> unlockUser(@RequestParam String email) {
        log.info("Test API: Unlocking user: {}", email);
        User user = userRepository.findByEmail(email);
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "User not found");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedDate(null);
        userRepository.save(user);

        response.put("success", true);
        response.put("email", email);
        response.put("locked", false);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for test API.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("profile", "playwright-test");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for creating a test user.
     */
    public record CreateUserRequest(String email, String password, String firstName, String lastName, Boolean enabled) {
    }
}
