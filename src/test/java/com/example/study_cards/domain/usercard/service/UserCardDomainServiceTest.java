package com.example.study_cards.domain.usercard.service;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.exception.UserCardErrorCode;
import com.example.study_cards.domain.usercard.exception.UserCardException;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserCardDomainServiceTest extends BaseUnitTest {

    @Mock
    private UserCardRepository userCardRepository;

    @InjectMocks
    private UserCardDomainService userCardDomainService;

    private User testUser;
    private User otherUser;
    private UserCard testUserCard;
    private Category testCategory;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
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

        otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .nickname("otherUser")
                .build();
        ReflectionTestUtils.setField(otherUser, "id", OTHER_USER_ID);

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
    }

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTest {

        @Test
        @DisplayName("사용자 카드 생성에 성공한다")
        void createUserCard_success() {
            // given
            given(userCardRepository.save(any(UserCard.class))).willReturn(testUserCard);

            // when
            UserCard result = userCardDomainService.createUserCard(
                    testUser, QUESTION, QUESTION_SUB, ANSWER, ANSWER_SUB, testCategory);

            // then
            assertThat(result.getQuestion()).isEqualTo(QUESTION);
            assertThat(result.getAnswer()).isEqualTo(ANSWER);
            assertThat(result.getCategory().getCode()).isEqualTo("CS");
            verify(userCardRepository).save(any(UserCard.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("ID로 사용자 카드를 조회한다")
        void findById_success() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when
            UserCard result = userCardDomainService.findById(USER_CARD_ID);

            // then
            assertThat(result).isEqualTo(testUserCard);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        void findById_notFound_throwsException() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userCardDomainService.findById(USER_CARD_ID))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findByIdAndValidateOwner")
    class FindByIdAndValidateOwnerTest {

        @Test
        @DisplayName("소유자 검증에 성공한다")
        void findByIdAndValidateOwner_success() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when
            UserCard result = userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, testUser);

            // then
            assertThat(result).isEqualTo(testUserCard);
        }

        @Test
        @DisplayName("소유자가 아닌 경우 예외가 발생한다")
        void findByIdAndValidateOwner_notOwner_throwsException() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when & then
            assertThatThrownBy(() -> userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, otherUser))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }

    @Nested
    @DisplayName("findByUser")
    class FindByUserTest {

        @Test
        @DisplayName("사용자의 카드 목록을 조회한다")
        void findByUser_success() {
            // given
            List<UserCard> cards = List.of(testUserCard);
            given(userCardRepository.findByUser(testUser)).willReturn(cards);

            // when
            List<UserCard> result = userCardDomainService.findByUser(testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testUserCard);
        }
    }

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTest {

        @Test
        @DisplayName("사용자 카드 수정에 성공한다")
        void updateUserCard_success() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));
            String newQuestion = "수정된 질문";
            String newAnswer = "수정된 답변";

            // when
            UserCard result = userCardDomainService.updateUserCard(
                    USER_CARD_ID, testUser, newQuestion, QUESTION_SUB, newAnswer, ANSWER_SUB, testCategory);

            // then
            assertThat(result.getQuestion()).isEqualTo(newQuestion);
            assertThat(result.getAnswer()).isEqualTo(newAnswer);
        }

        @Test
        @DisplayName("소유자가 아닌 경우 수정에 실패한다")
        void updateUserCard_notOwner_throwsException() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when & then
            assertThatThrownBy(() -> userCardDomainService.updateUserCard(
                    USER_CARD_ID, otherUser, QUESTION, QUESTION_SUB, ANSWER, ANSWER_SUB, testCategory))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTest {

        @Test
        @DisplayName("사용자 카드 삭제에 성공한다")
        void deleteUserCard_success() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when
            userCardDomainService.deleteUserCard(USER_CARD_ID, testUser);

            // then
            verify(userCardRepository).delete(testUserCard);
        }

        @Test
        @DisplayName("소유자가 아닌 경우 삭제에 실패한다")
        void deleteUserCard_notOwner_throwsException() {
            // given
            given(userCardRepository.findById(USER_CARD_ID)).willReturn(Optional.of(testUserCard));

            // when & then
            assertThatThrownBy(() -> userCardDomainService.deleteUserCard(USER_CARD_ID, otherUser))
                    .isInstanceOf(UserCardException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
    }
}
