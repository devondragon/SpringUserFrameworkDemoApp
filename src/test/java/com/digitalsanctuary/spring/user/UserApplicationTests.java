package com.digitalsanctuary.spring.user;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Import(UserConfiguration.class) // Explicitly include your library's configuration
@EnableJpaRepositories(basePackages = {"com.digitalsanctuary.spring.user.persistence.repository", // Library's UserRepository
        "com.digitalsanctuary.spring.demo.user.profile", // Application's DemoUserProfileRepository
        "com.digitalsanctuary.spring.demo.event" // Application's EventRepository
})
@EntityScan(basePackages = {"com.digitalsanctuary.spring.user.persistence.model", "com.digitalsanctuary.spring.demo.user.profile",
        "com.digitalsanctuary.spring.demo.event"})
class UserApplicationTests {

}
