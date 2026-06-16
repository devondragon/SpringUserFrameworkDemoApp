package com.digitalsanctuary.spring.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.digitalsanctuary.spring.user.persistence.model.Privilege;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.PasswordHistoryRepository;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.DSUserDetails;
import com.digitalsanctuary.spring.user.service.DSUserDetailsService;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.RoleTestDataBuilder;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;

/**
 * Integration tests for DSUserDetailsService.
 * 
 * This test class verifies the full integration behavior including:
 * - Database persistence
 * - Transaction management
 * - LoginHelperService integration
 * - Authority loading
 * - Account unlock functionality
 */
@IntegrationTest
// A positive lockout duration makes auto-unlock deterministic: a user locked longer ago than this window
// is eligible to be unlocked on load, while one just locked stays locked. (The framework reads
// user.security.accountLockoutDuration; >= 0 enables time-based auto-unlock.)
@TestPropertySource(properties = "user.security.accountLockoutDuration=30")
@DisplayName("DSUserDetailsService Integration Tests")
class DSUserDetailsServiceIntegrationTest {

    @Autowired
    private DSUserDetailsService dsUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up
        passwordHistoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        // Force the DELETEs to hit the DB before the role INSERTs below. The framework seeds the configured
        // roles (ROLE_USER/ROLE_ADMIN/...) at startup, so without an explicit flush Hibernate's action-queue
        // ordering would run the new-role INSERTs before these DELETEs and trip the unique ROLE(NAME) index.
        roleRepository.flush();

        // Create privileges
        Privilege userPrivilege = new Privilege();
        userPrivilege.setName("ROLE_USER");

        Privilege adminPrivilege = new Privilege();
        adminPrivilege.setName("ROLE_ADMIN");

        // Create roles with privileges
        userRole = RoleTestDataBuilder.aRole()
                .withName("ROLE_USER")
                .withId(null)
                .build();
        userRole.getPrivileges().add(userPrivilege);
        userRole = roleRepository.save(userRole);

        adminRole = RoleTestDataBuilder.aRole()
                .withName("ROLE_ADMIN")
                .withId(null)
                .build();
        adminRole.getPrivileges().add(adminPrivilege);
        adminRole = roleRepository.save(adminRole);
    }

    @Test
    @Transactional
    @DisplayName("Should load user and update lastActivityDate")
    void loadUserByUsername_updatesLastActivityDate() {
        // Given
        Date originalDate = Date.from(
                LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());

        User user = UserTestDataBuilder.aVerifiedUser()
                .withEmail("activity@test.com")
                .withLastActivityDate(originalDate)
                .withId(null)
                .build();
        user.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        user = userRepository.save(user);

        Date beforeLoad = new Date();

        // When
        DSUserDetails result = dsUserDetailsService.loadUserByUsername("activity@test.com");

        // Then
        User updatedUser = userRepository.findByEmail("activity@test.com");
        assertThat(updatedUser.getLastActivityDate()).isAfter(originalDate);
        assertThat(updatedUser.getLastActivityDate()).isAfterOrEqualTo(beforeLoad);
        assertThat(result.getUser().getLastActivityDate()).isAfterOrEqualTo(beforeLoad);
    }

    @Test
    @Transactional
    @DisplayName("Should auto-unlock eligible locked user")
    void loadUserByUsername_autoUnlocksEligibleUser() {
        // Given - Create a locked user with old lock date (should be unlocked)
        Date oldLockDate = Date.from(
                LocalDateTime.now().minusHours(2).atZone(ZoneId.systemDefault()).toInstant());

        User lockedUser = UserTestDataBuilder.aLockedUser()
                .withEmail("autounlock@test.com")
                .withLockedDate(oldLockDate)
                .withFailedLoginAttempts(5)
                .verified() // enabled, so that once auto-unlocked the account is fully usable
                .withId(null)
                .build();
        lockedUser.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        lockedUser = userRepository.save(lockedUser);

        // Verify user is initially locked
        assertThat(lockedUser.isLocked()).isTrue();

        // When - the lock is older than accountLockoutDuration (30m), so loading auto-unlocks the account
        DSUserDetails result = dsUserDetailsService.loadUserByUsername("autounlock@test.com");

        // Then - the user is unlocked on load and authentication succeeds
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("autounlock@test.com");
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(userRepository.findByEmail("autounlock@test.com").isLocked()).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("Should load user with multiple roles and correct authorities")
    void loadUserByUsername_withMultipleRoles_loadsAllAuthorities() {
        // Given
        User multiRoleUser = UserTestDataBuilder.aVerifiedUser()
                .withEmail("multirole@test.com")
                .withId(null)
                .build();
        multiRoleUser.setRoles(new ArrayList<>(Arrays.asList(userRole, adminRole)));
        userRepository.save(multiRoleUser);

        // When
        DSUserDetails result = dsUserDetailsService.loadUserByUsername("multirole@test.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void loadUserByUsername_nonExistentUser_throwsException() {
        // When & Then
        assertThatThrownBy(() -> dsUserDetailsService.loadUserByUsername("nonexistent@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("No user found with email/username: nonexistent@test.com");
    }

    @Test
    @Transactional
    @DisplayName("Should handle concurrent access correctly")
    void loadUserByUsername_concurrentAccess_handlesCorrectly() throws InterruptedException {
        // Given
        User user = UserTestDataBuilder.aVerifiedUser()
                .withEmail("concurrent@test.com")
                .withId(null)
                .build();
        user.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        userRepository.save(user);

        // When - Simulate concurrent access
        Thread thread1 = new Thread(() -> {
            DSUserDetails result = dsUserDetailsService.loadUserByUsername("concurrent@test.com");
            assertThat(result).isNotNull();
        });

        Thread thread2 = new Thread(() -> {
            DSUserDetails result = dsUserDetailsService.loadUserByUsername("concurrent@test.com");
            assertThat(result).isNotNull();
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then - Both threads should complete successfully
        User finalUser = userRepository.findByEmail("concurrent@test.com");
        assertThat(finalUser).isNotNull();
        assertThat(finalUser.getLastActivityDate()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("Should correctly map all UserDetails properties")
    void loadUserByUsername_mapsAllUserDetailsProperties() {
        // Given
        User user = UserTestDataBuilder.aUser()
                .withEmail("mapping@test.com")
                .withPassword("password123") // Will be encoded by builder
                .withFirstName("Jane")
                .withLastName("Smith")
                .verified()
                .withId(null)
                .build();
        user.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        userRepository.save(user);

        // When
        DSUserDetails result = dsUserDetailsService.loadUserByUsername("mapping@test.com");

        // Then
        assertThat(result.getUsername()).isEqualTo("mapping@test.com");
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getPassword()).startsWith("$2a$"); // BCrypt encoded
        assertThat(result.getName()).isEqualTo("Jane Smith");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getEmail()).isEqualTo("mapping@test.com");
    }

    @Test
    @Transactional
    @DisplayName("Should reject loading a disabled user")
    void loadUserByUsername_disabledUser_throwsDisabledException() {
        // Given
        User disabledUser = UserTestDataBuilder.anUnverifiedUser()
                .withEmail("disabled@test.com")
                .withId(null)
                .build();
        disabledUser.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        userRepository.save(disabledUser);

        // When & Then - as of 4.4.0 the login helper enforces account status on every auth path, so loading
        // a disabled (unverified) account throws rather than returning a principal with isEnabled()==false.
        assertThatThrownBy(() -> dsUserDetailsService.loadUserByUsername("disabled@test.com"))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    @Transactional
    @DisplayName("Should reject loading a currently locked user")
    void loadUserByUsername_currentlyLockedUser_throwsLockedException() {
        // Given - a user locked just now: well inside the accountLockoutDuration window, so it must NOT
        // auto-unlock and stays locked.
        Date recentLockDate = new Date();

        User lockedUser = UserTestDataBuilder.aLockedUser()
                .withEmail("locked@test.com")
                .withLockedDate(recentLockDate)
                .verified() // enabled but locked, to prove lock (not disabled) is what rejects the load
                .withId(null)
                .build();
        lockedUser.setRoles(new ArrayList<>(Arrays.asList(userRole)));
        lockedUser = userRepository.save(lockedUser);

        // Verify user is initially locked
        assertThat(lockedUser.isLocked()).isTrue();

        // When & Then - loading a still-locked account throws LockedException (checked before disabled status)
        assertThatThrownBy(() -> dsUserDetailsService.loadUserByUsername("locked@test.com"))
                .isInstanceOf(LockedException.class);
        assertThat(userRepository.findByEmail("locked@test.com").isLocked()).isTrue();
    }
}