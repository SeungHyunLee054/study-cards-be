package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
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
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    private String accessToken;
    private Category category;
    private Card card;

    @BeforeEach
    void setUp() {
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
        verifyUserEmail("test@example.com");

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

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/cards/{id}")
    class GetCardTest {

        @Test
        @DisplayName("단일 카드를 조회한다")
        void getCard_success() throws Exception {
            mockMvc.perform(get("/api/cards/{id}", card.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(card.getId()))
                    .andExpect(jsonPath("$.question").value(card.getQuestion()))
                    .andExpect(jsonPath("$.answer").value(card.getAnswer()))
                    .andDo(document("card/get-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
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
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC, CUSTOM)"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
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
        @DisplayName("학습용 카드 목록을 조회한다 (비인증)")
        void getCardsForStudy_unauthenticated_success() throws Exception {
            mockMvc.perform(get("/api/cards/study")
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("card/get-cards-for-study",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드").optional()
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("카드 목록"),
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
                                    fieldWithPath("content[].cardType").type(JsonFieldType.STRING).description("카드 타입"),
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
        @DisplayName("학습용 카드 목록을 조회한다 (인증)")
        void getCardsForStudy_authenticated_success() throws Exception {
            mockMvc.perform(get("/api/cards/study")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/all")
    class GetAllCardsWithUserCardsTest {

        @Test
        @DisplayName("모든 카드 (공개 + 사용자 카드)를 조회한다")
        void getAllCardsWithUserCards_success() throws Exception {
            mockMvc.perform(get("/api/cards/all")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("card/get-all-cards",
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

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getAllCardsWithUserCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/cards/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/study/all")
    class GetCardsForStudyWithUserCardsTest {

        @Test
        @DisplayName("학습용 카드 (공개 + 사용자 카드)를 조회한다")
        void getCardsForStudyWithUserCards_success() throws Exception {
            mockMvc.perform(get("/api/cards/study/all")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("card/get-cards-for-study-all",
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

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getCardsForStudyWithUserCards_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/cards/study/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/count")
    class GetCardCountTest {

        @Test
        @DisplayName("카드 개수를 조회한다")
        void getCardCount_success() throws Exception {
            mockMvc.perform(get("/api/cards/count")
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isNumber())
                    .andDo(document("card/get-card-count",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("category").description("카테고리 코드").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("전체 카드 개수를 조회한다")
        void getCardCount_all_success() throws Exception {
            mockMvc.perform(get("/api/cards/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isNumber());
        }
    }
}
