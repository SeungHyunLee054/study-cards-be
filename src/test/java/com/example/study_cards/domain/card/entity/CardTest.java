package com.example.study_cards.domain.card.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    private Card card;

    @BeforeEach
    void setUp() {
        card = Card.builder()
                .questionEn("What is Java?")
                .questionKo("자바란 무엇인가?")
                .answerEn("A programming language")
                .answerKo("프로그래밍 언어")
                .efFactor(2.5)
                .category(Category.CS)
                .build();
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("카드 정보를 업데이트한다")
        void update_changesCardInfo() {
            // when
            card.update("New Question", "새 질문", "New Answer", "새 답변", Category.ENGLISH);

            // then
            assertThat(card.getQuestionEn()).isEqualTo("New Question");
            assertThat(card.getQuestionKo()).isEqualTo("새 질문");
            assertThat(card.getAnswerEn()).isEqualTo("New Answer");
            assertThat(card.getAnswerKo()).isEqualTo("새 답변");
            assertThat(card.getCategory()).isEqualTo(Category.ENGLISH);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("efFactor 미지정 시 기본값 2.5가 설정된다")
        void builder_withoutEfFactor_defaultsTo2_5() {
            // when
            Card newCard = Card.builder()
                    .questionEn("Test")
                    .answerEn("Test")
                    .category(Category.CS)
                    .build();

            // then
            assertThat(newCard.getEfFactor()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("efFactor를 지정하면 해당 값이 설정된다")
        void builder_withEfFactor_usesProvidedValue() {
            // when
            Card newCard = Card.builder()
                    .questionEn("Test")
                    .answerEn("Test")
                    .efFactor(3.0)
                    .category(Category.CS)
                    .build();

            // then
            assertThat(newCard.getEfFactor()).isEqualTo(3.0);
        }
    }
}
