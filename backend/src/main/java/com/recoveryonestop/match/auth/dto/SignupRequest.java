package com.recoveryonestop.match.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4자 이상 50자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name
) {
}
