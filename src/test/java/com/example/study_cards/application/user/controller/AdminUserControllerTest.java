package com.example.study_cards.application.user.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class AdminUserControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String adminAccessToken;
    private String userAccessToken;
    private Long normalUserId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        SignUpRequest adminSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "admin@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "adminUser")
                .sample();
        authService.signUp(adminSignUpRequest);

        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        admin.addRole(Role.ROLE_ADMIN);
        admin.verifyEmail();
        userRepository.saveAndFlush(admin);

        adminAccessToken = authService.signIn(new SignInRequest("admin@example.com", "password123")).accessToken();

        SignUpRequest userSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "user@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "normalUser")
                .sample();
        authService.signUp(userSignUpRequest);

        User normalUser = userRepository.findByEmail("user@example.com").orElseThrow();
        normalUser.verifyEmail();
        userRepository.saveAndFlush(normalUser);
        normalUserId = normalUser.getId();

        userAccessToken = authService.signIn(new SignInRequest("user@example.com", "password123")).accessToken();
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetUsersTest {

        @Test
        @DisplayName("관리자가 사용자 목록을 조회한다")
        void getUsers_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("admin-user/get-users",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            queryParameters(
                                    parameterWithName("status").description("사용자 상태 필터(ACTIVE, WITHDRAWN, BANNED)").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void getUsers_user_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{id}")
    class GetUserTest {

        @Test
        @DisplayName("관리자가 사용자 단건을 조회한다")
        void getUser_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", normalUserId)
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(normalUserId))
                    .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()))
                    .andDo(document("admin-user/get-user",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("사용자 ID")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                                    fieldWithPath("roles").type(JsonFieldType.ARRAY).description("권한 목록"),
                                    fieldWithPath("provider").type(JsonFieldType.STRING).description("OAuth 제공자"),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description("사용자 상태"),
                                    fieldWithPath("emailVerified").type(JsonFieldType.BOOLEAN).description("이메일 인증 여부"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시"),
                                    fieldWithPath("modifiedAt").type(JsonFieldType.VARIES).description("수정일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/users/{id}")
    class BanUserTest {

        @Test
        @DisplayName("관리자가 사용자를 제재 처리한다")
        void banUser_admin_success() throws Exception {
            mockMvc.perform(delete("/api/admin/users/{id}", normalUserId)
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("admin-user/ban-user",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("제재 처리할 사용자 ID")
                            )
                    ));

            User bannedUser = userRepository.findById(normalUserId).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(bannedUser.getStatus()).isEqualTo(UserStatus.BANNED);
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void banUser_user_returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/users/{id}", normalUserId)
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }
}
