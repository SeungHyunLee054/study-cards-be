package com.example.study_cards.application.usercard.service;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.domain.usercard.exception.UserCardErrorCode;
import com.example.study_cards.domain.usercard.exception.UserCardException;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCardServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserCardService userCardService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;

    private static final String QUESTION_EN = "What is JPA?";
    private static final String QUESTION_KO = "JPA란 무엇인가요?";
    private static final String ANSWER_EN = "Java Persistence API";
    private static final String ANSWER_KO = "자바 영속성 API";
    private static final Category CATEGORY = Category.CS;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testUser")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .password("password")
                .nickname("otherUser")
                .build());
    }

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTest {

        @Test
        @DisplayName("사용자 카드 생성에 성공한다")
        void createUserCard_success() {
            // given
            UserCardCreateRequest request = fixtureMonkey.giveMeBuilder(UserCardCreateRequest.class)
                    .set("questionEn", QUESTION_EN)
                    .set("questionKo", QUESTION_KO)
                    .set("answerEn", ANSWER_EN)
                    .set("answerKo", ANSWER_KO)
                    .set("category", CATEGORY)
                    .sample();

            // when
            UserCardResponse result = userCardService.createUserCard(testUser.getId(), request);

            // then
            assertThat(result.id()).isNotNull();
            assertThat(result.questionEn()).isEqualTo(QUESTION_EN);
            assertThat(result.answerEn()).isEqualTo(ANSWER_EN);
            assertThat(result.category()).isEqualTo(CATEGORY);
            assertThat(result.efFactor()).isEqualTo(2.5);
        }
    }

    @Nested
    @DisplayName("getUserCards")
    class GetUserCardsTest {

        @Test
        @DisplayName("사용자의 전체 카드 목록을 조회한다")
        void getUserCards_success() {
            // given
            createTestCard(testUser.getId(), "Q1", "A1", Category.CS);
            createTestCard(testUser.getId(), "Q2", "A2", Category.ENGLISH);

            // when
            List<UserCardResponse> result = userCardService.getUserCards(testUser.getId());

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("다른 사용자의 카드는 조회되지 않는다")
        void getUserCards_notIncludeOtherUserCards() {
            // given
            createTestCard(testUser.getId(), "Q1", "A1", Category.CS);
            createTestCard(otherUser.getId(), "Q2", "A2", Category.CS);

            // when
            List<UserCardResponse> result = userCardService.getUserCards(testUser.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).questionEn()).isEqualTo("Q1");
        }
    }

    @Nested
    @DisplayName("getUserCardsByCategory")
    class GetUserCardsByCategoryTest {

        @Test
        @DisplayName("카테고리별 카드 목록을 조회한다")
        void getUserCardsByCategory_success() {
            // given
            createTestCard(testUser.getId(), "Q1", "A1", Category.CS);
            createTestCard(testUser.getId(), "Q2", "A2", Category.ENGLISH);

            // when
            List<UserCardResponse> result = userCardService.getUserCardsByCategory(testUser.getId(), Category.CS);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo(Category.CS);
        }
    }

    @Nested
    @DisplayName("getUserCard")
    class GetUserCardTest {

        @Test
        @DisplayName("특정 카드를 조회한다")
        void getUserCard_success() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);

            // when
            UserCardResponse result = userCardService.getUserCard(testUser.getId(), created.id());

            // then
            assertThat(result.id()).isEqualTo(created.id());
            assertThat(result.questionEn()).isEqualTo(QUESTION_EN);
        }

        @Test
        @DisplayName("다른 사용자의 카드 조회시 예외가 발생한다")
        void getUserCard_otherUser_throwsException() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);

            // when & then
            assertThatThrownBy(() -> userCardService.getUserCard(otherUser.getId(), created.id()))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTest {

        @Test
        @DisplayName("카드 수정에 성공한다")
        void updateUserCard_success() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);
            UserCardUpdateRequest updateRequest = fixtureMonkey.giveMeBuilder(UserCardUpdateRequest.class)
                    .set("questionEn", "Updated question")
                    .set("questionKo", QUESTION_KO)
                    .set("answerEn", "Updated answer")
                    .set("answerKo", ANSWER_KO)
                    .set("category", CATEGORY)
                    .sample();

            // when
            UserCardResponse result = userCardService.updateUserCard(testUser.getId(), created.id(), updateRequest);

            // then
            assertThat(result.questionEn()).isEqualTo("Updated question");
            assertThat(result.answerEn()).isEqualTo("Updated answer");
        }

        @Test
        @DisplayName("다른 사용자의 카드 수정시 예외가 발생한다")
        void updateUserCard_otherUser_throwsException() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);
            UserCardUpdateRequest updateRequest = fixtureMonkey.giveMeBuilder(UserCardUpdateRequest.class)
                    .set("questionEn", "Updated question")
                    .set("questionKo", QUESTION_KO)
                    .set("answerEn", "Updated answer")
                    .set("answerKo", ANSWER_KO)
                    .set("category", CATEGORY)
                    .sample();

            // when & then
            assertThatThrownBy(() -> userCardService.updateUserCard(otherUser.getId(), created.id(), updateRequest))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTest {

        @Test
        @DisplayName("카드 삭제에 성공한다")
        void deleteUserCard_success() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);

            // when
            userCardService.deleteUserCard(testUser.getId(), created.id());

            // then
            List<UserCardResponse> remaining = userCardService.getUserCards(testUser.getId());
            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 카드 삭제시 예외가 발생한다")
        void deleteUserCard_otherUser_throwsException() {
            // given
            UserCardResponse created = createTestCard(testUser.getId(), QUESTION_EN, ANSWER_EN, CATEGORY);

            // when & then
            assertThatThrownBy(() -> userCardService.deleteUserCard(otherUser.getId(), created.id()))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }

    private UserCardResponse createTestCard(Long userId, String questionEn, String answerEn, Category category) {
        UserCardCreateRequest request = fixtureMonkey.giveMeBuilder(UserCardCreateRequest.class)
                .set("questionEn", questionEn)
                .set("questionKo", null)
                .set("answerEn", answerEn)
                .set("answerKo", null)
                .set("category", category)
                .sample();
        return userCardService.createUserCard(userId, request);
    }
}
