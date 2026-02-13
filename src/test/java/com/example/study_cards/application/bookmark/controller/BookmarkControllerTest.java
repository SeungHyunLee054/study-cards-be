package com.example.study_cards.application.bookmark.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.bookmark.repository.BookmarkRepository;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
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
class BookmarkControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    private String accessToken;
    private Category category;
    private Card card;
    private UserCard userCard;

    @BeforeEach
    void setUp() {
        bookmarkRepository.deleteAll();
        userCardRepository.deleteAll();
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

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        userCard = userCardRepository.save(UserCard.builder()
                .user(user)
                .question("개인 질문")
                .questionSub("Personal question")
                .answer("개인 답변")
                .answerSub("Personal answer")
                .category(category)
                .build());
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("POST /api/bookmarks/cards/{cardId}")
    class BookmarkCardTest {

        @Test
        @DisplayName("공용 카드를 북마크한다")
        void bookmarkCard_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarkId").isNumber())
                    .andExpect(jsonPath("$.cardId").value(card.getId()))
                    .andExpect(jsonPath("$.cardType").value("PUBLIC"))
                    .andExpect(jsonPath("$.question").value(card.getQuestion()))
                    .andDo(document("bookmark/bookmark-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("cardId").description("카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("bookmarkId").type(JsonFieldType.NUMBER).description("북마크 ID"),
                                    fieldWithPath("cardId").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("cardType").type(JsonFieldType.STRING).description("카드 타입 (PUBLIC, CUSTOM)"),
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("category").type(JsonFieldType.OBJECT).description("카테고리 정보"),
                                    fieldWithPath("category.id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("category.code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("category.name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("category.parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("category.parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("bookmarkedAt").type(JsonFieldType.STRING).description("북마크 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void bookmarkCard_unauthorized_returns401() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/bookmarks/cards/{cardId}")
    class UnbookmarkCardTest {

        @Test
        @DisplayName("공용 카드 북마크를 해제한다")
        void unbookmarkCard_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(delete("/api/bookmarks/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("bookmark/unbookmark-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("cardId").description("카드 ID")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("POST /api/bookmarks/user-cards/{userCardId}")
    class BookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드를 북마크한다")
        void bookmarkUserCard_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/user-cards/{userCardId}", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarkId").isNumber())
                    .andExpect(jsonPath("$.cardId").value(userCard.getId()))
                    .andExpect(jsonPath("$.cardType").value("CUSTOM"))
                    .andDo(document("bookmark/bookmark-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("userCardId").description("개인 카드 ID")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("DELETE /api/bookmarks/user-cards/{userCardId}")
    class UnbookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드 북마크를 해제한다")
        void unbookmarkUserCard_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/user-cards/{userCardId}", userCard.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(delete("/api/bookmarks/user-cards/{userCardId}", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("bookmark/unbookmark-user-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("userCardId").description("개인 카드 ID")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/bookmarks")
    class GetBookmarksTest {

        @Test
        @DisplayName("북마크 목록을 조회한다")
        void getBookmarks_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(get("/api/bookmarks")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andDo(document("bookmark/get-bookmarks",
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
        @DisplayName("카테고리로 필터링하여 조회한다")
        void getBookmarks_withCategory_success() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(get("/api/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("category", "CS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getBookmarks_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/bookmarks"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/bookmarks/cards/{cardId}/status")
    class GetCardBookmarkStatusTest {

        @Test
        @DisplayName("북마크된 카드의 상태를 조회한다")
        void getCardBookmarkStatus_bookmarked() throws Exception {
            mockMvc.perform(post("/api/bookmarks/cards/{cardId}", card.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(get("/api/bookmarks/cards/{cardId}/status", card.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true))
                    .andDo(document("bookmark/get-card-bookmark-status",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("cardId").description("카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("bookmarked").type(JsonFieldType.BOOLEAN).description("북마크 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("북마크하지 않은 카드의 상태를 조회한다")
        void getCardBookmarkStatus_notBookmarked() throws Exception {
            mockMvc.perform(get("/api/bookmarks/cards/{cardId}/status", card.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/bookmarks/user-cards/{userCardId}/status")
    class GetUserCardBookmarkStatusTest {

        @Test
        @DisplayName("북마크된 개인 카드의 상태를 조회한다")
        void getUserCardBookmarkStatus_bookmarked() throws Exception {
            mockMvc.perform(post("/api/bookmarks/user-cards/{userCardId}", userCard.getId())
                    .header("Authorization", "Bearer " + accessToken));

            mockMvc.perform(get("/api/bookmarks/user-cards/{userCardId}/status", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true))
                    .andDo(document("bookmark/get-user-card-bookmark-status",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("userCardId").description("개인 카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("bookmarked").type(JsonFieldType.BOOLEAN).description("북마크 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("북마크하지 않은 개인 카드의 상태를 조회한다")
        void getUserCardBookmarkStatus_notBookmarked() throws Exception {
            mockMvc.perform(get("/api/bookmarks/user-cards/{userCardId}/status", userCard.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(false));
        }
    }
}
