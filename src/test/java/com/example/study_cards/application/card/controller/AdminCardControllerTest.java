package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.Role;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class AdminCardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String adminAccessToken;
    private String userAccessToken;
    private CardCreateRequest createRequest;
    private CardUpdateRequest updateRequest;

    private static final String QUESTION_EN = "What is JPA?";
    private static final String QUESTION_KO = "JPA란 무엇인가요?";
    private static final String ANSWER_EN = "Java Persistence API";
    private static final String ANSWER_KO = "자바 영속성 API";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Admin 사용자 생성
        SignUpRequest adminSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "admin@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "adminUser")
                .sample();
        authService.signUp(adminSignUpRequest);

        // Admin 역할 부여
        User adminUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        adminUser.addRole(Role.ROLE_ADMIN);
        userRepository.save(adminUser);

        SignInRequest adminSignInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "admin@example.com")
                .set("password", "password123")
                .sample();
        TokenResult adminTokenResult = authService.signIn(adminSignInRequest);
        adminAccessToken = adminTokenResult.accessToken();

        // 일반 사용자 생성
        SignUpRequest userSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "user@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "normalUser")
                .sample();
        authService.signUp(userSignUpRequest);

        SignInRequest userSignInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "user@example.com")
                .set("password", "password123")
                .sample();
        TokenResult userTokenResult = authService.signIn(userSignInRequest);
        userAccessToken = userTokenResult.accessToken();

        createRequest = fixtureMonkey.giveMeBuilder(CardCreateRequest.class)
                .set("questionEn", QUESTION_EN)
                .set("questionKo", QUESTION_KO)
                .set("answerEn", ANSWER_EN)
                .set("answerKo", ANSWER_KO)
                .set("category", Category.CS)
                .sample();

        updateRequest = fixtureMonkey.giveMeBuilder(CardUpdateRequest.class)
                .set("questionEn", "Updated question")
                .set("questionKo", QUESTION_KO)
                .set("answerEn", "Updated answer")
                .set("answerKo", ANSWER_KO)
                .set("category", Category.CS)
                .sample();
    }

    @Nested
    @DisplayName("POST /api/admin/cards")
    class CreateCardTest {

        @Test
        @DisplayName("Admin 사용자가 공용 카드 생성에 성공한다")
        void createCard_admin_success() throws Exception {
            mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.questionEn").value(QUESTION_EN))
                    .andExpect(jsonPath("$.cardType").value("PUBLIC"))
                    .andDo(document("admin-cards/create",
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
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 공용 카드 생성 시 403을 반환한다")
        void createCard_user_returns403() throws Exception {
            mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void createCard_unauthorized_returns401() throws Exception {
            mockMvc.perform(post("/api/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/cards/{id}")
    class UpdateCardTest {

        @Test
        @DisplayName("Admin 사용자가 공용 카드 수정에 성공한다")
        void updateCard_admin_success() throws Exception {
            // 카드 생성
            MvcResult result = mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(put("/api/admin/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionEn").value("Updated question"))
                    .andExpect(jsonPath("$.answerEn").value("Updated answer"))
                    .andDo(document("admin-cards/update",
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
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 공용 카드 수정 시 403을 반환한다")
        void updateCard_user_returns403() throws Exception {
            // Admin으로 카드 생성
            MvcResult result = mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(put("/api/admin/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/cards/{id}")
    class DeleteCardTest {

        @Test
        @DisplayName("Admin 사용자가 공용 카드 삭제에 성공한다")
        void deleteCard_admin_success() throws Exception {
            // 카드 생성
            MvcResult result = mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(delete("/api/admin/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("admin-cards/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));

            // 삭제 확인
            mockMvc.perform(get("/api/cards/{id}", cardId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("일반 사용자가 공용 카드 삭제 시 403을 반환한다")
        void deleteCard_user_returns403() throws Exception {
            // Admin으로 카드 생성
            MvcResult result = mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            Long cardId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(delete("/api/admin/cards/{id}", cardId)
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/cards")
    class GetCardsTest {

        @Test
        @DisplayName("Admin 사용자가 공용 카드 목록 조회에 성공한다")
        void getCards_admin_success() throws Exception {
            // 카드 생성
            mockMvc.perform(post("/api/admin/cards")
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)));

            mockMvc.perform(get("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andDo(document("admin-cards/list",
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
                                    fieldWithPath("[].cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC/CUSTOM)"),
                                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 Admin 카드 목록 조회 시 403을 반환한다")
        void getCards_user_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/cards")
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }
}
