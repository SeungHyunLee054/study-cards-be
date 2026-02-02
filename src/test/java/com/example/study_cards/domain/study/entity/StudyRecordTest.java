package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StudyRecordTest {

    private User testUser;
    private Card testCard;
    private StudySession testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        testCard = Card.builder()
                .questionEn("What is Java?")
                .questionKo("자바란 무엇인가?")
                .answerEn("A programming language")
                .answerKo("프로그래밍 언어")
                .efFactor(2.5)
                .category(Category.CS)
                .build();

        testSession = StudySession.builder()
                .user(testUser)
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("학습 기록을 생성한다")
        void builder_createsRecord() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .session(testSession)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getUser()).isEqualTo(testUser);
            assertThat(record.getCard()).isEqualTo(testCard);
            assertThat(record.getSession()).isEqualTo(testSession);
            assertThat(record.getIsCorrect()).isTrue();
            assertThat(record.getStudiedAt()).isNotNull();
        }

        @Test
        @DisplayName("repetitionCount 기본값은 1이다")
        void builder_repetitionCountDefaultsToOne() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getRepetitionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("interval 미지정 시 기본값은 1이다")
        void builder_intervalDefaultsToOne() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getInterval()).isEqualTo(1);
        }

        @Test
        @DisplayName("interval을 지정하면 해당 값이 설정된다")
        void builder_withInterval_usesProvidedValue() {
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
        @DisplayName("session 없이도 생성할 수 있다")
        void builder_withoutSession_createsRecord() {
            // when
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // then
            assertThat(record.getSession()).isNull();
            assertThat(record.getUser()).isEqualTo(testUser);
        }
    }

    @Nested
    @DisplayName("updateForReview")
    class UpdateForReviewTest {

        @Test
        @DisplayName("복습 후 정보를 업데이트한다")
        void updateForReview_updatesRecord() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            LocalDate newNextReviewDate = LocalDate.now().plusDays(6);
            int newInterval = 6;

            // when
            record.updateForReview(true, newNextReviewDate, newInterval);

            // then
            assertThat(record.getIsCorrect()).isTrue();
            assertThat(record.getNextReviewDate()).isEqualTo(newNextReviewDate);
            assertThat(record.getInterval()).isEqualTo(newInterval);
        }

        @Test
        @DisplayName("복습 후 repetitionCount가 증가한다")
        void updateForReview_incrementsRepetitionCount() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            int initialCount = record.getRepetitionCount();

            // when
            record.updateForReview(true, LocalDate.now().plusDays(6), 6);

            // then
            assertThat(record.getRepetitionCount()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("오답 처리 시 isCorrect가 false로 설정된다")
        void updateForReview_incorrect_setsIsCorrectFalse() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // when
            record.updateForReview(false, LocalDate.now().plusDays(1), 1);

            // then
            assertThat(record.getIsCorrect()).isFalse();
        }

        @Test
        @DisplayName("studiedAt이 현재 시간으로 업데이트된다")
        void updateForReview_updatesStudiedAt() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // when
            record.updateForReview(true, LocalDate.now().plusDays(6), 6);

            // then
            assertThat(record.getStudiedAt()).isNotNull();
        }

        @Test
        @DisplayName("여러 번 복습하면 repetitionCount가 누적된다")
        void updateForReview_multipleReviews_accumulatesCount() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // when
            record.updateForReview(true, LocalDate.now().plusDays(1), 1);
            record.updateForReview(true, LocalDate.now().plusDays(6), 6);
            record.updateForReview(true, LocalDate.now().plusDays(15), 15);

            // then
            assertThat(record.getRepetitionCount()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("updateEfFactor")
    class UpdateEfFactorTest {

        @Test
        @DisplayName("정답 시 efFactor가 유지된다 (quality=4, delta=0)")
        void updateEfFactor_correct_maintainsEfFactor() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            double initialEfFactor = record.getEfFactor();

            // when
            record.updateEfFactor(true);

            // then
            assertThat(record.getEfFactor()).isEqualTo(initialEfFactor);
        }

        @Test
        @DisplayName("오답 시 efFactor가 감소한다 (quality=2)")
        void updateEfFactor_incorrect_decreasesEfFactor() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            double initialEfFactor = record.getEfFactor();

            // when
            record.updateEfFactor(false);

            // then
            assertThat(record.getEfFactor()).isLessThan(initialEfFactor);
            assertThat(record.getEfFactor()).isCloseTo(2.18, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("efFactor는 1.3 미만으로 내려가지 않는다")
        void updateEfFactor_minimumIs1_3() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(1.4)
                    .build();

            // when
            record.updateEfFactor(false);

            // then
            assertThat(record.getEfFactor()).isGreaterThanOrEqualTo(1.3);
        }

        @Test
        @DisplayName("연속 오답 시 efFactor가 계속 감소하다 1.3에서 멈춘다")
        void updateEfFactor_multipleIncorrect_stopsAt1_3() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            // when
            for (int i = 0; i < 10; i++) {
                record.updateEfFactor(false);
            }

            // then
            assertThat(record.getEfFactor()).isEqualTo(1.3);
        }
    }
}
