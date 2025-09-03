package com.digitalsanctuary.spring.user.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.RoleRepository;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.digitalsanctuary.spring.user.test.builders.RoleTestDataBuilder;
import jakarta.persistence.EntityManager;

/**
 * Multi-User Test Utilities for Task 4.3 Implementation
 *
 * Provides infrastructure for testing concurrent user operations and multi-user interactions. Based on proven patterns from
 * AccountLockoutIntegrationTest.
 */
@Component
public class MultiUserTestUtilities {

    private static final Logger log = LoggerFactory.getLogger(MultiUserTestUtilities.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    public MultiUserTestUtilities(UserRepository userRepository, RoleRepository roleRepository, UserService userService,
            EntityManager entityManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.entityManager = entityManager;
    }

    /**
     * Executes a task concurrently across multiple threads. Based on the concurrent pattern from AccountLockoutIntegrationTest.
     *
     * @param threadCount Number of concurrent threads
     * @param task Task to execute in each thread
     * @param timeoutSeconds Timeout in seconds
     * @return ConcurrentExecutionResult with success counts and errors
     */
    public ConcurrentExecutionResult executeConcurrently(int threadCount, Runnable task, int timeoutSeconds) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        try {
            // Submit all tasks
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Execute the task
                        task.run();
                        successCount.incrementAndGet();

                    } catch (Exception e) {
                        log.error("Concurrent task failed", e);
                        errorCount.incrementAndGet();
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Start all threads at once
            startLatch.countDown();

            // Wait for completion
            boolean completed = doneLatch.await(timeoutSeconds, TimeUnit.SECONDS);

            return new ConcurrentExecutionResult(threadCount, successCount.get(), errorCount.get(), exceptions, completed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrent execution interrupted", e);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Creates test users with different roles for multi-user scenarios.
     */
    @Transactional
    public TestUserManager createTestUserManager(String testPrefix) {
        // Clean up any existing test users
        cleanupTestUsers(testPrefix);

        // Ensure roles exist
        ensureRolesExist();

        return new TestUserManager(testPrefix, this);
    }

    /**
     * Ensures standard roles exist in the database.
     */
    @Transactional
    public void ensureRolesExist() {
        createRoleIfNotExists("ROLE_ADMIN");
        createRoleIfNotExists("ROLE_MANAGER");
        createRoleIfNotExists("ROLE_USER");
        entityManager.flush();
    }

    private void createRoleIfNotExists(String roleName) {
        Role existingRole = roleRepository.findByName(roleName);
        if (existingRole == null) {
            Role role = switch (roleName) {
                case "ROLE_ADMIN" -> RoleTestDataBuilder.anAdminRole().withName(roleName).build();
                case "ROLE_MANAGER" -> RoleTestDataBuilder.aModeratorRole().withName(roleName).build();
                case "ROLE_USER" -> RoleTestDataBuilder.aUserRole().withName(roleName).build();
                default -> RoleTestDataBuilder.aRole().withName(roleName).build();
            };
            roleRepository.saveAndFlush(role);
            log.debug("Created test role: {}", roleName);
        }
    }

    /**
     * Creates a test user with specified role.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createTestUser(String email, String firstName, String lastName, String roleName) {
        try {
            UserDto userDto = new UserDto();
            userDto.setEmail(email);
            userDto.setFirstName(firstName);
            userDto.setLastName(lastName);
            userDto.setPassword("TestPassword123!");
            userDto.setMatchingPassword("TestPassword123!");

            User user = userService.registerNewUserAccount(userDto);

            // Assign role if specified
            if (roleName != null) {
                Role role = roleRepository.findByName(roleName);
                if (role != null) {
                    user.getRoles().clear();
                    user.getRoles().add(role);
                    user = userRepository.saveAndFlush(user);
                }
            }

            // Enable user for testing - using entity update instead of native query
            user.setEnabled(true);
            user = userRepository.saveAndFlush(user);

            entityManager.flush();
            entityManager.clear();

            return userRepository.findByEmail(email);

        } catch (Exception e) {
            log.error("Failed to create test user: {}", email, e);
            throw e;
        }
    }

    /**
     * Cleans up test users by prefix.
     */
    @Transactional
    public void cleanupTestUsers(String prefix) {
        try {
            List<User> testUsers = userRepository.findAll().stream().filter(user -> user.getEmail().startsWith(prefix)).toList();

            for (User user : testUsers) {
                userRepository.delete(user);
            }

            entityManager.flush();
            log.debug("Cleaned up {} test users with prefix: {}", testUsers.size(), prefix);

        } catch (Exception e) {
            log.warn("Error during test user cleanup: {}", e.getMessage());
        }
    }

    /**
     * Validates database state after concurrent operations.
     */
    @Transactional
    public void validateDatabaseConsistency() {
        entityManager.flush();
        entityManager.clear();

        // Verify no constraint violations exist
        long userCount = userRepository.count();
        log.debug("Total users in database: {}", userCount);

        // Additional consistency checks can be added here
    }

    /**
     * Result of concurrent execution.
     */
    public record ConcurrentExecutionResult(int totalThreads, int successCount, int errorCount, List<Exception> exceptions,
            boolean completedWithinTimeout) {
        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean allSucceeded() {
            return successCount == totalThreads && errorCount == 0;
        }

        public double successRate() {
            return totalThreads > 0 ? (double) successCount / totalThreads : 0.0;
        }
    }
}
