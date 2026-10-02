package com.recoveryonestop.match.security;

import com.recoveryonestop.match.auth.domain.RefreshTokenRepository;
import com.recoveryonestop.match.auth.jwt.JwtTokenProvider;
import com.recoveryonestop.match.service.BenefitProgramIngestService;
import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class AuthenticatedUserApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    BenefitProgramIngestService benefitProgramIngestService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.saveAndFlush(
                User.create("user01", "$2a$10$encoded", "테스트"));
    }

    @Test
    void meWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithValidAccessTokenReturnsSafeUser() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/users/me")
                        .header(AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.loginId").value("user01"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void meRejectsMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(AUTHORIZATION, "Bearer malformed.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsRefreshTokenUsedAsBearer() throws Exception {
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        mockMvc.perform(get("/api/users/me")
                        .header(AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingMatchEndpointRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void localAdminSyncEndpointsRemainPublic() throws Exception {
        mockMvc.perform(post("/api/admin/sync-central"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/sync-local"))
                .andExpect(status().isOk());
    }
}
