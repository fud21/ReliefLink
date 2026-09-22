package com.recoveryonestop.match.user.dto;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;

public record UserResponse(
        Long id,
        String loginId,
        String name,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getRole()
        );
    }
}
