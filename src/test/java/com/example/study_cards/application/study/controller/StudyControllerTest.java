package com.example.study_cards.application.study.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.card.dto.response.CardType;
import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
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

import java.time.LocalDateTime;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    private String accessToken;
    private User user;
    private Category category;
    private Card card;

    @BeforeEach
    void setUp() {
        studySessionRepository.deleteAll();
        cardRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);
        user = userRepository.findByEmail("test@example.com").orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);

        SignInRequest signInRequest = new SignInRequest("test@example.com", "password123");
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        category = categoryRepository.save(Category.builder()
                .code("CS")
                .name("Computer Science")
                .build());

        card = cardRepository.save(Card.builder()
                .question("What is a variable?")
                .questionSub("변수란 무엇인가요?")
                .answer("A storage location paired with an associated symbolic name")
                .answerSub("심볼릭 이름과 연결된 저장 위치")
                .category(category)
                .build());
    }

    @Nested
    @DisplayName("GET /api/study/cards")
    class GetTodayCardsTest {

        @Test
        @DisplayName("오늘의 학습 카드를 조회한다")
        void getTodayCards_success() throws Exception {
            mockMvc.perform(get("/api/study/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("study/get-today-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드 (기본값: CS)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("학습 카드 목록"),
                                    fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("content[].question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("content[].questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("content[].answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("content[].answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("content[].category").type(JsonFieldType.OBJECT).description("카테고리 정보"),
                                    fieldWithPath("content[].category.id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("content[].category.code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("content[].category.name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("content[].category.parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("content[].category.parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("content[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                    fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 여부"),
                                    fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 아님 여부"),
                                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                    fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("number").type(JsonFieldType.NUMBER).description("페이지 번호"),
                                    fieldWithPath("sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                    fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                    fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getTodayCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/cards"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/study/answer")
    class SubmitAnswerTest {

        @Test
        @DisplayName("정답을 제출한다")
        void submitAnswer_success() throws Exception {
            StudyAnswerRequest request = new StudyAnswerRequest(card.getId(), CardType.PUBLIC, true);

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cardId").value(card.getId()))
                    .andExpect(jsonPath("$.cardType").value("PUBLIC"))
                    .andExpect(jsonPath("$.isCorrect").value(true))
                    .andDo(document("study/submit-answer",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("cardId").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("isCorrect").type(JsonFieldType.BOOLEAN).description("정답 여부")
                            ),
                            responseFields(
                                    fieldWithPath("cardId").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("isCorrect").type(JsonFieldType.BOOLEAN).description("정답 여부"),
                                    fieldWithPath("nextReviewDate").type(JsonFieldType.STRING).description("다음 복습 날짜"),
                                    fieldWithPath("newEfFactor").type(JsonFieldType.NUMBER).description("새 EF 팩터")
                            )
                    ));
        }

        @Test
        @DisplayName("카드 ID가 없으면 400을 반환한다")
        void submitAnswer_nullCardId_returns400() throws Exception {
            String request = """
                    {
                        "isCorrect": true
                    }
                    """;

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("정답 여부가 없으면 400을 반환한다")
        void submitAnswer_nullIsCorrect_returns400() throws Exception {
            String request = """
                    {
                        "cardId": 1
                    }
                    """;

            mockMvc.perform(post("/api/study/answer")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/study/sessions")
    class GetSessionHistoryTest {

        @Test
        @DisplayName("학습 세션 이력을 조회한다")
        void getSessionHistory_success() throws Exception {
            mockMvc.perform(get("/api/study/sessions")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("study/get-session-history",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("세션 목록"),
                                    fieldWithPath("pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                    fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 여부"),
                                    fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 아님 여부"),
                                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                    fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("number").type(JsonFieldType.NUMBER).description("페이지 번호"),
                                    fieldWithPath("sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                    fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                    fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getSessionHistory_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/sessions"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/study/sessions/end")
    class EndCurrentSessionTest {

        @Test
        @DisplayName("활성 세션을 종료한다")
        void endCurrentSession_success() throws Exception {
            // given - 활성 세션 생성
            studySessionRepository.save(StudySession.builder().user(user).build());

            // when & then
            mockMvc.perform(put("/api/study/sessions/end")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.endedAt").isNotEmpty())
                    .andExpect(jsonPath("$.totalCards").value(0));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void endCurrentSession_unauthorized_returns401() throws Exception {
            mockMvc.perform(put("/api/study/sessions/end"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/study/sessions/current")
    class GetCurrentSessionTest {

        @Test
        @DisplayName("현재 활성 세션을 조회한다")
        void getCurrentSession_success() throws Exception {
            // given
            studySessionRepository.save(StudySession.builder().user(user).build());

            // when & then
            mockMvc.perform(get("/api/study/sessions/current")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.startedAt").isNotEmpty())
                    .andExpect(jsonPath("$.endedAt").isEmpty());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getCurrentSession_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/sessions/current"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/study/sessions/{sessionId}")
    class GetSessionTest {

        @Test
        @DisplayName("특정 세션을 조회한다")
        void getSession_success() throws Exception {
            // given
            StudySession session = studySessionRepository.save(StudySession.builder().user(user).build());

            // when & then
            mockMvc.perform(get("/api/study/sessions/{sessionId}", session.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(session.getId()))
                    .andExpect(jsonPath("$.startedAt").isNotEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 세션 조회 시 404를 반환한다")
        void getSession_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/study/sessions/{sessionId}", 99999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getSession_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/sessions/{sessionId}", 1L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/study/sessions/{sessionId}/stats")
    class GetSessionStatsTest {

        @Test
        @DisplayName("세션 통계를 조회한다")
        void getSessionStats_success() throws Exception {
            // given
            StudySession session = studySessionRepository.save(StudySession.builder().user(user).build());

            // when & then
            mockMvc.perform(get("/api/study/sessions/{sessionId}/stats", session.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(session.getId()))
                    .andExpect(jsonPath("$.totalCards").value(0))
                    .andExpect(jsonPath("$.correctCount").value(0))
                    .andExpect(jsonPath("$.records").isArray());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getSessionStats_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/sessions/{sessionId}/stats", 1L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/study/recommendations")
    class GetRecommendationsTest {

        @Test
        @DisplayName("학습 추천을 조회한다")
        void getRecommendations_success() throws Exception {
            mockMvc.perform(get("/api/study/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.totalCount").isNumber());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getRecommendations_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/recommendations"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/study/category-accuracy")
    class GetCategoryAccuracyTest {

        @Test
        @DisplayName("카테고리별 정확도를 조회한다")
        void getCategoryAccuracy_success() throws Exception {
            mockMvc.perform(get("/api/study/category-accuracy")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getCategoryAccuracy_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/study/category-accuracy"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
