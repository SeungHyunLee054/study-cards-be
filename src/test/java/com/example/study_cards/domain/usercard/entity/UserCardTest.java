package com.example.study_cards.domain.usercard.entity;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserCardTest {

    private User testUser;
    private User otherUser;
    private Category csCategory;
    private Category englishCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .nickname("다른유저")
                .build();
        ReflectionTestUtils.setField(otherUser, "id", 2L);

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
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("efFactor 미지정 시 기본값 2.5가 설정된다")
        void builder_efFactor_null이면_기본값2_5() {
            // when
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.getEfFactor()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("efFactor를 지정하면 해당 값이 설정된다")
        void builder_efFactor_지정값사용() {
            // when
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .efFactor(3.0)
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.getEfFactor()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("aiGenerated 미지정 시 기본값 false가 설정된다")
        void builder_aiGenerated_null이면_기본값false() {
            // when
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.getAiGenerated()).isFalse();
        }

        @Test
        @DisplayName("aiGenerated를 true로 지정하면 해당 값이 설정된다")
        void builder_aiGenerated_지정값사용() {
            // when
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .aiGenerated(true)
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.getAiGenerated()).isTrue();
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("카드 정보를 업데이트한다")
        void update_전체필드변경() {
            // given
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("원래 질문")
                    .questionSub("Original question")
                    .answer("원래 답변")
                    .answerSub("Original answer")
                    .category(csCategory)
                    .build();

            // when
            userCard.update("수정된 질문", "Updated question", "수정된 답변", "Updated answer", englishCategory);

            // then
            assertThat(userCard.getQuestion()).isEqualTo("수정된 질문");
            assertThat(userCard.getQuestionSub()).isEqualTo("Updated question");
            assertThat(userCard.getAnswer()).isEqualTo("수정된 답변");
            assertThat(userCard.getAnswerSub()).isEqualTo("Updated answer");
            assertThat(userCard.getCategory().getCode()).isEqualTo("ENGLISH");
        }
    }

    @Nested
    @DisplayName("isOwnedBy")
    class IsOwnedByTest {

        @Test
        @DisplayName("동일한 유저이면 true를 반환한다")
        void isOwnedBy_동일유저_true() {
            // given
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.isOwnedBy(testUser)).isTrue();
        }

        @Test
        @DisplayName("다른 유저이면 false를 반환한다")
        void isOwnedBy_다른유저_false() {
            // given
            UserCard userCard = UserCard.builder()
                    .user(testUser)
                    .question("질문")
                    .answer("답변")
                    .category(csCategory)
                    .build();

            // then
            assertThat(userCard.isOwnedBy(otherUser)).isFalse();
        }
    }
}
