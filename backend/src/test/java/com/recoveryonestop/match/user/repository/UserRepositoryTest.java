package com.recoveryonestop.match.user.repository;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsAndDetectsUserByLoginId() {
        userRepository.saveAndFlush(User.create("user01", "$2a$10$encoded", "테스트"));

        assertThat(userRepository.existsByLoginId("user01")).isTrue();
        assertThat(userRepository.findByLoginId("user01"))
                .hasValueSatisfying(user -> {
                    assertThat(user.getName()).isEqualTo("테스트");
                    assertThat(user.getRole()).isEqualTo(UserRole.USER);
                    assertThat(user.getCreatedAt()).isNotNull();
                });
    }
}
