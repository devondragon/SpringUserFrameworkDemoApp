package com.digitalsanctuary.spring.user.test.annotations;

import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Composite annotation for integration tests without automatic transaction management.
 * Use this when you need to manage transactions manually or test transaction boundaries.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ActiveProfiles("test")
public @interface IntegrationTestNoTx {
    
    /**
     * Additional Spring profiles to activate.
     */
    String[] additionalProfiles() default {};
}