package com.recoveryonestop.match.auth.jwt;

import com.recoveryonestop.match.user.domain.UserRole;

public record TokenClaims(String loginId, UserRole role) {
}
