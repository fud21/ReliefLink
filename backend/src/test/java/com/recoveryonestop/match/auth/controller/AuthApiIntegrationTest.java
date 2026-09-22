package com.recoveryonestop.match.auth.controller;

import com.recoveryonestop.match.auth.domain.RefreshTokenRepository;
import com.recoveryonestop.match.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signupReturnsCreatedSafeUser() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loginId").value("user01"))
                .andExpect(jsonPath("$.name").value("테스트사용자"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(header().string(SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(content().string(not(containsString("refreshToken"))));
    }

    @Test
    void invalidCredentialsUseSamePublicMessage() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user01\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"missing\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void refreshCookieIssuesNewAccessToken() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutDeletesRefreshSessionClearsCookieAndPreventsReuse() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")));

        assertThat(refreshTokenRepository.count()).isZero();
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCookieIsIdempotentAndStillClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void secondLoginReplacesFirstRefreshButFirstAccessTokenRemainsValid() throws Exception {
        signup();
        MvcResult firstLogin = performLogin();
        Cookie firstRefresh = firstLogin.getResponse().getCookie("refreshToken");
        String firstAccess = accessTokenFrom(firstLogin);

        MvcResult secondLogin = performLogin();
        Cookie secondRefresh = secondLogin.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mockMvc.perform(post("/api/auth/refresh").cookie(secondRefresh))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/users/me")
                        .header(AUTHORIZATION, "Bearer " + firstAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("user01"));
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson()))
                .andExpect(status().isCreated());
    }

    private Cookie loginAndGetRefreshCookie() throws Exception {
        signup();
        MvcResult result = performLogin();
        Cookie cookie = result.getResponse().getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private MvcResult performLogin() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessTokenFrom(MvcResult loginResult) throws Exception {
        String body = loginResult.getResponse().getContentAsString();
        int valueStart = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int valueEnd = body.indexOf('"', valueStart);
        return body.substring(valueStart, valueEnd);
    }

    private String signupJson() {
        return "{\"loginId\":\"user01\",\"password\":\"password123!\",\"name\":\"테스트사용자\"}";
    }

    private String loginJson() {
        return "{\"loginId\":\"user01\",\"password\":\"password123!\"}";
    }
}
