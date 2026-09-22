package com.recoveryonestop.match.auth.service;

import com.recoveryonestop.match.auth.domain.RefreshToken;
import com.recoveryonestop.match.auth.domain.RefreshTokenRepository;
import com.recoveryonestop.match.auth.dto.LoginRequest;
import com.recoveryonestop.match.auth.dto.LoginResponse;
import com.recoveryonestop.match.auth.dto.SignupRequest;
import com.recoveryonestop.match.auth.jwt.ExpiredJwtException;
import com.recoveryonestop.match.auth.jwt.InvalidJwtException;
import com.recoveryonestop.match.auth.jwt.JwtTokenProvider;
import com.recoveryonestop.match.auth.jwt.TokenClaims;
import com.recoveryonestop.match.common.exception.DuplicateLoginIdException;
import com.recoveryonestop.match.common.exception.InvalidCredentialsException;
import com.recoveryonestop.match.common.exception.InvalidRefreshTokenException;
import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.dto.UserResponse;
import com.recoveryonestop.match.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenHasher refreshTokenHasher,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new DuplicateLoginIdException();
        }

        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name()
        );

        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateLoginIdException();
        }
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        String tokenHash = refreshTokenHasher.hash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                clock.instant().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()),
                ZoneOffset.UTC
        );

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        existing -> existing.replace(tokenHash, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.issue(user, tokenHash, expiresAt))
                );

        return new AuthTokens(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(rawRefreshToken);
        } catch (InvalidJwtException | ExpiredJwtException exception) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!stored.getExpiresAt().isAfter(now)
                || !stored.getUser().getLoginId().equals(claims.loginId())) {
            throw new InvalidRefreshTokenException();
        }

        return new LoginResponse(
                jwtTokenProvider.createAccessToken(stored.getUser()),
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByTokenHash(refreshTokenHasher.hash(rawRefreshToken));
    }
}
