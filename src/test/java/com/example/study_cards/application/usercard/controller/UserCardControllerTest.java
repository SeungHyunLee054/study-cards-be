package com.example.study_cards.application.usercard.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.domain.card.entity.Category;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class UserCardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;
    private UserCardCreateRequest createRequest;
    private UserCardUpdateRequest updateRequest;

    private static final String QUESTION_EN = "What is JPA?";
    private static final String QUESTION_KO = "JPA란 무엇인가요?";
    private static final String ANSWER_EN = "Java Persistence API";
    private static final String ANSWER_KO = "자바 영속성 API";

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

        SignInRequest signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .sample();
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        createRequest = fixtureMonkey.giveMeBuilder(UserCardCreateRequest.class)
                .set("questionEn", QUESTION_EN)
                .set("questionKo", QUESTION_KO)
                .set("answerEn", ANSWER_EN)
                .set("answerKo", ANSWER_KO)
                .set("category", Category.CS)
                .sample();

        updateRequest = fixtureMonkey.giveMeBuilder(UserCardUpdateRequest.class)
                .set("questionEn", "Updated question")
                .set("questionKo", QUESTION_KO)
                .set("answerEn", "Updated answer")
                .set("answerKo", ANSWER_KO)
                .set("category", Category.CS)
                .sample();
    }

    @Nested
    @DisplayName("POST /api/user/cards")
    class CreateUserCardTest {

        @Test
        @DisplayName("사용자 카드 생성에 성공한다")
        void createUserCard_success() throws Exception {
            mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.questionEn").value(QUESTION_EN))
                    .andExpect(jsonPath("$.answerEn").value(ANSWER_EN))
                    .andExpect(jsonPath("$.category").value("CS"))
                    .andDo(document("user-cards/create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("efFactor").type(JsonFieldType.NUMBER).description("난이도 계수"),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void createUserCard_unauthorized_returns401() throws Exception {
            mockMvc.perform(post("/api/user/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/user/cards")
    class GetUserCardsTest {

        @Test
        @DisplayName("사용자 카드 목록 조회에 성공한다")
        void getUserCards_success() throws Exception {
            // 카드 생성
            mockMvc.perform(post("/api/user/cards")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)));

            mockMvc.perform(get("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].questionEn").value(QUESTION_EN))
                    .andDo(document("user-cards/list",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("[].questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("[].questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("[].answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("[].answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("[].efFactor").type(JsonFieldType.NUMBER).description("난이도 계수"),
                                    fieldWithPath("[].category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/user/cards/{id}")
    class GetUserCardTest {

        @Test
        @DisplayName("사용자 카드 단건 조회에 성공한다")
        void getUserCard_success() throws Exception {
            // 카드 생성
            MvcResult result = mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(get("/api/user/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cardId))
                    .andExpect(jsonPath("$.questionEn").value(QUESTION_EN))
                    .andDo(document("user-cards/get",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("efFactor").type(JsonFieldType.NUMBER).description("난이도 계수"),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PUT /api/user/cards/{id}")
    class UpdateUserCardTest {

        @Test
        @DisplayName("사용자 카드 수정에 성공한다")
        void updateUserCard_success() throws Exception {
            // 카드 생성
            MvcResult result = mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(put("/api/user/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionEn").value("Updated question"))
                    .andExpect(jsonPath("$.answerEn").value("Updated answer"))
                    .andDo(document("user-cards/update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("questionEn").type(JsonFieldType.STRING).description("영문 질문"),
                                    fieldWithPath("questionKo").type(JsonFieldType.STRING).description("한글 질문").optional(),
                                    fieldWithPath("answerEn").type(JsonFieldType.STRING).description("영문 정답"),
                                    fieldWithPath("answerKo").type(JsonFieldType.STRING).description("한글 정답").optional(),
                                    fieldWithPath("efFactor").type(JsonFieldType.NUMBER).description("난이도 계수"),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("DELETE /api/user/cards/{id}")
    class DeleteUserCardTest {

        @Test
        @DisplayName("사용자 카드 삭제에 성공한다")
        void deleteUserCard_success() throws Exception {
            // 카드 생성
            MvcResult result = mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(delete("/api/user/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("user-cards/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));

            // 삭제 확인
            mockMvc.perform(get("/api/user/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }
}
