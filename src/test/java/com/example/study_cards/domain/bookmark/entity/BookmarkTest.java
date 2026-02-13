package com.example.study_cards.domain.bookmark.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkTest {

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
    @DisplayName("isForPublicCard")
    class IsForPublicCardTest {

        @Test
        @DisplayName("공용 카드 북마크이면 true를 반환한다")
        void isForPublicCard_공용카드_true() {
            // given
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .card(testCard)
                    .build();

            // then
            assertThat(bookmark.isForPublicCard()).isTrue();
            assertThat(bookmark.isForUserCard()).isFalse();
        }

        @Test
        @DisplayName("개인 카드 북마크이면 false를 반환한다")
        void isForPublicCard_개인카드_false() {
            // given
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .userCard(testUserCard)
                    .build();

            // then
            assertThat(bookmark.isForPublicCard()).isFalse();
        }
    }

    @Nested
    @DisplayName("isForUserCard")
    class IsForUserCardTest {

        @Test
        @DisplayName("개인 카드 북마크이면 true를 반환한다")
        void isForUserCard_개인카드_true() {
            // given
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .userCard(testUserCard)
                    .build();

            // then
            assertThat(bookmark.isForUserCard()).isTrue();
            assertThat(bookmark.isForPublicCard()).isFalse();
        }

        @Test
        @DisplayName("공용 카드 북마크이면 false를 반환한다")
        void isForUserCard_공용카드_false() {
            // given
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .card(testCard)
                    .build();

            // then
            assertThat(bookmark.isForUserCard()).isFalse();
        }
    }
}
