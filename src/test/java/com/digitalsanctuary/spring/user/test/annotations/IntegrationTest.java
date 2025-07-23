package com.digitalsanctuary.spring.user.test.annotations;

import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Composite annotation for integration tests that require a full Spring context.
 * This annotation combines common integration test setup including:
 * - Full Spring Boot test context
 * - Mock MVC configuration
 * - Test profile activation
 * - Transaction management
 * - All test configurations
 * 
 * Usage:
 * <pre>
 * @IntegrationTest
 * class UserServiceIntegrationTest {
 *     // Test methods
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(classes = com.digitalsanctuary.spring.demo.UserDemoApplication.class)
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ActiveProfiles("test")
@Transactional
public @interface IntegrationTest {
    
    /**
     * Whether to rollback transactions after each test method.
     * Default is true to ensure test isolation.
     */
    boolean rollback() default true;
    
    /**
     * Additional Spring profiles to activate.
     */
    String[] additionalProfiles() default {};
}