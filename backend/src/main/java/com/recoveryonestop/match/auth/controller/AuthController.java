package com.recoveryonestop.match.auth.controller;

import com.recoveryonestop.match.auth.dto.LoginRequest;
import com.recoveryonestop.match.auth.dto.LoginResponse;
import com.recoveryonestop.match.auth.dto.SignupRequest;
import com.recoveryonestop.match.auth.jwt.JwtProperties;
import com.recoveryonestop.match.auth.service.AuthService;
import com.recoveryonestop.match.auth.service.AuthTokens;
import com.recoveryonestop.match.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(tokens.accessTokenResponse());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return cookieBuilder(refreshToken)
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return cookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH);
    }
}
