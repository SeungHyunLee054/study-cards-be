package com.example.study_cards.application.usercard.service;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.domain.card.entity.Category;
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

    @InjectMocks
    private UserCardService userCardService;

    private User testUser;
    private UserCard testUserCard;
    private UserCardCreateRequest createRequest;
    private UserCardUpdateRequest updateRequest;

    private static final Long USER_ID = 1L;
    private static final Long USER_CARD_ID = 1L;
    private static final String QUESTION_EN = "What is JPA?";
    private static final String QUESTION_KO = "JPA란 무엇인가요?";
    private static final String ANSWER_EN = "Java Persistence API";
    private static final String ANSWER_KO = "자바 영속성 API";
    private static final Category CATEGORY = Category.CS;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testUser")
                .build();
        ReflectionTestUtils.setField(testUser, "id", USER_ID);

        testUserCard = UserCard.builder()
                .user(testUser)
                .questionEn(QUESTION_EN)
                .questionKo(QUESTION_KO)
                .answerEn(ANSWER_EN)
                .answerKo(ANSWER_KO)
                .category(CATEGORY)
                .build();
        ReflectionTestUtils.setField(testUserCard, "id", USER_CARD_ID);

        createRequest = fixtureMonkey.giveMeBuilder(UserCardCreateRequest.class)
                .set("questionEn", QUESTION_EN)
                .set("questionKo", QUESTION_KO)
                .set("answerEn", ANSWER_EN)
                .set("answerKo", ANSWER_KO)
                .set("category", CATEGORY)
                .sample();

        updateRequest = fixtureMonkey.giveMeBuilder(UserCardUpdateRequest.class)
                .set("questionEn", "Updated question")
                .set("questionKo", QUESTION_KO)
                .set("answerEn", "Updated answer")
                .set("answerKo", ANSWER_KO)
                .set("category", CATEGORY)
                .sample();
    }

    @Nested
    @DisplayName("getUserCards")
    class GetUserCardsTest {

        @Test
        @DisplayName("사용자의 전체 카드 목록을 조회한다")
        void getUserCards_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByUser(testUser)).willReturn(List.of(testUserCard));

            // when
            List<UserCardResponse> result = userCardService.getUserCards(USER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(USER_CARD_ID);
            assertThat(result.get(0).questionEn()).isEqualTo(QUESTION_EN);
        }
    }

    @Nested
    @DisplayName("getUserCardsByCategory")
    class GetUserCardsByCategoryTest {

        @Test
        @DisplayName("사용자의 카테고리별 카드 목록을 조회한다")
        void getUserCardsByCategory_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByUserAndCategory(testUser, CATEGORY)).willReturn(List.of(testUserCard));

            // when
            List<UserCardResponse> result = userCardService.getUserCardsByCategory(USER_ID, CATEGORY);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo(CATEGORY);
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
            assertThat(result.questionEn()).isEqualTo(QUESTION_EN);
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
            given(userCardDomainService.createUserCard(
                    eq(testUser), eq(QUESTION_EN), eq(QUESTION_KO), eq(ANSWER_EN), eq(ANSWER_KO), eq(CATEGORY)))
                    .willReturn(testUserCard);

            // when
            UserCardResponse result = userCardService.createUserCard(USER_ID, createRequest);

            // then
            assertThat(result.id()).isEqualTo(USER_CARD_ID);
            assertThat(result.questionEn()).isEqualTo(QUESTION_EN);
            verify(userCardDomainService).createUserCard(
                    eq(testUser), eq(QUESTION_EN), eq(QUESTION_KO), eq(ANSWER_EN), eq(ANSWER_KO), eq(CATEGORY));
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
                    .questionEn("Updated question")
                    .questionKo(QUESTION_KO)
                    .answerEn("Updated answer")
                    .answerKo(ANSWER_KO)
                    .category(CATEGORY)
                    .build();
            ReflectionTestUtils.setField(updatedCard, "id", USER_CARD_ID);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.updateUserCard(
                    eq(USER_CARD_ID), eq(testUser), any(), any(), any(), any(), any()))
                    .willReturn(updatedCard);

            // when
            UserCardResponse result = userCardService.updateUserCard(USER_ID, USER_CARD_ID, updateRequest);

            // then
            assertThat(result.questionEn()).isEqualTo("Updated question");
            assertThat(result.answerEn()).isEqualTo("Updated answer");
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
