package com.digitalsanctuary.spring.demo.controller;

import java.util.Date;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.util.JSONResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON endpoints behind the admin actions page (templates/admin/actions.html and
 * static/js/admin/admin-action.js). All endpoints require ADMIN_PRIVILEGE, the same authority as the page.
 *
 * Every outcome returns a {@link JSONResponse} body so the page's fetch() can always read messages[0].
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminAPIController {

    private final UserRepository userRepository;

    /**
     * Request body for the lock and unlock endpoints.
     *
     * @param email the email address of the account to act on
     */
    public record AccountActionRequest(String email) {
    }

    /**
     * Locks a user account. A locked user fails authentication until the lockout duration elapses or an admin
     * unlocks the account.
     *
     * @param request the account to lock
     * @return 200 on success, 400 when the email is missing, 404 when no user has that email
     */
    @PostMapping("/lockAccount")
    @PreAuthorize("hasAuthority('ADMIN_PRIVILEGE')")
    @Transactional
    public ResponseEntity<JSONResponse> lockAccount(@RequestBody AccountActionRequest request) {
        return setLocked(request, true);
    }

    /**
     * Unlocks a user account and clears its failed login counter, matching what the framework's
     * LoginAttemptService does when a lockout expires.
     *
     * @param request the account to unlock
     * @return 200 on success, 400 when the email is missing, 404 when no user has that email
     */
    @PostMapping("/unlockAccount")
    @PreAuthorize("hasAuthority('ADMIN_PRIVILEGE')")
    @Transactional
    public ResponseEntity<JSONResponse> unlockAccount(@RequestBody AccountActionRequest request) {
        return setLocked(request, false);
    }

    private ResponseEntity<JSONResponse> setLocked(AccountActionRequest request, boolean locked) {
        String email = request.email() != null ? request.email().trim() : "";
        if (email.isEmpty()) {
            return response(HttpStatus.BAD_REQUEST, false, "Email is required.");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.info("Admin lock/unlock requested for unknown email: {}", email);
            return response(HttpStatus.NOT_FOUND, false, "User not found.");
        }

        user.setLocked(locked);
        if (locked) {
            user.setLockedDate(new Date());
        } else {
            user.setLockedDate(null);
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
        log.info("Admin set locked={} for user: {}", locked, email);

        return response(HttpStatus.OK, true, locked ? "Account locked." : "Account unlocked.");
    }

    private ResponseEntity<JSONResponse> response(HttpStatus status, boolean success, String message) {
        return ResponseEntity.status(status).body(JSONResponse.builder().success(success).code(status.value()).message(message).build());
    }
}
