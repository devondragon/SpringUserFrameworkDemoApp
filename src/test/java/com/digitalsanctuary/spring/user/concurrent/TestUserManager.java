package com.digitalsanctuary.spring.user.concurrent;

import com.digitalsanctuary.spring.user.persistence.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test User Manager for managing multiple test users in concurrent scenarios.
 * 
 * Provides convenient methods for creating and managing test users with different roles
 * for multi-user interaction testing.
 */
public class TestUserManager {
    
    private static final Logger log = LoggerFactory.getLogger(TestUserManager.class);
    
    private final String testPrefix;
    private final MultiUserTestUtilities utilities;
    private final AtomicInteger userCounter = new AtomicInteger(1);
    private final List<User> managedUsers = new ArrayList<>();

    public TestUserManager(String testPrefix, MultiUserTestUtilities utilities) {
        this.testPrefix = testPrefix;
        this.utilities = utilities;
    }

    /**
     * Creates an admin user for testing admin operations.
     */
    public User createAdminUser() {
        return createUser("Admin", "ROLE_ADMIN");
    }

    /**
     * Creates a manager user for testing management operations.
     */
    public User createManagerUser() {
        return createUser("Manager", "ROLE_MANAGER");
    }

    /**
     * Creates a standard user for testing regular operations.
     */
    public User createStandardUser() {
        return createUser("User", "ROLE_USER");
    }

    /**
     * Creates multiple standard users for concurrent testing.
     */
    public List<User> createStandardUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createStandardUser());
        }
        return users;
    }

    /**
     * Creates a user with specified role.
     */
    public User createUser(String roleType, String roleName) {
        int userNum = userCounter.getAndIncrement();
        String email = String.format("%s.%s.%d@test.example.com", testPrefix, roleType.toLowerCase(), userNum);
        String firstName = roleType;
        String lastName = "User" + userNum;
        
        User user = utilities.createTestUser(email, firstName, lastName, roleName);
        managedUsers.add(user);
        
        log.debug("Created {} user: {} ({})", roleType, email, roleName);
        return user;
    }

    /**
     * Creates a user with unique email for concurrent testing.
     */
    public User createUniqueUser(String suffix) {
        int userNum = userCounter.getAndIncrement();
        String email = String.format("%s.unique.%s.%d@test.example.com", testPrefix, suffix, userNum);
        String firstName = "Unique" + suffix;
        String lastName = "User" + userNum;
        
        User user = utilities.createTestUser(email, firstName, lastName, "ROLE_USER");
        managedUsers.add(user);
        
        log.debug("Created unique user: {}", email);
        return user;
    }

    /**
     * Creates users with the same email for race condition testing.
     * Note: Only one should succeed due to unique constraints.
     */
    public String getDuplicateEmailForTesting() {
        return testPrefix + ".duplicate@test.example.com";
    }

    /**
     * Gets all managed users created by this manager.
     */
    public List<User> getAllManagedUsers() {
        return new ArrayList<>(managedUsers);
    }

    /**
     * Gets count of managed users.
     */
    public int getManagedUserCount() {
        return managedUsers.size();
    }

    /**
     * Cleanup all managed users.
     */
    public void cleanup() {
        utilities.cleanupTestUsers(testPrefix);
        managedUsers.clear();
        log.debug("Cleaned up all managed users for prefix: {}", testPrefix);
    }
}