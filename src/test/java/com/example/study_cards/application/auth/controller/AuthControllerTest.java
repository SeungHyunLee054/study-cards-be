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
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.cookies.CookieDocumentation.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
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
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(signUpRequest.email()))
                    .andExpect(jsonPath("$.nickname").value(signUpRequest.nickname()))
                    .andDo(document("auth/signup",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (8자 이상)"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                                    fieldWithPath("streak").type(JsonFieldType.NUMBER).description("연속 학습 일수").optional(),
                                    fieldWithPath("masteryRate").type(JsonFieldType.NUMBER).description("숙련도").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400 Bad Request를 반환한다")
        void signUp_withBlankEmail_returns400() throws Exception {
            SignUpRequest invalidRequest = new SignUpRequest("", "password123", "nickname");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 이메일 형식이면 400 Bad Request를 반환한다")
        void signUp_withInvalidEmail_returns400() throws Exception {
            SignUpRequest invalidRequest = new SignUpRequest("invalid-email", "password123", "nickname");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
        void signUp_withShortPassword_returns400() throws Exception {
            SignUpRequest invalidRequest = new SignUpRequest("test@example.com", "short", "nickname");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("닉네임이 비어있으면 400 Bad Request를 반환한다")
        void signUp_withBlankNickname_returns400() throws Exception {
            SignUpRequest invalidRequest = new SignUpRequest("test@example.com", "password123", "");

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
            authService.signUp(signUpRequest);

            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andDo(document("auth/signin",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                            ),
                            responseFields(
                                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                                    fieldWithPath("tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"),
                                    fieldWithPath("expiresIn").type(JsonFieldType.NUMBER).description("토큰 만료 시간 (ms)")
                            ),
                            responseCookies(
                                    cookieWithName(CookieProvider.REFRESH_TOKEN_COOKIE_NAME).description("리프레시 토큰 쿠키")
                            )
                    ));
        }

        @Test
        @DisplayName("로그인 시 RefreshToken 쿠키를 설정한다")
        void signIn_setsRefreshTokenCookie() throws Exception {
            authService.signUp(signUpRequest);

            MvcResult result = mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andReturn();

            Cookie refreshTokenCookie = result.getResponse().getCookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("이메일이 비어있으면 400 Bad Request를 반환한다")
        void signIn_withBlankEmail_returns400() throws Exception {
            SignInRequest invalidRequest = new SignInRequest("", "password123");

            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 비어있으면 400 Bad Request를 반환한다")
        void signIn_withBlankPassword_returns400() throws Exception {
            SignInRequest invalidRequest = new SignInRequest("test@example.com", "");

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
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            mockMvc.perform(post("/api/auth/signout")
                            .header("Authorization", "Bearer " + tokenResult.accessToken()))
                    .andExpect(status().isNoContent())
                    .andDo(document("auth/signout",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            )
                    ));
        }

        @Test
        @DisplayName("로그아웃 시 RefreshToken 쿠키를 삭제한다")
        void signOut_deletesRefreshTokenCookie() throws Exception {
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            MvcResult result = mockMvc.perform(post("/api/auth/signout")
                            .header("Authorization", "Bearer " + tokenResult.accessToken()))
                    .andReturn();

            Cookie refreshTokenCookie = result.getResponse().getCookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME);
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getMaxAge()).isEqualTo(0);
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void signOut_withoutAuth_returns401() throws Exception {
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
            authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE_NAME, tokenResult.refreshToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andDo(document("auth/refresh",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestCookies(
                                    cookieWithName(CookieProvider.REFRESH_TOKEN_COOKIE_NAME).description("리프레시 토큰 쿠키")
                            ),
                            responseFields(
                                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
                                    fieldWithPath("tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"),
                                    fieldWithPath("expiresIn").type(JsonFieldType.NUMBER).description("토큰 만료 시간 (ms)")
                            )
                    ));
        }
    }
}
