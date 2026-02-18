package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
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
class AdminCardControllerTest extends BaseIntegrationTest {

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

    private String adminAccessToken;
    private String userAccessToken;
    private Category category;
    private Card card;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        categoryRepository.deleteAll();
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
        userRepository.save(admin);

        SignInRequest adminSignInRequest = new SignInRequest("admin@example.com", "password123");
        TokenResult adminTokenResult = authService.signIn(adminSignInRequest);
        adminAccessToken = adminTokenResult.accessToken();

        SignUpRequest userSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "user@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "normalUser")
                .sample();

        authService.signUp(userSignUpRequest);
        verifyUserEmail("user@example.com");

        SignInRequest userSignInRequest = new SignInRequest("user@example.com", "password123");
        TokenResult userTokenResult = authService.signIn(userSignInRequest);
        userAccessToken = userTokenResult.accessToken();

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

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/admin/cards")
    class GetCardsTest {

        @Test
        @DisplayName("관리자가 카드 목록을 조회한다")
        void getCards_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("admin-card/get-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드").optional(),
                                    parameterWithName("keyword").description("질문/답변 검색어 (2자 이상)").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("관리자가 검색어로 카드 목록을 조회한다")
        void getCards_admin_withKeyword_success() throws Exception {
            mockMvc.perform(get("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .param("keyword", "variable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(card.getId()));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void getCards_user_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/cards")
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/admin/cards"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/cards/{id}")
    class GetCardTest {

        @Test
        @DisplayName("관리자가 단일 카드를 조회한다")
        void getCard_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(card.getId()))
                    .andDo(document("admin-card/get-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("efFactor").type(JsonFieldType.NUMBER).description("EF 팩터"),
                                    fieldWithPath("category").type(JsonFieldType.OBJECT).description("카테고리 정보"),
                                    fieldWithPath("category.id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("category.code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("category.name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("category.parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("category.parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void getCard_user_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/cards")
    class CreateCardTest {

        @Test
        @DisplayName("관리자가 카드를 생성한다")
        void createCard_admin_success() throws Exception {
            CardCreateRequest request = new CardCreateRequest(
                    "What is OOP?",
                    "OOP란 무엇인가요?",
                    "Object-Oriented Programming",
                    "객체 지향 프로그래밍",
                    "CS"
            );

            mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.question").value(request.question()))
                    .andDo(document("admin-card/create-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            requestFields(
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리 코드")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void createCard_user_returns403() throws Exception {
            CardCreateRequest request = new CardCreateRequest(
                    "Question", null, "Answer", null, "CS"
            );

            mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("질문이 비어있으면 400을 반환한다")
        void createCard_blankQuestion_returns400() throws Exception {
            CardCreateRequest request = new CardCreateRequest(
                    "", null, "Answer", null, "CS"
            );

            mockMvc.perform(post("/api/admin/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/cards/{id}")
    class UpdateCardTest {

        @Test
        @DisplayName("관리자가 카드를 수정한다")
        void updateCard_admin_success() throws Exception {
            CardUpdateRequest request = new CardUpdateRequest(
                    "Updated Question",
                    "Updated Question Sub",
                    "Updated Answer",
                    "Updated Answer Sub",
                    "CS"
            );

            mockMvc.perform(put("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.question").value(request.question()))
                    .andDo(document("admin-card/update-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카드 ID")
                            ),
                            requestFields(
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리 코드")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void updateCard_user_returns403() throws Exception {
            CardUpdateRequest request = new CardUpdateRequest(
                    "Updated Question", null, "Updated Answer", null, "CS"
            );

            mockMvc.perform(put("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/cards/{id}")
    class DeleteCardTest {

        @Test
        @DisplayName("관리자가 카드를 삭제한다")
        void deleteCard_admin_success() throws Exception {
            mockMvc.perform(delete("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("admin-card/delete-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카드 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void deleteCard_user_returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/cards/{id}", card.getId())
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }
}
