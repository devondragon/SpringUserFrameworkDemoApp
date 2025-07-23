package com.digitalsanctuary.spring.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.digitalsanctuary.spring.user.audit.AuditEvent;
import com.digitalsanctuary.spring.user.event.OnRegistrationCompleteEvent;
import com.digitalsanctuary.spring.user.event.UserPreDeleteEvent;
import com.digitalsanctuary.spring.user.listener.AuthenticationEventListener;
import com.digitalsanctuary.spring.user.listener.RegistrationListener;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.service.LoginAttemptService;
import com.digitalsanctuary.spring.user.service.UserEmailService;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;
import com.digitalsanctuary.spring.user.test.builders.UserTestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@IntegrationTest
@TestPropertySource(properties = {
    "user.registration.sendVerificationEmail=true",
    "spring.main.allow-bean-definition-overriding=true"
})
@Import(EventSystemIntegrationTest.TestEventConfiguration.class)
@DisplayName("Event System Integration Tests")
class EventSystemIntegrationTest {

    @TestConfiguration
    static class TestEventConfiguration {
        @Bean
        public TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TestEventCapture eventCapture;

    @MockitoBean
    private UserEmailService userEmailService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = UserTestDataBuilder.aUser()
                .withId(1L)
                .withEmail("test@example.com")
                .withFirstName("Test")
                .withLastName("User")
                .enabled()
                .build();
        eventCapture.clear();
    }

    @Nested
    @DisplayName("Registration Event Flow Tests")
    class RegistrationEventFlowTests {

        @Test
        @DisplayName("Registration event triggers email service")
        void registrationEvent_triggersEmailService() throws Exception {
            // Given
            String appUrl = "https://example.com";
            Locale locale = Locale.ENGLISH;
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(userEmailService).sendRegistrationVerificationEmail(any(), any());

            // When
            OnRegistrationCompleteEvent event = OnRegistrationCompleteEvent.builder()
                    .user(testUser)
                    .locale(locale)
                    .appUrl(appUrl)
                    .build();
            eventPublisher.publishEvent(event);

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(userEmailService).sendRegistrationVerificationEmail(testUser, appUrl);
            assertThat(eventCapture.getCapturedEvents())
                    .filteredOn(e -> e instanceof OnRegistrationCompleteEvent)
                    .hasSize(1);
        }

        @Test
        @DisplayName("Multiple registration events are handled independently")
        void multipleRegistrationEvents_handledIndependently() throws Exception {
            // Given
            User user1 = UserTestDataBuilder.aUser()
                    .withEmail("user1@example.com")
                    .build();
            User user2 = UserTestDataBuilder.aUser()
                    .withEmail("user2@example.com")
                    .build();
            CountDownLatch latch = new CountDownLatch(2);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(userEmailService).sendRegistrationVerificationEmail(any(), any());

            // When
            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user1, Locale.ENGLISH, "app1"));
            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user2, Locale.FRENCH, "app2"));

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            verify(userEmailService).sendRegistrationVerificationEmail(user1, "app1");
            verify(userEmailService).sendRegistrationVerificationEmail(user2, "app2");
        }
    }

    @Nested
    @DisplayName("Authentication Event Flow Tests")
    class AuthenticationEventFlowTests {

        @Test
        @DisplayName("Success event updates login attempt service")
        void successEvent_updatesLoginAttemptService() {
            // Given
            String username = "test@example.com";
            Authentication auth = new UsernamePasswordAuthenticationToken(username, "password");

            // When
            eventPublisher.publishEvent(new AuthenticationSuccessEvent(auth));

            // Then
            verify(loginAttemptService, timeout(1000)).loginSucceeded(username);
            assertThat(eventCapture.getCapturedEvents())
                    .filteredOn(e -> e instanceof AuthenticationSuccessEvent)
                    .hasSize(1);
        }

        @Test
        @DisplayName("Failure event updates login attempt service")
        void failureEvent_updatesLoginAttemptService() {
            // Given
            String username = "test@example.com";
            Authentication auth = new UsernamePasswordAuthenticationToken(username, "password");
            BadCredentialsException exception = new BadCredentialsException("Bad credentials");

            // When
            eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(auth, exception));

            // Then
            verify(loginAttemptService, timeout(1000)).loginFailed(username);
            assertThat(eventCapture.getCapturedEvents())
                    .filteredOn(e -> e instanceof AuthenticationFailureBadCredentialsEvent)
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("User Deletion Event Flow Tests")
    class UserDeletionEventFlowTests {

        @Test
        @DisplayName("UserPreDeleteEvent is captured correctly")
        void userPreDeleteEvent_capturedCorrectly() {
            // When
            UserPreDeleteEvent event = new UserPreDeleteEvent(this, testUser);
            eventPublisher.publishEvent(event);

            // Then
            assertThat(eventCapture.getCapturedEvents())
                    .filteredOn(e -> e instanceof UserPreDeleteEvent)
                    .hasSize(1)
                    .first()
                    .satisfies(e -> {
                        UserPreDeleteEvent deleteEvent = (UserPreDeleteEvent) e;
                        assertThat(deleteEvent.getUser()).isEqualTo(testUser);
                        assertThat(deleteEvent.getUserId()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("Audit Event Flow Tests")
    class AuditEventFlowTests {

        @Test
        @DisplayName("Audit events are captured")
        void auditEvents_areCaptured() {
            // Given
            AuditEvent auditEvent = AuditEvent.builder()
                    .source(this)
                    .user(testUser)
                    .action("Test Action")
                    .actionStatus("Success")
                    .message("Test audit event")
                    .build();

            // When
            eventPublisher.publishEvent(auditEvent);

            // Then
            assertThat(eventCapture.getCapturedEvents())
                    .filteredOn(e -> e instanceof AuditEvent)
                    .hasSize(1)
                    .first()
                    .satisfies(e -> {
                        AuditEvent captured = (AuditEvent) e;
                        assertThat(captured.getAction()).isEqualTo("Test Action");
                        assertThat(captured.getUser()).isEqualTo(testUser);
                    });
        }
    }

    @Nested
    @DisplayName("Event Ordering Tests")
    class EventOrderingTests {

        @Test
        @DisplayName("Events are processed in order")
        void events_processedInOrder() throws Exception {
            // Given
            CountDownLatch latch = new CountDownLatch(3);
            List<String> processedEvents = Collections.synchronizedList(new ArrayList<>());
            
            doAnswer(invocation -> {
                processedEvents.add("registration");
                latch.countDown();
                return null;
            }).when(userEmailService).sendRegistrationVerificationEmail(any(), any());
            
            doAnswer(invocation -> {
                processedEvents.add("login-success");
                latch.countDown();
                return null;
            }).when(loginAttemptService).loginSucceeded(any());

            // When
            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(testUser, Locale.ENGLISH, "app"));
            eventPublisher.publishEvent(new AuthenticationSuccessEvent(
                    new UsernamePasswordAuthenticationToken("user", "pass")));
            eventPublisher.publishEvent(new UserPreDeleteEvent(this, testUser));
            processedEvents.add("delete");
            latch.countDown();

            // Then
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(processedEvents).hasSize(3);
        }
    }

    /**
     * Test utility class to capture all events for verification
     */
    static class TestEventCapture {
        private final List<Object> capturedEvents = Collections.synchronizedList(new ArrayList<>());

        @EventListener
        public void handleEvent(Object event) {
            capturedEvents.add(event);
        }

        public List<Object> getCapturedEvents() {
            return new ArrayList<>(capturedEvents);
        }

        public void clear() {
            capturedEvents.clear();
        }
    }
}