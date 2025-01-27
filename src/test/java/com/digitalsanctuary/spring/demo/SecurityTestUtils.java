package com.digitalsanctuary.spring.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class SecurityTestUtils {

    public static RequestPostProcessor mockUserWithCsrf() {
        return request -> {
            user("testUser").roles("USER").postProcessRequest(request);
            csrf().postProcessRequest(request);
            return request;
        };
    }
}
