package com.digitalsanctuary.spring.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import com.digitalsanctuary.spring.user.UserConfiguration;

@ActiveProfiles("test")
@SpringBootTest
@Import(UserConfiguration.class) // Explicitly include your library's configuration
@EnableJpaRepositories(basePackages = {"com.digitalsanctuary.spring.user.persistence.repository", // Library's UserRepository
        "com.digitalsanctuary.spring.demo.user.profile", // Application's DemoUserProfileRepository
        "com.digitalsanctuary.spring.demo.event" // Application's EventRepository
})
@EntityScan(basePackages = {"com.digitalsanctuary.spring.user.persistence.model", "com.digitalsanctuary.spring.demo.user.profile",
        "com.digitalsanctuary.spring.demo.event"})
public class DemoTests {

    @Test
    void contextLoads() {
        // This ensures the entire application context, including your library's configuration, is loaded.
    }

}
