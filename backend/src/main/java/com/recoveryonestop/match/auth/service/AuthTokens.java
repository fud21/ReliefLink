package com.recoveryonestop.match.auth.service;

import com.recoveryonestop.match.auth.dto.LoginResponse;

public record AuthTokens(String accessToken, String refreshToken, long expiresIn) {

    public LoginResponse accessTokenResponse() {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
