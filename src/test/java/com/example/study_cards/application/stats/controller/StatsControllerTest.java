package com.example.study_cards.application.stats.controller;

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
class StatsControllerTest extends BaseIntegrationTest {

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
    @DisplayName("GET /api/stats")
    class GetStatsTest {

        @Test
        @DisplayName("통계 정보를 조회한다")
        void getStats_success() throws Exception {
            mockMvc.perform(get("/api/stats")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overview").exists())
                    .andExpect(jsonPath("$.overview.totalStudied").isNumber())
                    .andExpect(jsonPath("$.overview.streak").isNumber())
                    .andDo(document("stats/get-stats",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("overview").type(JsonFieldType.OBJECT).description("전체 통계 요약"),
                                    fieldWithPath("overview.dueToday").type(JsonFieldType.NUMBER).description("오늘 복습 예정 카드 수"),
                                    fieldWithPath("overview.totalStudied").type(JsonFieldType.NUMBER).description("총 학습 카드 수"),
                                    fieldWithPath("overview.newCards").type(JsonFieldType.NUMBER).description("새 카드 수"),
                                    fieldWithPath("overview.streak").type(JsonFieldType.NUMBER).description("연속 학습 일수"),
                                    fieldWithPath("overview.accuracyRate").type(JsonFieldType.NUMBER).description("정답률"),
                                    fieldWithPath("deckStats").type(JsonFieldType.ARRAY).description("덱별 통계"),
                                    fieldWithPath("deckStats[].category").type(JsonFieldType.STRING).description("카테고리").optional(),
                                    fieldWithPath("deckStats[].newCount").type(JsonFieldType.NUMBER).description("새 카드 수").optional(),
                                    fieldWithPath("deckStats[].learningCount").type(JsonFieldType.NUMBER).description("학습 중 카드 수").optional(),
                                    fieldWithPath("deckStats[].reviewCount").type(JsonFieldType.NUMBER).description("복습 카드 수").optional(),
                                    fieldWithPath("deckStats[].masteryRate").type(JsonFieldType.NUMBER).description("숙련도").optional(),
                                    fieldWithPath("recentActivity").type(JsonFieldType.ARRAY).description("최근 활동"),
                                    fieldWithPath("recentActivity[].date").type(JsonFieldType.STRING).description("날짜").optional(),
                                    fieldWithPath("recentActivity[].studied").type(JsonFieldType.NUMBER).description("학습 수").optional(),
                                    fieldWithPath("recentActivity[].correct").type(JsonFieldType.NUMBER).description("정답 수").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getStats_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/stats"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
