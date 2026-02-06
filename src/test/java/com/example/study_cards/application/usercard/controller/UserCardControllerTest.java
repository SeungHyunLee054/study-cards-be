package com.example.study_cards.application.usercard.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
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
class UserCardControllerTest extends BaseIntegrationTest {

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
    private UserCardRepository userCardRepository;

    private String accessToken;
    private User user;
    private Category category;
    private UserCard userCard;

    @BeforeEach
    void setUp() {
        userCardRepository.deleteAll();
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

        userCard = userCardRepository.save(UserCard.builder()
                .question("What is an algorithm?")
                .questionSub("알고리즘이란 무엇인가요?")
                .answer("A step-by-step procedure for solving a problem")
                .answerSub("문제를 해결하기 위한 단계별 절차")
                .category(category)
                .user(user)
                .build());
    }

    @Nested
    @DisplayName("GET /api/user/cards")
    class GetUserCardsTest {

        @Test
        @DisplayName("사용자 카드 목록을 조회한다")
        void getUserCards_success() throws Exception {
            mockMvc.perform(get("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(userCard.getId()))
                    .andDo(document("user-card/get-user-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드").optional()
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("사용자 카드 목록"),
                                    fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("content[].question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("content[].questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("content[].answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("content[].answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("content[].efFactor").type(JsonFieldType.NUMBER).description("EF 팩터"),
                                    fieldWithPath("content[].category").type(JsonFieldType.OBJECT).description("카테고리 정보"),
                                    fieldWithPath("content[].category.id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("content[].category.code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("content[].category.name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("content[].category.parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("content[].category.parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
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
        @DisplayName("카테고리로 필터링하여 조회한다")
        void getUserCards_withCategory_success() throws Exception {
            mockMvc.perform(get("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getUserCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/user/cards"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/user/cards/{id}")
    class GetUserCardTest {

        @Test
        @DisplayName("단일 사용자 카드를 조회한다")
        void getUserCard_success() throws Exception {
            mockMvc.perform(get("/api/user/cards/{id}", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userCard.getId()))
                    .andExpect(jsonPath("$.question").value(userCard.getQuestion()))
                    .andDo(document("user-card/get-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
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
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 카드 조회 시 404를 반환한다")
        void getUserCard_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/user/cards/{id}", 99999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/user/cards/study")
    class GetUserCardsForStudyTest {

        @Test
        @DisplayName("학습용 사용자 카드를 조회한다")
        void getUserCardsForStudy_success() throws Exception {
            mockMvc.perform(get("/api/user/cards/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("user-card/get-user-cards-for-study",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드").optional()
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("POST /api/user/cards")
    class CreateUserCardTest {

        @Test
        @DisplayName("사용자 카드를 생성한다")
        void createUserCard_success() throws Exception {
            UserCardCreateRequest request = new UserCardCreateRequest(
                    "What is OOP?",
                    "OOP란 무엇인가요?",
                    "Object-Oriented Programming",
                    "객체 지향 프로그래밍",
                    "CS"
            );

            mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.question").value(request.question()))
                    .andExpect(jsonPath("$.answer").value(request.answer()))
                    .andDo(document("user-card/create-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리 코드")
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
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }

        @Test
        @DisplayName("질문이 비어있으면 400을 반환한다")
        void createUserCard_blankQuestion_returns400() throws Exception {
            UserCardCreateRequest request = new UserCardCreateRequest(
                    "", null, "Answer", null, "CS"
            );

            mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("답변이 비어있으면 400을 반환한다")
        void createUserCard_blankAnswer_returns400() throws Exception {
            UserCardCreateRequest request = new UserCardCreateRequest(
                    "Question", null, "", null, "CS"
            );

            mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("카테고리가 없으면 400을 반환한다")
        void createUserCard_nullCategory_returns400() throws Exception {
            String request = """
                    {
                        "question": "Question",
                        "answer": "Answer"
                    }
                    """;

            mockMvc.perform(post("/api/user/cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/user/cards/{id}")
    class UpdateUserCardTest {

        @Test
        @DisplayName("사용자 카드를 수정한다")
        void updateUserCard_success() throws Exception {
            UserCardUpdateRequest request = new UserCardUpdateRequest(
                    "Updated Question",
                    "Updated Question Sub",
                    "Updated Answer",
                    "Updated Answer Sub",
                    "CS"
            );

            mockMvc.perform(put("/api/user/cards/{id}", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.question").value(request.question()))
                    .andExpect(jsonPath("$.answer").value(request.answer()))
                    .andDo(document("user-card/update-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
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
        @DisplayName("존재하지 않는 카드 수정 시 404를 반환한다")
        void updateUserCard_notFound_returns404() throws Exception {
            UserCardUpdateRequest request = new UserCardUpdateRequest(
                    "Updated Question", null, "Updated Answer", null, "CS"
            );

            mockMvc.perform(put("/api/user/cards/{id}", 99999L)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/user/cards/{id}")
    class DeleteUserCardTest {

        @Test
        @DisplayName("사용자 카드를 삭제한다")
        void deleteUserCard_success() throws Exception {
            mockMvc.perform(delete("/api/user/cards/{id}", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("user-card/delete-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카드 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 카드 삭제 시 404를 반환한다")
        void deleteUserCard_notFound_returns404() throws Exception {
            mockMvc.perform(delete("/api/user/cards/{id}", 99999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void deleteUserCard_unauthorized_returns401() throws Exception {
            mockMvc.perform(delete("/api/user/cards/{id}", userCard.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
