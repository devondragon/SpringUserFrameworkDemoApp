package com.digitalsanctuary.spring.demo.user.ui.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class for validating database state during UI tests
 */
public class DatabaseStateValidator {
    
    private static final String USER_EXISTS_QUERY = 
        "SELECT COUNT(*) FROM user_account WHERE email = ?";
    
    private static final String USER_ENABLED_QUERY = 
        "SELECT enabled FROM user_account WHERE email = ?";
    
    private static final String USER_LOCKED_QUERY = 
        "SELECT locked FROM user_account WHERE email = ?";
    
    private static final String USER_DETAILS_QUERY = 
        "SELECT first_name, last_name, enabled, locked, failed_login_attempts FROM user_account WHERE email = ?";
    
    private static final String VERIFICATION_TOKEN_EXISTS_QUERY = 
        "SELECT COUNT(*) FROM verification_token WHERE user_id = (SELECT id FROM user_account WHERE email = ?)";
    
    private static final String PASSWORD_RESET_TOKEN_EXISTS_QUERY = 
        "SELECT COUNT(*) FROM password_reset_token WHERE user_id = (SELECT id FROM user_account WHERE email = ?)";

    /**
     * Check if a user exists in the database
     */
    public static boolean userExists(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(USER_EXISTS_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            return resultSet.next() && resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if user exists: " + userEmail, e);
        }
    }

    /**
     * Check if a user is enabled
     */
    public static boolean isUserEnabled(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(USER_ENABLED_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            return resultSet.next() && resultSet.getBoolean(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if user is enabled: " + userEmail, e);
        }
    }

    /**
     * Check if a user account is locked
     */
    public static boolean isUserLocked(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(USER_LOCKED_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            return resultSet.next() && resultSet.getBoolean(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if user is locked: " + userEmail, e);
        }
    }

    /**
     * Get user details for validation
     */
    public static UserDetails getUserDetails(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(USER_DETAILS_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return new UserDetails(
                    resultSet.getString("first_name"),
                    resultSet.getString("last_name"),
                    resultSet.getBoolean("enabled"),
                    resultSet.getBoolean("locked"),
                    resultSet.getInt("failed_login_attempts")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user details: " + userEmail, e);
        }
    }

    /**
     * Check if a verification token exists for the user
     */
    public static boolean hasVerificationToken(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(VERIFICATION_TOKEN_EXISTS_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            return resultSet.next() && resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check verification token: " + userEmail, e);
        }
    }

    /**
     * Check if a password reset token exists for the user
     */
    public static boolean hasPasswordResetToken(String userEmail) {
        try (Connection connection = com.digitalsanctuary.spring.user.jdbc.ConnectionManager.open()) {
            PreparedStatement statement = connection.prepareStatement(PASSWORD_RESET_TOKEN_EXISTS_QUERY);
            statement.setString(1, userEmail);
            ResultSet resultSet = statement.executeQuery();
            
            return resultSet.next() && resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check password reset token: " + userEmail, e);
        }
    }

    /**
     * Validate complete user registration state
     */
    public static void validateUserRegistered(String userEmail, String firstName, String lastName) {
        if (!userExists(userEmail)) {
            throw new AssertionError("User should exist after registration: " + userEmail);
        }
        
        UserDetails details = getUserDetails(userEmail);
        if (!details.getFirstName().equals(firstName)) {
            throw new AssertionError("First name mismatch. Expected: " + firstName + ", Actual: " + details.getFirstName());
        }
        
        if (!details.getLastName().equals(lastName)) {
            throw new AssertionError("Last name mismatch. Expected: " + lastName + ", Actual: " + details.getLastName());
        }
        
        if (!hasVerificationToken(userEmail)) {
            throw new AssertionError("Verification token should exist after registration: " + userEmail);
        }
    }

    /**
     * Validate user email verification completed
     */
    public static void validateEmailVerified(String userEmail) {
        if (!isUserEnabled(userEmail)) {
            throw new AssertionError("User should be enabled after email verification: " + userEmail);
        }
        
        if (hasVerificationToken(userEmail)) {
            throw new AssertionError("Verification token should be consumed after verification: " + userEmail);
        }
    }

    /**
     * Validate user profile update
     */
    public static void validateProfileUpdated(String userEmail, String newFirstName, String newLastName) {
        UserDetails details = getUserDetails(userEmail);
        if (!details.getFirstName().equals(newFirstName)) {
            throw new AssertionError("First name not updated. Expected: " + newFirstName + ", Actual: " + details.getFirstName());
        }
        
        if (!details.getLastName().equals(newLastName)) {
            throw new AssertionError("Last name not updated. Expected: " + newLastName + ", Actual: " + details.getLastName());
        }
    }

    /**
     * Validate user account deletion
     */
    public static void validateAccountDeleted(String userEmail) {
        if (userExists(userEmail)) {
            // Could be soft delete - check if disabled instead
            if (isUserEnabled(userEmail)) {
                throw new AssertionError("User account should be deleted or disabled: " + userEmail);
            }
        }
    }

    /**
     * Data class for user details
     */
    public static class UserDetails {
        private final String firstName;
        private final String lastName;
        private final boolean enabled;
        private final boolean locked;
        private final int failedLoginAttempts;

        public UserDetails(String firstName, String lastName, boolean enabled, boolean locked, int failedLoginAttempts) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.enabled = enabled;
            this.locked = locked;
            this.failedLoginAttempts = failedLoginAttempts;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public boolean isEnabled() { return enabled; }
        public boolean isLocked() { return locked; }
        public int getFailedLoginAttempts() { return failedLoginAttempts; }
    }
}