package com.example.study_cards.domain.card.entity;

import com.example.study_cards.domain.category.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    private Card card;
    private Category csCategory;
    private Category englishCategory;

    @BeforeEach
    void setUp() {
        csCategory = Category.builder()
                .code("CS")
                .name("CS")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(csCategory, "id", 1L);

        englishCategory = Category.builder()
                .code("ENGLISH")
                .name("영어")
                .displayOrder(2)
                .build();
        ReflectionTestUtils.setField(englishCategory, "id", 2L);

        card = Card.builder()
                .question("자바란 무엇인가?")
                .questionSub("What is Java?")
                .answer("프로그래밍 언어")
                .answerSub("A programming language")
                .efFactor(2.5)
                .category(csCategory)
                .build();
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("카드 정보를 업데이트한다")
        void update_changesCardInfo() {
            // when
            card.update("New Question", "새 질문", "New Answer", "새 답변", englishCategory);

            // then
            assertThat(card.getQuestion()).isEqualTo("New Question");
            assertThat(card.getQuestionSub()).isEqualTo("새 질문");
            assertThat(card.getAnswer()).isEqualTo("New Answer");
            assertThat(card.getAnswerSub()).isEqualTo("새 답변");
            assertThat(card.getCategory().getCode()).isEqualTo("ENGLISH");
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
                    .question("Test")
                    .answer("Test")
                    .category(csCategory)
                    .build();

            // then
            assertThat(newCard.getEfFactor()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("efFactor를 지정하면 해당 값이 설정된다")
        void builder_withEfFactor_usesProvidedValue() {
            // when
            Card newCard = Card.builder()
                    .question("Test")
                    .answer("Test")
                    .efFactor(3.0)
                    .category(csCategory)
                    .build();

            // then
            assertThat(newCard.getEfFactor()).isEqualTo(3.0);
        }
    }
}
