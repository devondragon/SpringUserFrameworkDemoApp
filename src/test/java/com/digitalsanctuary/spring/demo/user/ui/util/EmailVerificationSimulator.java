package com.digitalsanctuary.spring.demo.user.ui.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class for simulating email verification in UI tests
 */
public class EmailVerificationSimulator {

    private static final String GET_VERIFICATION_TOKEN_QUERY =
            "SELECT token FROM verification_token WHERE user_id = (SELECT id FROM user_account WHERE email = ?)";

    private static final String ENABLE_USER_QUERY = "UPDATE user_account SET enabled = true WHERE email = ?";

    private static final String DELETE_VERIFICATION_TOKEN_QUERY =
            "DELETE FROM verification_token WHERE user_id = (SELECT id FROM user_account WHERE email = ?)";

    /**
     * Get verification token for a user by email In a real application, this would come from the email content
     */
    public static String getVerificationToken(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(GET_VERIFICATION_TOKEN_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get verification token for " + userEmail, e);
        }
    }

    /**
     * Simulate email verification by directly enabling the user This bypasses the actual email verification endpoint
     */
    public static void simulateEmailVerification(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            // Enable the user
            PreparedStatement enableStatement = connection.prepareStatement(ENABLE_USER_QUERY);
            enableStatement.setString(1, userEmail);
            enableStatement.executeUpdate();

            // Delete the verification token (consumed)
            PreparedStatement deleteStatement = connection.prepareStatement(DELETE_VERIFICATION_TOKEN_QUERY);
            deleteStatement.setString(1, userEmail);
            deleteStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to simulate email verification for " + userEmail, e);
        }
    }

    /**
     * Check if user has a pending verification token
     */
    public static boolean hasVerificationToken(String userEmail) {
        return getVerificationToken(userEmail) != null;
    }

    /**
     * Generate a mock verification URL for testing
     */
    public static String generateVerificationUrl(String userEmail) {
        String token = getVerificationToken(userEmail);
        if (token == null) {
            throw new RuntimeException("No verification token found for user: " + userEmail);
        }
        return "http://localhost:8080/user/registrationConfirm?token=" + token;
    }

    /**
     * Generate an invalid verification URL for testing error scenarios
     */
    public static String generateInvalidVerificationUrl() {
        return "http://localhost:8080/user/registrationConfirm?token=invalid-token-12345";
    }

    /**
     * Get verification token directly from database (for testing purposes) This simulates what would normally be extracted from the email content
     */
    public static String extractTokenFromEmail(String userEmail) {
        // In a real test, this would parse the email content
        // For our simulation, we get it directly from the database
        return getVerificationToken(userEmail);
    }
}
