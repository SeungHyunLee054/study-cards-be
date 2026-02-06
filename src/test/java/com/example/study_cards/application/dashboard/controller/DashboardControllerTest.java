package com.example.study_cards.application.dashboard.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class DashboardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);
        verifyUserEmail("test@example.com");

        SignInRequest signInRequest = new SignInRequest("test@example.com", "password123");
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/dashboard")
    class GetDashboardTest {

        @Test
        @DisplayName("대시보드 정보를 조회한다")
        void getDashboard_success() throws Exception {
            mockMvc.perform(get("/api/dashboard")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user").exists())
                    .andExpect(jsonPath("$.today").exists())
                    .andExpect(jsonPath("$.recommendation").exists())
                    .andDo(document("dashboard/get-dashboard",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("user").type(JsonFieldType.OBJECT).description("사용자 요약 정보"),
                                    fieldWithPath("user.id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                    fieldWithPath("user.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                    fieldWithPath("user.streak").type(JsonFieldType.NUMBER).description("연속 학습 일수"),
                                    fieldWithPath("user.level").type(JsonFieldType.NUMBER).description("레벨"),
                                    fieldWithPath("user.totalStudied").type(JsonFieldType.NUMBER).description("총 학습 수"),
                                    fieldWithPath("today").type(JsonFieldType.OBJECT).description("오늘 학습 정보"),
                                    fieldWithPath("today.dueCards").type(JsonFieldType.NUMBER).description("복습 예정 카드 수"),
                                    fieldWithPath("today.newCardsAvailable").type(JsonFieldType.NUMBER).description("새 카드 수"),
                                    fieldWithPath("today.studiedToday").type(JsonFieldType.NUMBER).description("오늘 학습한 카드 수"),
                                    fieldWithPath("today.todayAccuracy").type(JsonFieldType.NUMBER).description("오늘 정답률"),
                                    fieldWithPath("categoryProgress").type(JsonFieldType.ARRAY).description("카테고리별 진행 상황"),
                                    fieldWithPath("categoryProgress[].categoryCode").type(JsonFieldType.STRING).description("카테고리 코드").optional(),
                                    fieldWithPath("categoryProgress[].totalCards").type(JsonFieldType.NUMBER).description("총 카드 수").optional(),
                                    fieldWithPath("categoryProgress[].studiedCards").type(JsonFieldType.NUMBER).description("학습한 카드 수").optional(),
                                    fieldWithPath("categoryProgress[].progressRate").type(JsonFieldType.NUMBER).description("진행률").optional(),
                                    fieldWithPath("categoryProgress[].masteryRate").type(JsonFieldType.NUMBER).description("숙련도").optional(),
                                    fieldWithPath("recentActivity").type(JsonFieldType.ARRAY).description("최근 활동"),
                                    fieldWithPath("recentActivity[].date").type(JsonFieldType.STRING).description("날짜").optional(),
                                    fieldWithPath("recentActivity[].studied").type(JsonFieldType.NUMBER).description("학습 수").optional(),
                                    fieldWithPath("recentActivity[].correct").type(JsonFieldType.NUMBER).description("정답 수").optional(),
                                    fieldWithPath("recentActivity[].accuracy").type(JsonFieldType.NUMBER).description("정답률").optional(),
                                    fieldWithPath("recommendation").type(JsonFieldType.OBJECT).description("학습 추천"),
                                    fieldWithPath("recommendation.message").type(JsonFieldType.STRING).description("추천 메시지"),
                                    fieldWithPath("recommendation.recommendedCategory").type(JsonFieldType.STRING).description("추천 카테고리").optional(),
                                    fieldWithPath("recommendation.cardsToStudy").type(JsonFieldType.NUMBER).description("추천 학습 카드 수").optional(),
                                    fieldWithPath("recommendation.type").type(JsonFieldType.STRING).description("추천 유형")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getDashboard_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/dashboard"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
