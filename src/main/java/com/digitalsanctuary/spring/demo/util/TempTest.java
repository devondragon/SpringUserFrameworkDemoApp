package com.digitalsanctuary.spring.demo.util;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a class that is used to test the library's functionality outside of the JUnit test context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempTest {

    private final DemoUserProfileRepository demoUserProfileRepository;

    @Transactional
    @EventListener(ApplicationStartedEvent.class)
    public void test() {
        log.info("This is a test");
        log.info("{}", demoUserProfileRepository.findAll());
    }

}
