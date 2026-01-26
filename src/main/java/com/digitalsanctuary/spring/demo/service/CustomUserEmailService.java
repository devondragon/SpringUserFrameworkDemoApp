package com.digitalsanctuary.spring.demo.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.digitalsanctuary.spring.user.persistence.repository.PasswordResetTokenRepository;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.mail.MailService;
import com.digitalsanctuary.spring.user.service.SessionInvalidationService;
import com.digitalsanctuary.spring.user.service.UserEmailService;
import com.digitalsanctuary.spring.user.service.UserVerificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom extension of UserEmailService that allows disabling password reset emails during tests.
 * When email sending is disabled, tokens are still created so tests can retrieve them via the Test API.
 */
@Slf4j
@Service
@Primary
public class CustomUserEmailService extends UserEmailService {

    @Value("${app.mail.sendPasswordResetEmail:true}")
    private boolean sendPasswordResetEmail;

    private final SecureRandom secureRandom = new SecureRandom();

    public CustomUserEmailService(
            MailService mailService,
            UserVerificationService userVerificationService,
            PasswordResetTokenRepository passwordTokenRepository,
            ApplicationEventPublisher eventPublisher,
            SessionInvalidationService sessionInvalidationService) {
        super(mailService, userVerificationService, passwordTokenRepository, eventPublisher, sessionInvalidationService);
    }

    @Override
    public void sendForgotPasswordVerificationEmail(final User user, final String appUrl) {
        if (!sendPasswordResetEmail) {
            log.debug("Password reset email disabled, creating token only for: {}", user.getEmail());
            // Generate token and save it so tests can retrieve via Test API
            String token = generateToken();
            createPasswordResetTokenForUser(user, token);
            return;
        }
        super.sendForgotPasswordVerificationEmail(user, appUrl);
    }

    /**
     * Generates a secure random token for password reset.
     * This mirrors the private generateToken() method in the parent class.
     */
    private String generateToken() {
        byte[] tokenBytes = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
