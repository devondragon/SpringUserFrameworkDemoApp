package com.digitalsanctuary.spring.user.security;

import com.digitalsanctuary.spring.demo.UserDemoApplication;
import com.digitalsanctuary.spring.user.audit.AuditEvent;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Basic Audit Logging Tests to verify the audit infrastructure works
 * 
 * This test focuses on testing the audit event publishing mechanism
 * rather than assuming specific events are automatically generated.
 */
@SpringBootTest(classes = UserDemoApplication.class)
@IntegrationTest
@ActiveProfiles("test")
@DisplayName("Basic Audit Logging Tests")
@Import(AuditLoggingBasicTest.TestConfiguration.class)
class AuditLoggingBasicTest {
    
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        @Bean("basicAuditEventCaptor")
        public BasicAuditEventCaptor basicAuditEventCaptor() {
            return new BasicAuditEventCaptor();
        }
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    @Qualifier("basicAuditEventCaptor")
    private BasicAuditEventCaptor auditCaptor;
    
    private static final String TEST_EMAIL = "basic.audit.test@example.com";
    
    @BeforeEach
    void setUp() {
        auditCaptor.clear();
        
        // Clean up any existing test user
        User existingUser = userRepository.findByEmail(TEST_EMAIL);
        if (existingUser != null) {
            userRepository.delete(existingUser);
        }
    }
    
    @AfterEach
    void tearDown() {
        User user = userRepository.findByEmail(TEST_EMAIL);
        if (user != null) {
            userRepository.delete(user);
        }
        auditCaptor.clear();
    }
    
    @Nested
    @DisplayName("Audit Event Publishing Tests")
    class AuditEventPublishingTests {
        
        @Test
        @DisplayName("Should capture manually published audit events")
        void shouldCaptureManuallyPublishedAuditEvents() {
            // Given - Create a test user
            User testUser = new User();
            testUser.setEmail(TEST_EMAIL);
            testUser.setFirstName("Basic");
            testUser.setLastName("Test");
            testUser = userRepository.save(testUser);
            
            // When - Publish an audit event
            AuditEvent auditEvent = AuditEvent.builder()
                .source(this)
                .user(testUser)
                .action("TEST_ACTION")
                .actionStatus("SUCCESS")
                .message("Test audit event message")
                .build();
            
            eventPublisher.publishEvent(auditEvent);
            
            // Then - Verify event was captured
            await().atMost(1, TimeUnit.SECONDS).until(() -> 
                auditCaptor.getAuditEvents().size() > 0);
                
            List<AuditEvent> capturedEvents = auditCaptor.getAuditEvents();
            assertThat(capturedEvents).hasSize(1);
            
            AuditEvent captured = capturedEvents.get(0);
            assertThat(captured.getAction()).isEqualTo("TEST_ACTION");
            assertThat(captured.getActionStatus()).isEqualTo("SUCCESS");
            assertThat(captured.getMessage()).isEqualTo("Test audit event message");
            assertThat(captured.getUser().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(captured.getTimestamp()).isNotNull();
        }
        
        @Test
        @DisplayName("Should capture multiple audit events")
        void shouldCaptureMultipleAuditEvents() {
            // Given - Create a test user
            User testUser = new User();
            testUser.setEmail(TEST_EMAIL);
            testUser.setFirstName("Multiple");
            testUser.setLastName("Events");
            testUser = userRepository.save(testUser);
            
            // When - Publish multiple audit events
            for (int i = 1; i <= 3; i++) {
                AuditEvent auditEvent = AuditEvent.builder()
                    .source(this)
                    .user(testUser)
                    .action("ACTION_" + i)
                    .actionStatus("SUCCESS")
                    .message("Message " + i)
                    .build();
                    
                eventPublisher.publishEvent(auditEvent);
            }
            
            // Then - Verify all events were captured
            await().atMost(2, TimeUnit.SECONDS).until(() -> 
                auditCaptor.getAuditEvents().size() == 3);
                
            List<AuditEvent> capturedEvents = auditCaptor.getAuditEvents();
            assertThat(capturedEvents).hasSize(3);
            
            for (int i = 0; i < 3; i++) {
                AuditEvent captured = capturedEvents.get(i);
                assertThat(captured.getAction()).isEqualTo("ACTION_" + (i + 1));
                assertThat(captured.getMessage()).isEqualTo("Message " + (i + 1));
            }
        }
        
        @Test
        @DisplayName("Should handle audit events with different statuses")
        void shouldHandleAuditEventsWithDifferentStatuses() {
            // Given - Create a test user
            User testUser = new User();
            testUser.setEmail(TEST_EMAIL);
            testUser.setFirstName("Status");
            testUser.setLastName("Test");
            testUser = userRepository.save(testUser);
            
            // When - Publish events with different statuses
            String[] statuses = {"SUCCESS", "FAILURE", "WARNING", "INFO"};
            
            for (String status : statuses) {
                AuditEvent auditEvent = AuditEvent.builder()
                    .source(this)
                    .user(testUser)
                    .action("STATUS_TEST")
                    .actionStatus(status)
                    .message("Testing status: " + status)
                    .build();
                    
                eventPublisher.publishEvent(auditEvent);
            }
            
            // Then - Verify all events were captured with correct statuses
            await().atMost(2, TimeUnit.SECONDS).until(() -> 
                auditCaptor.getAuditEvents().size() == statuses.length);
                
            List<AuditEvent> capturedEvents = auditCaptor.getAuditEvents();
            assertThat(capturedEvents).hasSize(statuses.length);
            
            for (int i = 0; i < statuses.length; i++) {
                AuditEvent captured = capturedEvents.get(i);
                assertThat(captured.getActionStatus()).isEqualTo(statuses[i]);
                assertThat(captured.getMessage()).contains(statuses[i]);
            }
        }
    }
    
    @Nested
    @DisplayName("Audit Event Content Tests")
    class AuditEventContentTests {
        
        @Test
        @DisplayName("Should include all required audit fields")
        void shouldIncludeAllRequiredAuditFields() {
            // Given - Create a test user
            User testUser = new User();
            testUser.setEmail(TEST_EMAIL);
            testUser.setFirstName("Complete");
            testUser.setLastName("Audit");
            testUser = userRepository.save(testUser);
            
            // When - Publish a comprehensive audit event
            AuditEvent auditEvent = AuditEvent.builder()
                .source(this)
                .user(testUser)
                .action("COMPREHENSIVE_TEST")
                .actionStatus("SUCCESS")
                .message("Testing all required fields are present")
                .build();
                
            eventPublisher.publishEvent(auditEvent);
            
            // Then - Verify all fields are present and valid
            await().atMost(1, TimeUnit.SECONDS).until(() -> 
                auditCaptor.getAuditEvents().size() > 0);
                
            AuditEvent captured = auditCaptor.getAuditEvents().get(0);
            
            // Verify all required fields are present
            assertThat(captured.getUser()).isNotNull();
            assertThat(captured.getUser().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(captured.getAction()).isNotNull().isNotBlank();
            assertThat(captured.getActionStatus()).isNotNull().isNotBlank();
            assertThat(captured.getMessage()).isNotNull().isNotBlank();
            assertThat(captured.getTimestamp()).isNotNull();
            assertThat(captured.getSource()).isNotNull();
        }
        
        @Test
        @DisplayName("Should preserve audit event integrity")
        void shouldPreserveAuditEventIntegrity() {
            // Given - Create a test user
            User testUser = new User();
            testUser.setEmail(TEST_EMAIL);
            testUser.setFirstName("Integrity");
            testUser.setLastName("Test");
            testUser = userRepository.save(testUser);
            
            String originalAction = "INTEGRITY_TEST";
            String originalStatus = "SUCCESS";
            String originalMessage = "Testing data integrity preservation";
            
            // When - Publish an audit event
            AuditEvent original = AuditEvent.builder()
                .source(this)
                .user(testUser)
                .action(originalAction)
                .actionStatus(originalStatus)
                .message(originalMessage)
                .build();
                
            eventPublisher.publishEvent(original);
            
            // Then - Verify captured event matches original
            await().atMost(1, TimeUnit.SECONDS).until(() -> 
                auditCaptor.getAuditEvents().size() > 0);
                
            AuditEvent captured = auditCaptor.getAuditEvents().get(0);
            
            assertThat(captured.getAction()).isEqualTo(originalAction);
            assertThat(captured.getActionStatus()).isEqualTo(originalStatus);
            assertThat(captured.getMessage()).isEqualTo(originalMessage);
            assertThat(captured.getUser().getEmail()).isEqualTo(testUser.getEmail());
        }
    }
    
    /**
     * Test utility for capturing audit events during basic integration tests
     */
    @Component
    public static class BasicAuditEventCaptor {
        private final List<AuditEvent> capturedEvents = Collections.synchronizedList(new ArrayList<>());
        
        @EventListener
        public void handleAuditEvent(AuditEvent event) {
            capturedEvents.add(event);
        }
        
        public List<AuditEvent> getAuditEvents() {
            return new ArrayList<>(capturedEvents);
        }
        
        public void clear() {
            capturedEvents.clear();
        }
        
        public int size() {
            return capturedEvents.size();
        }
    }
}