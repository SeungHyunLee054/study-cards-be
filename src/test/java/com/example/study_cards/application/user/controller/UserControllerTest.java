package com.example.study_cards.application.user.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.user.dto.request.PasswordChangeRequest;
import com.example.study_cards.application.user.dto.request.UserUpdateRequest;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String NICKNAME = "testUser";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", EMAIL)
                .set("password", PASSWORD)
                .set("passwordConfirm", PASSWORD)
                .set("nickname", NICKNAME)
                .sample();
        authService.signUp(signUpRequest);
        verifyUserEmail(EMAIL);

        SignInRequest signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", EMAIL)
                .set("password", PASSWORD)
                .sample();
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetMyInfoEndpointTest {

        @Test
        @DisplayName("내 정보 조회 성공 시 200 OK와 사용자 정보를 반환한다")
        void getMyInfo_success_returns200() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.nickname").value(NICKNAME))
                    .andDo(document("user/get-my-info",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                                    fieldWithPath("roles").type(JsonFieldType.ARRAY).description("사용자 역할"),
                                    fieldWithPath("provider").type(JsonFieldType.STRING).description("OAuth 제공자"),
                                    fieldWithPath("streak").type(JsonFieldType.NUMBER).description("연속 학습 일수"),
                                    fieldWithPath("masteryRate").type(JsonFieldType.NUMBER).description("숙련도")
                            )
                    ));
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void getMyInfo_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me")
    class UpdateMyInfoEndpointTest {

        @Test
        @DisplayName("내 정보 수정 성공 시 200 OK와 수정된 사용자 정보를 반환한다")
        void updateMyInfo_success_returns200() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("newNickname");

            mockMvc.perform(patch("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("newNickname"))
                    .andDo(document("user/update-my-info",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("새 닉네임 (2~20자)")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("수정된 닉네임"),
                                    fieldWithPath("roles").type(JsonFieldType.ARRAY).description("사용자 역할"),
                                    fieldWithPath("provider").type(JsonFieldType.STRING).description("OAuth 제공자"),
                                    fieldWithPath("streak").type(JsonFieldType.NUMBER).description("연속 학습 일수"),
                                    fieldWithPath("masteryRate").type(JsonFieldType.NUMBER).description("숙련도")
                            )
                    ));
        }

        @Test
        @DisplayName("닉네임이 비어있으면 400 Bad Request를 반환한다")
        void updateMyInfo_withBlankNickname_returns400() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("");

            mockMvc.perform(patch("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("닉네임이 2자 미만이면 400 Bad Request를 반환한다")
        void updateMyInfo_withShortNickname_returns400() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("a");

            mockMvc.perform(patch("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void updateMyInfo_withoutAuth_returns401() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("newNickname");

            mockMvc.perform(patch("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me/password")
    class ChangePasswordEndpointTest {

        @Test
        @DisplayName("비밀번호 변경 성공 시 204 No Content를 반환한다")
        void changePassword_success_returns204() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest(PASSWORD, "newPassword123");

            mockMvc.perform(patch("/api/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andDo(document("user/change-password",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("currentPassword").type(JsonFieldType.STRING).description("현재 비밀번호"),
                                    fieldWithPath("newPassword").type(JsonFieldType.STRING).description("새 비밀번호 (8~20자)")
                            )
                    ));
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 401 Unauthorized를 반환한다")
        void changePassword_wrongCurrentPassword_returns401() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("wrongPassword", "newPassword123");

            mockMvc.perform(patch("/api/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
        void changePassword_shortNewPassword_returns400() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest(PASSWORD, "short");

            mockMvc.perform(patch("/api/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void changePassword_withoutAuth_returns401() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest(PASSWORD, "newPassword123");

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
