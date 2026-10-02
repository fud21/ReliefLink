package com.recoveryonestop.match.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
