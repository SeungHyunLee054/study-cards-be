package com.example.study_cards.domain.generation.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedCardTest {

    private GeneratedCard generatedCard;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .code("CS")
                .name("Computer Science")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", 1L);

        generatedCard = GeneratedCard.builder()
                .model("gpt-4")
                .sourceWord("Java")
                .prompt("Generate a flashcard about Java")
                .question("자바란 무엇인가?")
                .questionSub("What is Java?")
                .answer("프로그래밍 언어")
                .answerSub("A programming language")
                .category(category)
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("생성 시 상태가 PENDING으로 설정된다")
        void builder_statusIsPending() {
            // then
            assertThat(generatedCard.getStatus()).isEqualTo(GenerationStatus.PENDING);
        }

        @Test
        @DisplayName("모든 필드가 정상적으로 설정된다")
        void builder_allFields_setCorrectly() {
            // then
            assertThat(generatedCard.getModel()).isEqualTo("gpt-4");
            assertThat(generatedCard.getSourceWord()).isEqualTo("Java");
            assertThat(generatedCard.getQuestion()).isEqualTo("자바란 무엇인가?");
            assertThat(generatedCard.getQuestionSub()).isEqualTo("What is Java?");
            assertThat(generatedCard.getAnswer()).isEqualTo("프로그래밍 언어");
            assertThat(generatedCard.getAnswerSub()).isEqualTo("A programming language");
            assertThat(generatedCard.getCategory()).isEqualTo(category);
        }
    }

    @Nested
    @DisplayName("approve")
    class ApproveTest {

        @Test
        @DisplayName("승인하면 상태가 APPROVED로 변경되고 승인 시간이 설정된다")
        void approve_changesStatusAndSetsApprovedAt() {
            // when
            generatedCard.approve();

            // then
            assertThat(generatedCard.getStatus()).isEqualTo(GenerationStatus.APPROVED);
            assertThat(generatedCard.getApprovedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTest {

        @Test
        @DisplayName("거부하면 상태가 REJECTED로 변경된다")
        void reject_changesStatusToRejected() {
            // when
            generatedCard.reject();

            // then
            assertThat(generatedCard.getStatus()).isEqualTo(GenerationStatus.REJECTED);
        }

        @Test
        @DisplayName("승인 후 거부하면 승인 시간이 초기화된다")
        void reject_afterApprove_clearsApprovedAt() {
            // given
            generatedCard.approve();

            // when
            generatedCard.reject();

            // then
            assertThat(generatedCard.getStatus()).isEqualTo(GenerationStatus.REJECTED);
            assertThat(generatedCard.getApprovedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markAsMigrated")
    class MarkAsMigratedTest {

        @Test
        @DisplayName("마이그레이션 처리하면 상태가 MIGRATED로 변경된다")
        void markAsMigrated_changesStatusToMigrated() {
            // when
            generatedCard.markAsMigrated();

            // then
            assertThat(generatedCard.getStatus()).isEqualTo(GenerationStatus.MIGRATED);
        }
    }

    @Nested
    @DisplayName("isPending")
    class IsPendingTest {

        @Test
        @DisplayName("PENDING 상태이면 true를 반환한다")
        void isPending_pendingStatus_returnsTrue() {
            // then
            assertThat(generatedCard.isPending()).isTrue();
        }

        @Test
        @DisplayName("APPROVED 상태이면 false를 반환한다")
        void isPending_approvedStatus_returnsFalse() {
            // given
            generatedCard.approve();

            // then
            assertThat(generatedCard.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("isApproved")
    class IsApprovedTest {

        @Test
        @DisplayName("APPROVED 상태이면 true를 반환한다")
        void isApproved_approvedStatus_returnsTrue() {
            // given
            generatedCard.approve();

            // then
            assertThat(generatedCard.isApproved()).isTrue();
        }

        @Test
        @DisplayName("PENDING 상태이면 false를 반환한다")
        void isApproved_pendingStatus_returnsFalse() {
            // then
            assertThat(generatedCard.isApproved()).isFalse();
        }
    }

    @Nested
    @DisplayName("toCard")
    class ToCardTest {

        @Test
        @DisplayName("GeneratedCard를 Card로 변환한다")
        void toCard_convertsToCard() {
            // when
            Card card = generatedCard.toCard();

            // then
            assertThat(card.getQuestion()).isEqualTo("자바란 무엇인가?");
            assertThat(card.getQuestionSub()).isEqualTo("What is Java?");
            assertThat(card.getAnswer()).isEqualTo("프로그래밍 언어");
            assertThat(card.getAnswerSub()).isEqualTo("A programming language");
            assertThat(card.getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("Card로 변환 시 efFactor 기본값이 설정된다")
        void toCard_setsDefaultEfFactor() {
            // when
            Card card = generatedCard.toCard();

            // then
            assertThat(card.getEfFactor()).isEqualTo(2.5);
        }
    }
}
