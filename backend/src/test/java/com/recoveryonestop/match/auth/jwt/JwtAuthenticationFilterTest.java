package com.recoveryonestop.match.auth.jwt;

import com.recoveryonestop.match.security.CustomUserDetailsService;
import com.recoveryonestop.match.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock FilterChain filterChain;

    @Test
    void doesNotHideUnexpectedUserStoreFailureAsAuthenticationFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                userDetailsService
        );
        when(jwtTokenProvider.parseAccessToken("access.jwt"))
                .thenReturn(new TokenClaims("user01", UserRole.USER));
        when(userDetailsService.loadUserByUsername("user01"))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }
}
