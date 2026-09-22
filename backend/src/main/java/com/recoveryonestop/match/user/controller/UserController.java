package com.recoveryonestop.match.user.controller;

import com.recoveryonestop.match.security.CustomUserDetails;
import com.recoveryonestop.match.user.dto.UserResponse;
import com.recoveryonestop.match.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.getCurrentUser(principal.getUserId());
    }
}
