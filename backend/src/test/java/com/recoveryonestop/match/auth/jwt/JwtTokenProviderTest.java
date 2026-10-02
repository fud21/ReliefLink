package com.recoveryonestop.match.auth.jwt;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-09-22T00:00:00Z");
    private static final String SECRET = encodeSecret("relieflink-test-secret-key-material-01");
    private static final String OTHER_SECRET = encodeSecret("relieflink-other-secret-key-material-2");

    private User user;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        user = User.create("user01", "$2a$10$encoded", "테스트");
        provider = provider(SECRET, ISSUED_AT);
    }

    @Test
    void createsAndParsesAccessToken() {
        String token = provider.createAccessToken(user);

        TokenClaims claims = provider.parseAccessToken(token);

        assertThat(claims.loginId()).isEqualTo("user01");
        assertThat(claims.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String forged = provider(OTHER_SECRET, ISSUED_AT).createAccessToken(user);

        assertThatThrownBy(() -> provider.parseAccessToken(forged))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rejectsExpiredAccessToken() {
        String token = provider.createAccessToken(user);
        JwtTokenProvider laterProvider = provider(
                SECRET,
                ISSUED_AT.plus(Duration.ofMinutes(31))
        );

        assertThatThrownBy(() -> laterProvider.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        String refreshToken = provider.createRefreshToken(user);

        assertThatThrownBy(() -> provider.parseAccessToken(refreshToken))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void reportsAccessTokenExpirationInSeconds() {
        assertThat(provider.getAccessTokenExpirationSeconds()).isEqualTo(1800);
    }

    private static JwtTokenProvider provider(String secret, Instant now) {
        JwtProperties properties = new JwtProperties(secret, 1_800_000, 1_209_600_000, false);
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        return new JwtTokenProvider(properties, clock);
    }

    private static String encodeSecret(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
