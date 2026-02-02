package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class CardControllerTest extends BaseIntegrationTest {

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
        cardRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "card@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "cardUser")
                .sample();
        authService.signUp(signUpRequest);

        SignInRequest signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "card@example.com")
                .set("password", "password123")
                .sample();
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        testCard = Card.builder()
                .questionEn("What is polymorphism?")
                .questionKo("다형성이란 무엇인가?")
                .answerEn("Polymorphism is the ability of an object to take on many forms")
                .answerKo("다형성은 객체가 여러 형태를 가질 수 있는 능력입니다")
                .efFactor(2.5)
                .category(Category.CS)
                .build();
        testCard = cardRepository.save(testCard);
    }

    @Nested
    @DisplayName("GET /api/cards")
    class GetCardsTest {

        @Test
        @DisplayName("모든 카드를 조회한다")
        void getCards_success_returns200() throws Exception {
            mockMvc.perform(get("/api/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testCard.getId()))
                    .andExpect(jsonPath("$[0].questionEn").value("What is polymorphism?"))
                    .andExpect(jsonPath("$[0].cardType").value("PUBLIC"))
                    .andDo(document("card/get-all",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("category").description("카드 카테고리 (CS, ENGLISH, SQL, JAPANESE)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("[].efFactor").type(JsonFieldType.NUMBER).description("EF 계수"),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("카테고리별 카드를 조회한다")
        void getCards_byCategory_returns200() throws Exception {
            mockMvc.perform(get("/api/cards")
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("CS"));
        }
    }

    @Nested
    @DisplayName("GET /api/cards/{id}")
    class GetCardTest {

        @Test
        @DisplayName("카드 ID로 카드를 조회한다")
        void getCard_success_returns200() throws Exception {
            mockMvc.perform(get("/api/cards/{id}", testCard.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCard.getId()))
                    .andExpect(jsonPath("$.questionEn").value("What is polymorphism?"))
                    .andExpect(jsonPath("$.cardType").value("PUBLIC"))
                    .andDo(document("card/get-one",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("id").description("카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("efFactor").type(JsonFieldType.NUMBER).description("EF 계수"),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 카드 조회 시 404를 반환한다")
        void getCard_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/cards/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/study")
    class GetCardsForStudyTest {

        @Test
        @DisplayName("인증된 사용자는 제한 없이 학습 카드를 조회한다")
        void getCardsForStudy_authenticated_returns200() throws Exception {
            mockMvc.perform(get("/api/cards/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testCard.getId()))
                    .andExpect(jsonPath("$[0].cardType").value("PUBLIC"))
                    .andDo(document("card/get-study",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (선택)").optional()
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카드 카테고리 (선택)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("[].efFactor").type(JsonFieldType.NUMBER).description("EF 계수"),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("비인증 사용자도 학습 카드를 조회할 수 있다")
        void getCardsForStudy_unauthenticated_returns200() throws Exception {
            mockMvc.perform(get("/api/cards/study"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/all")
    class GetAllCardsWithUserCardsTest {

        @Test
        @DisplayName("인증된 사용자가 공용 + 커스텀 카드를 통합 조회한다")
        void getAllCardsWithUserCards_success() throws Exception {
            mockMvc.perform(get("/api/cards/all")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testCard.getId()))
                    .andExpect(jsonPath("$[0].cardType").value("PUBLIC"))
                    .andDo(document("card/get-all-with-user-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카드 카테고리 (선택)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("[].efFactor").type(JsonFieldType.NUMBER).description("EF 계수"),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("비인증 사용자는 통합 조회에 접근할 수 없다")
        void getAllCardsWithUserCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/cards/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/study/all")
    class GetCardsForStudyWithUserCardsTest {

        @Test
        @DisplayName("인증된 사용자가 공용 + 커스텀 학습 카드를 통합 조회한다")
        void getCardsForStudyWithUserCards_success() throws Exception {
            mockMvc.perform(get("/api/cards/study/all")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testCard.getId()))
                    .andExpect(jsonPath("$[0].cardType").value("PUBLIC"))
                    .andDo(document("card/get-study-all",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카드 카테고리 (선택)").optional()
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 답변"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 답변").optional(),
                                    fieldWithPath("[].efFactor").type(JsonFieldType.NUMBER).description("EF 계수"),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("비인증 사용자는 통합 학습 조회에 접근할 수 없다")
        void getCardsForStudyWithUserCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/cards/study/all"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
