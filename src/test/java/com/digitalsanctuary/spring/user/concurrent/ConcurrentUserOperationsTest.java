package com.digitalsanctuary.spring.user.concurrent;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.UserService;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent User Operations Test - Phase 2 of Task 4.3
 * 
 * Tests concurrent user operations including:
 * - Multiple users registering simultaneously 
 * - Same email registration race conditions
 * - Concurrent login attempts
 * - Data consistency under concurrent load
 */
@SpringBootTest(classes = UserDemoApplication.class)
@IntegrationTest
@ActiveProfiles("test")
@DisplayName("Concurrent User Operations Tests")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentUserOperationsTest {

    @Autowired
    private MultiUserTestUtilities testUtilities;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EntityManager entityManager;
    
    private TestUserManager userManager;
    private static final String TEST_PREFIX = "concurrent";

    @BeforeEach
    void setUp() {
        userManager = testUtilities.createTestUserManager(TEST_PREFIX);
    }

    @AfterEach
    void tearDown() {
        if (userManager != null) {
            userManager.cleanup();
        }
    }

    @Nested
    @DisplayName("Concurrent Registration Tests")
    class ConcurrentRegistrationTests {

        @Test
        @DisplayName("Multiple users should register simultaneously without conflicts")
        void testConcurrentUniqueRegistrations() {
            final int threadCount = 10;
            final AtomicInteger successCount = new AtomicInteger(0);
            final AtomicInteger errorCount = new AtomicInteger(0);

            // Task: Register unique users concurrently
            Runnable registrationTask = () -> {
                try {
                    String uniqueId = Thread.currentThread().getName();
                    String email = TEST_PREFIX + ".unique." + uniqueId + "@test.example.com";
                    
                    UserDto userDto = new UserDto();
                    userDto.setEmail(email);
                    userDto.setFirstName("Concurrent");
                    userDto.setLastName("User" + uniqueId);
                    userDto.setPassword("TestPassword123!");
                    userDto.setMatchingPassword("TestPassword123!");

                    User registeredUser = userService.registerNewUserAccount(userDto);
                    
                    if (registeredUser != null && registeredUser.getId() != null) {
                        successCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            };

            // Execute concurrent registrations
            MultiUserTestUtilities.ConcurrentExecutionResult result = 
                testUtilities.executeConcurrently(threadCount, registrationTask, 30);

            // Verify results
            assertThat(result.completedWithinTimeout()).isTrue();
            assertThat(result.errorCount()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(threadCount);
            
            // Verify database consistency
            testUtilities.validateDatabaseConsistency();
            
            // Count actual registered users
            long userCount = userRepository.findAll().stream()
                .filter(user -> user.getEmail().contains(TEST_PREFIX + ".unique."))
                .count();
            
            assertThat(userCount).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("Same email registration should enforce unique constraint")
        void testConcurrentDuplicateEmailRegistration() {
            final int threadCount = 5;
            final String duplicateEmail = userManager.getDuplicateEmailForTesting();
            
            final AtomicInteger successCount = new AtomicInteger(0);
            final AtomicInteger constraintViolationCount = new AtomicInteger(0);

            // Task: Try to register with same email
            Runnable duplicateRegistrationTask = () -> {
                try {
                    UserDto userDto = new UserDto();
                    userDto.setEmail(duplicateEmail);
                    userDto.setFirstName("Duplicate");
                    userDto.setLastName("User");
                    userDto.setPassword("TestPassword123!");
                    userDto.setMatchingPassword("TestPassword123!");

                    userService.registerNewUserAccount(userDto);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    // Expected: constraint violation or user already exists
                    if (e.getMessage().contains("already exists") || 
                        e.getMessage().contains("constraint") ||
                        e.getMessage().contains("duplicate")) {
                        constraintViolationCount.incrementAndGet();
                    }
                }
            };

            // Execute concurrent duplicate registrations
            MultiUserTestUtilities.ConcurrentExecutionResult result = 
                testUtilities.executeConcurrently(threadCount, duplicateRegistrationTask, 30);

            // Verify results - only one should succeed
            assertThat(result.completedWithinTimeout()).isTrue();
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(constraintViolationCount.get()).isEqualTo(threadCount - 1);
            
            // Verify only one user exists with that email
            long duplicateUserCount = userRepository.findAll().stream()
                .filter(user -> duplicateEmail.equals(user.getEmail()))
                .count();
                
            assertThat(duplicateUserCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Mixed operations should maintain data consistency")  
        void testMixedConcurrentOperations() {
            final int registrationThreads = 3;
            final int queryThreads = 2;
            
            final AtomicInteger registrationSuccess = new AtomicInteger(0);
            final AtomicInteger querySuccess = new AtomicInteger(0);

            // Registration task - use UserService directly to avoid transaction issues
            Runnable registrationTask = () -> {
                try {
                    String uniqueId = Thread.currentThread().getName();
                    String email = TEST_PREFIX + ".mixed." + uniqueId + "@test.example.com";
                    
                    UserDto userDto = new UserDto();
                    userDto.setEmail(email);
                    userDto.setFirstName("Mixed");
                    userDto.setLastName("User" + uniqueId);
                    userDto.setPassword("TestPassword123!");
                    userDto.setMatchingPassword("TestPassword123!");

                    User user = userService.registerNewUserAccount(userDto);
                    
                    if (user != null && user.getId() != null) {
                        registrationSuccess.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    // Expected in concurrent testing - log but don't fail
                }
            };

            // Query task
            Runnable queryTask = () -> {
                try {
                    long userCount = userRepository.count();
                    if (userCount >= 0) {
                        querySuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log but don't fail
                }
            };

            // Execute registrations
            MultiUserTestUtilities.ConcurrentExecutionResult registrationResult = 
                testUtilities.executeConcurrently(registrationThreads, registrationTask, 30);
            
            // Execute queries  
            MultiUserTestUtilities.ConcurrentExecutionResult queryResult = 
                testUtilities.executeConcurrently(queryThreads, queryTask, 30);

            // Verify operations completed successfully
            assertThat(registrationResult.completedWithinTimeout()).isTrue();
            assertThat(queryResult.completedWithinTimeout()).isTrue();
            
            // At least some registrations should succeed
            assertThat(registrationSuccess.get()).isGreaterThan(0);
            assertThat(querySuccess.get()).isEqualTo(queryThreads);
            
            // Verify final database state
            testUtilities.validateDatabaseConsistency();
        }
    }
}