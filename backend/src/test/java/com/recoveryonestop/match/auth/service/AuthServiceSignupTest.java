package com.recoveryonestop.match.auth.service;

import com.recoveryonestop.match.auth.domain.RefreshTokenRepository;
import com.recoveryonestop.match.auth.dto.SignupRequest;
import com.recoveryonestop.match.auth.jwt.JwtTokenProvider;
import com.recoveryonestop.match.common.exception.DuplicateLoginIdException;
import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.dto.UserResponse;
import com.recoveryonestop.match.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                mock(RefreshTokenRepository.class),
                mock(JwtTokenProvider.class),
                new RefreshTokenHasher(),
                Clock.systemUTC()
        );
    }

    @Test
    void signupStoresBcryptHashAndNeverReturnsPassword() {
        when(userRepository.existsByLoginId("user01")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.signup(
                new SignupRequest("user01", "password123!", "테스트"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(passwordEncoder.matches("password123!", saved.getPassword())).isTrue();
        assertThat(saved.getPassword()).isNotEqualTo("password123!");
        assertThat(response.loginId()).isEqualTo("user01");
    }

    @Test
    void rejectsExistingLoginId() {
        when(userRepository.existsByLoginId("user01")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("user01", "password123!", "테스트")))
                .isInstanceOf(DuplicateLoginIdException.class);
    }

    @Test
    void translatesDatabaseUniquenessRaceToDuplicateLoginId() {
        when(userRepository.existsByLoginId("user01")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_login_id"));

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("user01", "password123!", "테스트")))
                .isInstanceOf(DuplicateLoginIdException.class);
    }
}
