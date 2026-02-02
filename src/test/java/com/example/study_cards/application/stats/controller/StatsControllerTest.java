package com.example.study_cards.application.stats.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.user.entity.User;
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

import java.time.LocalDate;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class StatsControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    private String accessToken;
    private User user;

    @BeforeEach
    void setUp() {
        studyRecordRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);

        SignInRequest signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .sample();

        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();
        user = userRepository.findByEmail("test@example.com").orElseThrow();
    }

    @Nested
    @DisplayName("GET /api/stats")
    class GetStatsEndpointTest {

        @Test
        @DisplayName("통계 조회 성공 시 200 OK를 반환한다")
        void getStats_success_returns200() throws Exception {
            // given
            Card card = Card.builder()
                    .questionEn("What is Java?")
                    .answerEn("A programming language")
                    .category(Category.CS)
                    .build();
            cardRepository.save(card);

            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();
            studyRecordRepository.save(record);

            // when & then
            mockMvc.perform(get("/api/stats")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overview").exists())
                    .andExpect(jsonPath("$.overview.dueToday").isNumber())
                    .andExpect(jsonPath("$.overview.totalStudied").isNumber())
                    .andExpect(jsonPath("$.overview.newCards").isNumber())
                    .andExpect(jsonPath("$.overview.streak").isNumber())
                    .andExpect(jsonPath("$.overview.accuracyRate").isNumber())
                    .andExpect(jsonPath("$.deckStats").isArray())
                    .andExpect(jsonPath("$.deckStats[0].masteryRate").isNumber())
                    .andExpect(jsonPath("$.recentActivity").isArray())
                    .andDo(document("stats/get-stats",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("overview").type(JsonFieldType.OBJECT).description("학습 개요"),
                                    fieldWithPath("overview.dueToday").type(JsonFieldType.NUMBER).description("오늘 복습해야 할 카드 수"),
                                    fieldWithPath("overview.totalStudied").type(JsonFieldType.NUMBER).description("총 학습한 카드 수"),
                                    fieldWithPath("overview.newCards").type(JsonFieldType.NUMBER).description("아직 학습하지 않은 새 카드 수"),
                                    fieldWithPath("overview.streak").type(JsonFieldType.NUMBER).description("연속 학습 일수"),
                                    fieldWithPath("overview.accuracyRate").type(JsonFieldType.NUMBER).description("전체 정답률 (%)"),
                                    fieldWithPath("deckStats").type(JsonFieldType.ARRAY).description("카테고리별 통계"),
                                    fieldWithPath("deckStats[].category").type(JsonFieldType.STRING).description("카테고리명"),
                                    fieldWithPath("deckStats[].newCount").type(JsonFieldType.NUMBER).description("새 카드 수"),
                                    fieldWithPath("deckStats[].learningCount").type(JsonFieldType.NUMBER).description("학습 중인 카드 수"),
                                    fieldWithPath("deckStats[].reviewCount").type(JsonFieldType.NUMBER).description("복습 카드 수"),
                                    fieldWithPath("deckStats[].masteryRate").type(JsonFieldType.NUMBER).description("마스터리율 (%)"),
                                    fieldWithPath("recentActivity").type(JsonFieldType.ARRAY).description("최근 학습 활동"),
                                    fieldWithPath("recentActivity[].date").type(JsonFieldType.STRING).description("학습 날짜"),
                                    fieldWithPath("recentActivity[].studied").type(JsonFieldType.NUMBER).description("학습한 카드 수"),
                                    fieldWithPath("recentActivity[].correct").type(JsonFieldType.NUMBER).description("정답 카드 수")
                            )
                    ));
        }

        @Test
        @DisplayName("학습 기록이 없어도 통계를 반환한다")
        void getStats_noStudyRecord_returnsStats() throws Exception {
            // given
            Card card = Card.builder()
                    .questionEn("What is Java?")
                    .answerEn("A programming language")
                    .category(Category.CS)
                    .build();
            cardRepository.save(card);

            // when & then
            mockMvc.perform(get("/api/stats")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overview.dueToday").value(0))
                    .andExpect(jsonPath("$.overview.totalStudied").value(0))
                    .andExpect(jsonPath("$.overview.newCards").value(1))
                    .andExpect(jsonPath("$.overview.accuracyRate").value(0.0))
                    .andExpect(jsonPath("$.recentActivity").isEmpty());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void getStats_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/stats"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
