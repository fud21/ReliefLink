package com.recoveryonestop.match.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    @Test
    void accessDeniedHandlerReturnsJsonForbiddenResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler().handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }
}
