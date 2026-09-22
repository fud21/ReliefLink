package com.recoveryonestop.match.security;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final UserRole role;

    private CustomUserDetails(Long userId, String loginId, String password, UserRole role) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getLoginId(),
                user.getPassword(),
                user.getRole()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }
}
