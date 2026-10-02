package com.recoveryonestop.match.auth.service;

import com.recoveryonestop.match.auth.domain.RefreshToken;
import com.recoveryonestop.match.auth.domain.RefreshTokenRepository;
import com.recoveryonestop.match.auth.dto.LoginRequest;
import com.recoveryonestop.match.auth.dto.LoginResponse;
import com.recoveryonestop.match.auth.jwt.JwtTokenProvider;
import com.recoveryonestop.match.auth.jwt.TokenClaims;
import com.recoveryonestop.match.auth.jwt.ExpiredJwtException;
import com.recoveryonestop.match.auth.jwt.InvalidJwtException;
import com.recoveryonestop.match.common.exception.InvalidCredentialsException;
import com.recoveryonestop.match.common.exception.InvalidRefreshTokenException;
import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;
import com.recoveryonestop.match.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTokenTest {

    private static final Instant NOW = Instant.parse("2026-09-22T00:00:00Z");

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtTokenProvider jwtTokenProvider;

    private RefreshTokenHasher refreshTokenHasher;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenHasher = new RefreshTokenHasher();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                refreshTokenRepository,
                jwtTokenProvider,
                refreshTokenHasher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        user = User.create("user01", "$2a$10$encoded", "테스트");
    }

    @Test
    void loginUsesMatchesAndStoresOnlyRefreshTokenHash() {
        when(userRepository.findByLoginId("user01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("access.jwt");
        when(jwtTokenProvider.createRefreshToken(user)).thenReturn("refresh.jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1_209_600_000L);
        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        AuthTokens result = authService.login(new LoginRequest("user01", "password123!"));

        verify(passwordEncoder).matches("password123!", user.getPassword());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(refreshTokenHasher.hash("refresh.jwt"));
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo("refresh.jwt");
        assertThat(result.accessToken()).isEqualTo("access.jwt");
        assertThat(result.refreshToken()).isEqualTo("refresh.jwt");
    }

    @Test
    void rejectsUnknownLoginIdWithUnifiedMessage() {
        when(userRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "password123!")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void rejectsWrongPasswordWithUnifiedMessage() {
        when(userRepository.findByLoginId("user01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void newLoginReplacesExistingRefreshTokenForSameUser() {
        RefreshToken existing = RefreshToken.issue(
                user,
                "a".repeat(64),
                LocalDateTime.ofInstant(NOW.plusSeconds(60), ZoneOffset.UTC)
        );
        when(userRepository.findByLoginId("user01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("new-access.jwt");
        when(jwtTokenProvider.createRefreshToken(user)).thenReturn("new-refresh.jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1_209_600_000L);
        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        authService.login(new LoginRequest("user01", "password123!"));

        assertThat(existing.getTokenHash()).isEqualTo(refreshTokenHasher.hash("new-refresh.jwt"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshIssuesNewAccessTokenWhenJwtAndDatabaseMatch() {
        RefreshToken stored = futureTokenOwnedBy(user, "refresh.jwt");
        when(jwtTokenProvider.parseRefreshToken("refresh.jwt"))
                .thenReturn(new TokenClaims("user01", UserRole.USER));
        when(refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash("refresh.jwt")))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("new-access.jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);

        LoginResponse response = authService.refresh("refresh.jwt");

        assertThat(response.accessToken()).isEqualTo("new-access.jwt");
        assertThat(response.expiresIn()).isEqualTo(1800);
    }

    @Test
    void rejectsRefreshTokenMissingFromDatabaseAfterReplacement() {
        when(jwtTokenProvider.parseRefreshToken("old-refresh.jwt"))
                .thenReturn(new TokenClaims("user01", UserRole.USER));
        when(refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash("old-refresh.jwt")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh.jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsRefreshTokenWhoseStoredUserDoesNotMatchSubject() {
        User anotherUser = User.create("other01", "$2a$10$encoded", "다른 사용자");
        RefreshToken stored = futureTokenOwnedBy(anotherUser, "refresh.jwt");
        when(jwtTokenProvider.parseRefreshToken("refresh.jwt"))
                .thenReturn(new TokenClaims("user01", UserRole.USER));
        when(refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash("refresh.jwt")))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("refresh.jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsDatabaseExpiredRefreshToken() {
        RefreshToken expired = RefreshToken.issue(
                user,
                refreshTokenHasher.hash("refresh.jwt"),
                LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC)
        );
        when(jwtTokenProvider.parseRefreshToken("refresh.jwt"))
                .thenReturn(new TokenClaims("user01", UserRole.USER));
        when(refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash("refresh.jwt")))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("refresh.jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsCryptographicallyExpiredRefreshToken() {
        when(jwtTokenProvider.parseRefreshToken("expired-refresh.jwt"))
                .thenThrow(new ExpiredJwtException());

        assertThatThrownBy(() -> authService.refresh("expired-refresh.jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsRefreshTokenWithInvalidSignature() {
        when(jwtTokenProvider.parseRefreshToken("forged-refresh.jwt"))
                .thenThrow(new InvalidJwtException());

        assertThatThrownBy(() -> authService.refresh("forged-refresh.jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutDeletesRefreshTokenByHash() {
        authService.logout("refresh.jwt");

        verify(refreshTokenRepository).deleteByTokenHash(refreshTokenHasher.hash("refresh.jwt"));
    }

    private RefreshToken futureTokenOwnedBy(User owner, String rawToken) {
        return RefreshToken.issue(
                owner,
                refreshTokenHasher.hash(rawToken),
                LocalDateTime.ofInstant(NOW.plusSeconds(60), ZoneOffset.UTC)
        );
    }
}
