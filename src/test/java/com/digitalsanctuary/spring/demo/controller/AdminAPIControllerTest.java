package com.digitalsanctuary.spring.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;
import jakarta.persistence.EntityManager;

/**
 * Covers the admin lock/unlock endpoints that back src/main/resources/static/js/admin/admin-action.js.
 */
@IntegrationTest
@DisplayName("Admin Lock/Unlock API Tests")
class AdminAPIControllerTest {

    private static final String LOCK_URI = "/admin/lockAccount";
    private static final String UNLOCK_URI = "/admin/unlockAccount";
    private static final String TARGET_EMAIL = "admin.action.target@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        User existing = userRepository.findByEmail(TARGET_EMAIL);
        if (existing != null) {
            userRepository.delete(existing);
            entityManager.flush();
        }
    }

    /** Persists the target user inside the test transaction so it rolls back cleanly. */
    private User saveTargetUser(UserTestDataBuilder builder) {
        User user = builder.withEmail(TARGET_EMAIL).withFirstName("Target").withLastName("User").verified().withId(null).build();
        user.setRoles(new ArrayList<>());
        User saved = userRepository.save(user);
        entityManager.flush();
        return saved;
    }

    /** Re-reads the target user from the database, bypassing the first level cache. */
    private User reloadTargetUser() {
        entityManager.flush();
        entityManager.clear();
        return userRepository.findByEmail(TARGET_EMAIL);
    }

    private static String body(String email) {
        return "{\"email\":\"" + email + "\"}";
    }

    @Test
    @DisplayName("Admin can lock an account")
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN_PRIVILEGE"})
    void adminCanLockAccount() throws Exception {
        saveTargetUser(UserTestDataBuilder.aUser().unlocked());

        mockMvc.perform(post(LOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body(TARGET_EMAIL)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("Account locked."));

        User locked = reloadTargetUser();
        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.getLockedDate()).isNotNull();
    }

    @Test
    @DisplayName("Admin can unlock an account")
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN_PRIVILEGE"})
    void adminCanUnlockAccount() throws Exception {
        saveTargetUser(UserTestDataBuilder.aUser().locked().withFailedLoginAttempts(5));

        mockMvc.perform(post(UNLOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body(TARGET_EMAIL)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages[0]").value("Account unlocked."));

        User unlocked = reloadTargetUser();
        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.getLockedDate()).isNull();
        assertThat(unlocked.getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("Unknown email returns a not found JSON response")
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN_PRIVILEGE"})
    void unknownEmailReturnsNotFound() throws Exception {
        mockMvc.perform(post(LOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body("nobody@example.com")).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0]").value("User not found."));
    }

    @Test
    @DisplayName("Blank email returns a bad request JSON response")
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN_PRIVILEGE"})
    void blankEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(UNLOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body("  ")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0]").value("Email is required."));
    }

    @Test
    @DisplayName("Non-admin gets 403 on lock")
    @WithMockUser(username = "user@example.com", authorities = {"LOGIN_PRIVILEGE"})
    void nonAdminCannotLock() throws Exception {
        saveTargetUser(UserTestDataBuilder.aUser().unlocked());

        mockMvc.perform(post(LOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body(TARGET_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reloadTargetUser().isLocked()).isFalse();
    }

    @Test
    @DisplayName("Non-admin gets 403 on unlock")
    @WithMockUser(username = "user@example.com", authorities = {"LOGIN_PRIVILEGE"})
    void nonAdminCannotUnlock() throws Exception {
        saveTargetUser(UserTestDataBuilder.aUser().locked());

        mockMvc.perform(post(UNLOCK_URI).contentType(MediaType.APPLICATION_JSON).content(body(TARGET_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reloadTargetUser().isLocked()).isTrue();
    }
}
