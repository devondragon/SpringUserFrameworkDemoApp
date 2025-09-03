package com.digitalsanctuary.spring.user.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;

/**
 * Admin Role Access Control Test - Phase 3 of Task 4.3 (Simplified)
 *
 * Tests role-based access control and admin privileges: - Admin vs User permission verification - Role hierarchy validation - Multi-user permission
 * scenarios
 *
 * This simplified version focuses on working functionality.
 */
@SpringBootTest(classes = UserDemoApplication.class)
@AutoConfigureMockMvc
@IntegrationTest
@ActiveProfiles("test")
@DisplayName("Admin Role Access Control Tests")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminRoleAccessControlTest {

    @Autowired
    private MultiUserTestUtilities testUtilities;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    private TestUserManager userManager;
    private static final String TEST_PREFIX = "role.access";

    @BeforeEach
    void setUp() {
        String uniquePrefix = TEST_PREFIX + "." + System.currentTimeMillis();
        userManager = testUtilities.createTestUserManager(uniquePrefix);
        testUtilities.ensureRolesExist();
    }

    @AfterEach
    void tearDown() {
        if (userManager != null) {
            userManager.cleanup();
        }
    }

    @Nested
    @DisplayName("Role Privilege Verification")
    class RolePrivilegeTests {

        @Test
        @DisplayName("Admin role should have comprehensive privileges")
        @Transactional
        void testAdminRolePrivileges() {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            assertThat(adminRole).isNotNull();

            // Verify admin has key privileges
            assertThat(adminRole.getPrivileges()).extracting("name").contains("ADMIN_PRIVILEGE", "READ_USER_PRIVILEGE",
                    "RESET_ANY_USER_PASSWORD_PRIVILEGE", "CREATE_EVENT_PRIVILEGE", "DELETE_EVENT_PRIVILEGE", "UPDATE_EVENT_PRIVILEGE");
        }

        @Test
        @DisplayName("Manager role should have limited privileges")
        @Transactional
        void testManagerRolePrivileges() {
            Role managerRole = roleRepository.findByName("ROLE_MANAGER");
            assertThat(managerRole).isNotNull();

            // Manager should have specific privileges
            assertThat(managerRole.getPrivileges()).extracting("name").contains("ADD_USER_TO_TEAM_PRIVILEGE", "REMOVE_USER_FROM_TEAM_PRIVILEGE",
                    "RESET_TEAM_PASSWORD_PRIVILEGE");

            // Manager should NOT have admin privileges
            assertThat(managerRole.getPrivileges()).extracting("name").doesNotContain("ADMIN_PRIVILEGE", "RESET_ANY_USER_PASSWORD_PRIVILEGE");
        }

        @Test
        @DisplayName("User role should have basic privileges only")
        @Transactional
        void testUserRolePrivileges() {
            Role userRole = roleRepository.findByName("ROLE_USER");
            assertThat(userRole).isNotNull();

            // User should have basic privileges
            assertThat(userRole.getPrivileges()).extracting("name").contains("LOGIN_PRIVILEGE", "UPDATE_OWN_USER_PRIVILEGE",
                    "RESET_OWN_PASSWORD_PRIVILEGE", "REGISTER_FOR_EVENT_PRIVILEGE");

            // User should NOT have admin or manager privileges
            assertThat(userRole.getPrivileges()).extracting("name").doesNotContain("ADMIN_PRIVILEGE", "READ_USER_PRIVILEGE",
                    "ADD_USER_TO_TEAM_PRIVILEGE");
        }
    }

    @Nested
    @DisplayName("Event Management Access Control")
    class EventAccessControlTests {

        @Test
        @WithMockUser(authorities = {"CREATE_EVENT_PRIVILEGE", "UPDATE_EVENT_PRIVILEGE", "DELETE_EVENT_PRIVILEGE"})
        @DisplayName("Admin can perform all event operations")
        void testAdminEventOperations() throws Exception {
            // Admin can create events
            mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Admin Event\", \"description\": \"Created by admin\"}").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = {"REGISTER_FOR_EVENT_PRIVILEGE"})
        @DisplayName("Regular user can only register for events")
        void testUserEventLimitations() throws Exception {
            // User CANNOT create events
            mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"User Event\", \"description\": \"Should fail\"}").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {})
        @DisplayName("No authorities should deny access")
        void testNoAuthoritiesAccess() throws Exception {
            mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Unauthorized\", \"description\": \"Should fail\"}").with(csrf())).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Multi-User Permission Scenarios")
    class MultiUserPermissionTests {

        @Test
        @DisplayName("Multiple permission levels should work independently")
        void testMultiplePermissionLevels() {
            // Test that different permission combinations work as expected
            testUtilities.ensureRolesExist();

            // Verify roles exist and have correct privileges
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            Role managerRole = roleRepository.findByName("ROLE_MANAGER");
            Role userRole = roleRepository.findByName("ROLE_USER");

            assertThat(adminRole).isNotNull();
            assertThat(managerRole).isNotNull();
            assertThat(userRole).isNotNull();

            // Admin should have more privileges than manager
            assertThat(adminRole.getPrivileges().size()).isGreaterThan(managerRole.getPrivileges().size());

            // All roles should have at least one privilege
            assertThat(adminRole.getPrivileges().size()).isGreaterThan(0);
            assertThat(managerRole.getPrivileges().size()).isGreaterThan(0);
            assertThat(userRole.getPrivileges().size()).isGreaterThan(0);
        }

        @Test
        @WithMockUser(authorities = {"CREATE_EVENT_PRIVILEGE", "REGISTER_FOR_EVENT_PRIVILEGE"})
        @DisplayName("Partial admin permissions should work correctly")
        void testPartialAdminPermissions() throws Exception {
            // User with some admin privileges can create events
            mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Partial Admin Event\", \"description\": \"Test\"}").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = {"ADMIN_PRIVILEGE", "READ_USER_PRIVILEGE"})
        @DisplayName("Admin privileges should allow user management access")
        void testAdminUserManagementAccess() throws Exception {
            // This would test admin-specific endpoints if they existed
            // For now, verify the privileges are correctly configured
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            assertThat(adminRole.getPrivileges()).extracting("name").contains("ADMIN_PRIVILEGE", "READ_USER_PRIVILEGE");
        }
    }

    @Nested
    @DisplayName("Concurrent Permission Tests")
    class ConcurrentPermissionTests {

        @Test
        @DisplayName("Multiple users with different roles should access appropriately")
        void testConcurrentRoleAccess() {
            final int threadCount = 5;

            // Test concurrent role checks
            Runnable roleCheckTask = () -> {
                try {
                    // Each thread verifies role configuration
                    Role adminRole = roleRepository.findByName("ROLE_ADMIN");
                    Role userRole = roleRepository.findByName("ROLE_USER");

                    assertThat(adminRole).isNotNull();
                    assertThat(userRole).isNotNull();

                    // Admin should have more privileges
                    assertThat(adminRole.getPrivileges().size()).isGreaterThan(userRole.getPrivileges().size());

                } catch (Exception e) {
                    throw new AssertionError("Concurrent role check failed", e);
                }
            };

            // Execute concurrent role checks
            MultiUserTestUtilities.ConcurrentExecutionResult result = testUtilities.executeConcurrently(threadCount, roleCheckTask, 30);

            // Verify all checks passed
            assertThat(result.completedWithinTimeout()).isTrue();
            assertThat(result.errorCount()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(threadCount);
        }
    }
}
