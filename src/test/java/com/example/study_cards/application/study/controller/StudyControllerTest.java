package com.example.study_cards.application.study.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class StudyControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    private String accessToken;
    private Card testCard;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        cardRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "study@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "studyUser")
                .sample();
        authService.signUp(signUpRequest);

        SignInRequest signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "study@example.com")
                .set("password", "password123")
                .sample();
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        testCard = Card.builder()
                .questionEn("What is Java?")
                .questionKo("자바란 무엇인가?")
                .answerEn("Java is a programming language")
                .answerKo("자바는 프로그래밍 언어입니다")
                .efFactor(2.5)
                .category(Category.CS)
                .build();
        testCard = cardRepository.save(testCard);
    }

    @Nested
    @DisplayName("GET /api/study/cards")
    class GetTodayCardsTest {

        @Test
        @DisplayName("오늘 학습할 카드 목록을 반환한다")
        void getTodayCards_success_returns200() throws Exception {
            mockMvc.perform(get("/api/study/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testCard.getId()))
                    .andExpect(jsonPath("$[0].questionEn").value("What is Java?"))
                    .andExpect(jsonPath("$[0].category").value("CS"))
                    .andDo(document("study/get-today-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카드 카테고리 (기본값: CS)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리")
                            )
                    ));
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void getTodayCards_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/study/cards"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/study/answer")
    class SubmitAnswerTest {

        @Test
        @DisplayName("정답 제출 성공 시 200 OK와 결과를 반환한다")
        void submitAnswer_success_returns200() throws Exception {
            StudyAnswerRequest request = new StudyAnswerRequest(testCard.getId(), true);

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cardId").value(testCard.getId()))
                    .andExpect(jsonPath("$.isCorrect").value(true))
                    .andExpect(jsonPath("$.nextReviewDate").isNotEmpty())
                    .andExpect(jsonPath("$.newEfFactor").isNumber())
                    .andDo(document("study/submit-answer",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("cardId").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("isCorrect").type(JsonFieldType.BOOLEAN).description("정답 여부")
                            ),
                            responseFields(
                                    fieldWithPath("cardId").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("isCorrect").type(JsonFieldType.BOOLEAN).description("정답 여부"),
                                    fieldWithPath("nextReviewDate").type(JsonFieldType.STRING).description("다음 복습 날짜 (ISO 8601 형식)"),
                                    fieldWithPath("newEfFactor").type(JsonFieldType.NUMBER).description("업데이트된 EF 계수")
                            )
                    ));
        }

        @Test
        @DisplayName("오답 제출 시에도 200 OK와 결과를 반환한다")
        void submitAnswer_incorrect_returns200() throws Exception {
            StudyAnswerRequest request = new StudyAnswerRequest(testCard.getId(), false);

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cardId").value(testCard.getId()))
                    .andExpect(jsonPath("$.isCorrect").value(false))
                    .andExpect(jsonPath("$.nextReviewDate").isNotEmpty());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void submitAnswer_withoutAuth_returns401() throws Exception {
            StudyAnswerRequest request = new StudyAnswerRequest(testCard.getId(), true);

            mockMvc.perform(post("/api/study/answer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("카드 ID가 없으면 400 Bad Request를 반환한다")
        void submitAnswer_withoutCardId_returns400() throws Exception {
            String invalidRequest = "{\"isCorrect\": true}";

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("정답 여부가 없으면 400 Bad Request를 반환한다")
        void submitAnswer_withoutIsCorrect_returns400() throws Exception {
            String invalidRequest = "{\"cardId\": " + testCard.getId() + "}";

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
