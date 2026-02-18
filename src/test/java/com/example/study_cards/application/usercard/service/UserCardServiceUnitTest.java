package com.example.study_cards.application.usercard.service;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserCardServiceUnitTest extends BaseUnitTest {

    @Mock
    private UserCardDomainService userCardDomainService;

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @InjectMocks
    private UserCardService userCardService;

    private User testUser;
    private UserCard testUserCard;
    private UserCardCreateRequest createRequest;
    private UserCardUpdateRequest updateRequest;
    private Category testCategory;

    private static final Long USER_ID = 1L;
    private static final Long USER_CARD_ID = 1L;
    private static final String QUESTION = "JPA란 무엇인가요?";
    private static final String QUESTION_SUB = "What is JPA?";
    private static final String ANSWER = "자바 영속성 API";
    private static final String ANSWER_SUB = "Java Persistence API";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testUser")
                .build();
        ReflectionTestUtils.setField(testUser, "id", USER_ID);

        testCategory = Category.builder()
                .code("CS")
                .name("CS")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testUserCard = UserCard.builder()
                .user(testUser)
                .question(QUESTION)
                .questionSub(QUESTION_SUB)
                .answer(ANSWER)
                .answerSub(ANSWER_SUB)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(testUserCard, "id", USER_CARD_ID);

        createRequest = fixtureMonkey.giveMeBuilder(UserCardCreateRequest.class)
                .set("question", QUESTION)
                .set("questionSub", QUESTION_SUB)
                .set("answer", ANSWER)
                .set("answerSub", ANSWER_SUB)
                .set("category", "CS")
                .sample();

        updateRequest = fixtureMonkey.giveMeBuilder(UserCardUpdateRequest.class)
                .set("question", "수정된 질문")
                .set("questionSub", QUESTION_SUB)
                .set("answer", "수정된 답변")
                .set("answerSub", ANSWER_SUB)
                .set("category", "CS")
                .sample();
    }

    @Nested
    @DisplayName("getUserCards")
    class GetUserCardsTest {

        @Test
        @DisplayName("사용자의 전체 카드 목록을 페이지네이션하여 조회한다")
        void getUserCards_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserCard> userCardPage = new PageImpl<>(List.of(testUserCard), pageable, 1);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByUser(testUser, pageable)).willReturn(userCardPage);

            // when
            Page<UserCardResponse> result = userCardService.getUserCards(USER_ID, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(USER_CARD_ID);
            assertThat(result.getContent().get(0).question()).isEqualTo(QUESTION);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getUserCardsByCategory")
    class GetUserCardsByCategoryTest {

        @Test
        @DisplayName("사용자의 카테고리별 카드 목록을 페이지네이션하여 조회한다")
        void getUserCardsByCategory_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserCard> userCardPage = new PageImpl<>(List.of(testUserCard), pageable, 1);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(categoryDomainService.findSelfAndDescendants(testCategory)).willReturn(List.of(testCategory));
            given(userCardDomainService.findByUserAndCategories(testUser, List.of(testCategory), pageable)).willReturn(userCardPage);

            // when
            Page<UserCardResponse> result = userCardService.getUserCardsByCategory(USER_ID, "CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category().code()).isEqualTo("CS");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getUserCard")
    class GetUserCardTest {

        @Test
        @DisplayName("사용자의 특정 카드를 조회한다")
        void getUserCard_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, testUser)).willReturn(testUserCard);

            // when
            UserCardResponse result = userCardService.getUserCard(USER_ID, USER_CARD_ID);

            // then
            assertThat(result.id()).isEqualTo(USER_CARD_ID);
            assertThat(result.question()).isEqualTo(QUESTION);
        }
    }

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTest {

        @Test
        @DisplayName("사용자 카드 생성에 성공한다")
        void createUserCard_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(userCardDomainService.createUserCard(
                    eq(testUser), eq(QUESTION), eq(QUESTION_SUB), eq(ANSWER), eq(ANSWER_SUB), eq(testCategory)))
                    .willReturn(testUserCard);

            // when
            UserCardResponse result = userCardService.createUserCard(USER_ID, createRequest);

            // then
            assertThat(result.id()).isEqualTo(USER_CARD_ID);
            assertThat(result.question()).isEqualTo(QUESTION);
            verify(userCardDomainService).createUserCard(
                    eq(testUser), eq(QUESTION), eq(QUESTION_SUB), eq(ANSWER), eq(ANSWER_SUB), eq(testCategory));
            verify(categoryDomainService).validateLeafCategory(testCategory);
        }
    }

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTest {

        @Test
        @DisplayName("사용자 카드 수정에 성공한다")
        void updateUserCard_success() {
            // given
            UserCard updatedCard = UserCard.builder()
                    .user(testUser)
                    .question("수정된 질문")
                    .questionSub(QUESTION_SUB)
                    .answer("수정된 답변")
                    .answerSub(ANSWER_SUB)
                    .category(testCategory)
                    .build();
            ReflectionTestUtils.setField(updatedCard, "id", USER_CARD_ID);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(userCardDomainService.updateUserCard(
                    eq(USER_CARD_ID), eq(testUser), any(), any(), any(), any(), any()))
                    .willReturn(updatedCard);

            // when
            UserCardResponse result = userCardService.updateUserCard(USER_ID, USER_CARD_ID, updateRequest);

            // then
            assertThat(result.question()).isEqualTo("수정된 질문");
            assertThat(result.answer()).isEqualTo("수정된 답변");
            verify(categoryDomainService).validateLeafCategory(testCategory);
        }
    }

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTest {

        @Test
        @DisplayName("사용자 카드 삭제에 성공한다")
        void deleteUserCard_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            userCardService.deleteUserCard(USER_ID, USER_CARD_ID);

            // then
            verify(userCardDomainService).deleteUserCard(USER_CARD_ID, testUser);
        }
    }
}
