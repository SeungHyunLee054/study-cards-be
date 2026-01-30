package com.example.study_cards.application.auth.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.security.jwt.CookieProvider;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("nickname", "testUser")
                .sample();

        signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .sample();
    }

    @Nested
    @DisplayName("POST /api/auth/signup")
    class SignUpEndpointTest {

        @Test
        @DisplayName("회원가입 성공 시 201 Created를 반환한다")
        void signUp_success_returns201() throws Exception {
            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(signUpRequest.email()))
                    .andExpect(jsonPath("$.nickname").value(signUpRequest.nickname()));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400 Bad Request를 반환한다")
        void signUp_withBlankEmail_returns400() throws Exception {
            // given
            SignUpRequest invalidRequest = new SignUpRequest("", "password123", "nickname");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 이메일 형식이면 400 Bad Request를 반환한다")
        void signUp_withInvalidEmail_returns400() throws Exception {
            // given
            SignUpRequest invalidRequest = new SignUpRequest("invalid-email", "password123", "nickname");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
        void signUp_withShortPassword_returns400() throws Exception {
            // given
            SignUpRequest invalidRequest = new SignUpRequest("test@example.com", "short", "nickname");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("닉네임이 비어있으면 400 Bad Request를 반환한다")
        void signUp_withBlankNickname_returns400() throws Exception {
            // given
            SignUpRequest invalidRequest = new SignUpRequest("test@example.com", "password123", "");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signin")
    class SignInEndpointTest {

        @Test
        @DisplayName("로그인 성공 시 200 OK와 토큰을 반환한다")
        void signIn_success_returns200WithToken() throws Exception {
            // given
            authService.signUp(signUpRequest);

            // when & then
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber());
        }

        @Test
        @DisplayName("로그인 시 RefreshToken 쿠키를 설정한다")
        void signIn_setsRefreshTokenCookie() throws Exception {
            // given
            authService.signUp(signUpRequest);

            // when
            MvcResult result = mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andReturn();

            // then
            Cookie refreshTokenCookie = result.getResponse().getCookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("이메일이 비어있으면 400 Bad Request를 반환한다")
        void signIn_withBlankEmail_returns400() throws Exception {
            // given
            SignInRequest invalidRequest = new SignInRequest("", "password123");

            // when & then
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 비어있으면 400 Bad Request를 반환한다")
        void signIn_withBlankPassword_returns400() throws Exception {
            // given
            SignInRequest invalidRequest = new SignInRequest("test@example.com", "");

            // when & then
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signout")
    class SignOutEndpointTest {

        @Test
        @DisplayName("로그아웃 성공 시 204 No Content를 반환한다")
        void signOut_success_returns204() throws Exception {
            // given
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when & then
            mockMvc.perform(post("/api/auth/signout")
                            .header("Authorization", "Bearer " + tokenResult.accessToken()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("로그아웃 시 RefreshToken 쿠키를 삭제한다")
        void signOut_deletesRefreshTokenCookie() throws Exception {
            // given
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when
            MvcResult result = mockMvc.perform(post("/api/auth/signout")
                            .header("Authorization", "Bearer " + tokenResult.accessToken()))
                    .andReturn();

            // then
            Cookie refreshTokenCookie = result.getResponse().getCookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getMaxAge()).isEqualTo(0);
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void signOut_withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/auth/signout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshEndpointTest {

        @Test
        @DisplayName("토큰 갱신 성공 시 200 OK와 새 AccessToken을 반환한다")
        void refresh_success_returns200WithNewToken() throws Exception {
            // given
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME, tokenResult.refreshToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber());
        }
    }
}
