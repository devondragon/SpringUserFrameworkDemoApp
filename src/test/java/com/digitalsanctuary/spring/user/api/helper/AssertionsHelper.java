package com.digitalsanctuary.spring.user.api.helper;

import com.digitalsanctuary.spring.user.api.data.Response;
import com.digitalsanctuary.spring.user.json.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.springframework.mock.web.MockHttpServletResponse;


public class AssertionsHelper {
    public static void compareResponses(MockHttpServletResponse servletResponse, Response expected) throws Exception{
        String content = servletResponse.getContentAsString();
        if (content == null || content.isEmpty()) {
            // Handle empty response (typically from Spring Security blocking the request)
            if (servletResponse.getStatus() == 401 || servletResponse.getStatus() == 403) {
                // For unauthorized/forbidden, check if we expected an error response
                if (expected != null && !expected.isSuccess()) {
                    // Test passes - we got an auth error as expected
                    return;
                }
            }
            throw new AssertionError("Empty response body. Status: " + servletResponse.getStatus());
        }
        Response actual = JsonUtil.readValue(content, Response.class);
        Assertions.assertEquals(actual, expected);
    }
}
