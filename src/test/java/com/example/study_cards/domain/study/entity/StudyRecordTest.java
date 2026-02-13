package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StudyRecordTest {

    private User testUser;
    private Card testCard;
    private UserCard testUserCard;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .code("CS")
                .name("CS")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testCard = Card.builder()
                .question("질문")
                .answer("답변")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(testCard, "id", 1L);

        testUserCard = UserCard.builder()
                .user(testUser)
                .question("개인 질문")
                .answer("개인 답변")
                .efFactor(2.5)
                .aiGenerated(false)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(testUserCard, "id", 1L);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("repetitionCount 기본값은 1이다")
        void builder_repetitionCount_기본값1() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getRepetitionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("interval이 null이면 기본값 1이 설정된다")
        void builder_interval_null이면_기본값1() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(null)
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getInterval()).isEqualTo(1);
        }

        @Test
        @DisplayName("interval을 지정하면 해당 값이 설정된다")
        void builder_interval_지정값사용() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(6))
                    .interval(6)
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getInterval()).isEqualTo(6);
        }

        @Test
        @DisplayName("studiedAt이 자동으로 현재 시간으로 설정된다")
        void builder_studiedAt_자동설정() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getStudiedAt()).isNotNull();
            assertThat(record.getStudiedAt().toLocalDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("isForPublicCard / isForUserCard")
    class CardTypeTest {

        @Test
        @DisplayName("공용 카드로 생성하면 isForPublicCard은 true이다")
        void isForPublicCard_공용카드_true() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.isForPublicCard()).isTrue();
            assertThat(record.isForUserCard()).isFalse();
        }

        @Test
        @DisplayName("개인 카드로 생성하면 isForUserCard은 true이다")
        void isForUserCard_개인카드_true() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .userCard(testUserCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.isForUserCard()).isTrue();
            assertThat(record.isForPublicCard()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateEfFactor")
    class UpdateEfFactorTest {

        @Test
        @DisplayName("정답이면 efFactor가 증가한다")
        void updateEfFactor_정답_증가() {
            // given
            double initialEfFactor = 2.5;
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(initialEfFactor)
                    .build();

            // when
            record.updateEfFactor(true);

            // then
            double expected = SM2Constants.calculateNewEfFactor(initialEfFactor, true);
            assertThat(record.getEfFactor()).isEqualTo(expected);
            assertThat(record.getEfFactor()).isGreaterThanOrEqualTo(initialEfFactor);
        }

        @Test
        @DisplayName("오답이면 efFactor가 감소한다")
        void updateEfFactor_오답_감소() {
            // given
            double initialEfFactor = 2.5;
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(initialEfFactor)
                    .build();

            // when
            record.updateEfFactor(false);

            // then
            double expected = SM2Constants.calculateNewEfFactor(initialEfFactor, false);
            assertThat(record.getEfFactor()).isEqualTo(expected);
            assertThat(record.getEfFactor()).isLessThan(initialEfFactor);
        }

        @Test
        @DisplayName("efFactor는 최솟값 아래로 내려가지 않는다")
        void updateEfFactor_최솟값_보장() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(SM2Constants.MIN_EF_FACTOR)
                    .build();

            // when
            record.updateEfFactor(false);

            // then
            assertThat(record.getEfFactor()).isGreaterThanOrEqualTo(SM2Constants.MIN_EF_FACTOR);
        }
    }

    @Nested
    @DisplayName("updateForReview")
    class UpdateForReviewTest {

        @Test
        @DisplayName("복습 시 모든 필드가 업데이트되고 repetitionCount가 증가한다")
        void updateForReview_전체필드_업데이트() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.5)
                    .build();
            int initialRepetitionCount = record.getRepetitionCount();

            LocalDate newNextReviewDate = LocalDate.now().plusDays(6);

            // when
            record.updateForReview(false, newNextReviewDate, 6);

            // then
            assertThat(record.getIsCorrect()).isFalse();
            assertThat(record.getNextReviewDate()).isEqualTo(newNextReviewDate);
            assertThat(record.getInterval()).isEqualTo(6);
            assertThat(record.getRepetitionCount()).isEqualTo(initialRepetitionCount + 1);
            assertThat(record.getStudiedAt().toLocalDate()).isEqualTo(LocalDate.now());
        }
    }
}
