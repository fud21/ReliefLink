package com.recoveryonestop.match.auth.domain;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void keepsOneRefreshTokenPerUserAndFindsItByHash() {
        User user = userRepository.saveAndFlush(
                User.create("user01", "$2a$10$encoded", "테스트"));
        String tokenHash = "a".repeat(64);

        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(user, tokenHash, LocalDateTime.now().plusDays(14)));

        assertThat(refreshTokenRepository.findByUserId(user.getId())).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash(tokenHash))
                .hasValueSatisfying(token -> assertThat(token.getUser().getId()).isEqualTo(user.getId()));
    }
}
