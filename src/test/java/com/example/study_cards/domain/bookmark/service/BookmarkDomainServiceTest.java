package com.example.study_cards.domain.bookmark.service;

import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.bookmark.exception.BookmarkErrorCode;
import com.example.study_cards.domain.bookmark.exception.BookmarkException;
import com.example.study_cards.domain.bookmark.repository.BookmarkRepository;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class BookmarkDomainServiceTest extends BaseUnitTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkDomainService bookmarkDomainService;

    private User testUser;
    private Card testCard;
    private UserCard testUserCard;
    private Category testCategory;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;
    private static final Long USER_CARD_ID = 1L;
    private static final Long BOOKMARK_ID = 1L;

    @BeforeEach
    void setUp() {
        testCategory = createCategory();
        testUser = createTestUser();
        testCard = createTestCard();
        testUserCard = createTestUserCard();
    }

    private Category createCategory() {
        Category category = Category.builder()
                .code("CS")
                .name("CS")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", 1L);
        return category;
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Card createTestCard() {
        Card card = Card.builder()
                .question("자바란 무엇인가?")
                .answer("프로그래밍 언어")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
    }

    private UserCard createTestUserCard() {
        UserCard userCard = UserCard.builder()
                .user(testUser)
                .question("나만의 질문")
                .answer("나만의 답변")
                .efFactor(2.5)
                .aiGenerated(false)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(userCard, "id", USER_CARD_ID);
        return userCard;
    }

    private Bookmark createBookmark(Card card, UserCard userCard) {
        Bookmark bookmark = Bookmark.builder()
                .user(testUser)
                .card(card)
                .userCard(userCard)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", BOOKMARK_ID);
        return bookmark;
    }

    @Nested
    @DisplayName("bookmarkCard")
    class BookmarkCardTest {

        @Test
        @DisplayName("공용 카드를 북마크한다")
        void bookmarkCard_성공() {
            // given
            given(bookmarkRepository.existsByUserAndCard(testUser, testCard)).willReturn(false);
            given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> {
                Bookmark saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", BOOKMARK_ID);
                return saved;
            });

            // when
            Bookmark result = bookmarkDomainService.bookmarkCard(testUser, testCard);

            // then
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getCard()).isEqualTo(testCard);
            assertThat(result.isForPublicCard()).isTrue();
        }

        @Test
        @DisplayName("이미 북마크한 카드를 다시 북마크하면 예외가 발생한다")
        void bookmarkCard_이미북마크_예외() {
            // given
            given(bookmarkRepository.existsByUserAndCard(testUser, testCard)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> bookmarkDomainService.bookmarkCard(testUser, testCard))
                    .isInstanceOf(BookmarkException.class)
                    .satisfies(exception -> {
                        BookmarkException ex = (BookmarkException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(BookmarkErrorCode.ALREADY_BOOKMARKED);
                    });
        }
    }

    @Nested
    @DisplayName("bookmarkUserCard")
    class BookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드를 북마크한다")
        void bookmarkUserCard_성공() {
            // given
            given(bookmarkRepository.existsByUserAndUserCard(testUser, testUserCard)).willReturn(false);
            given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> {
                Bookmark saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", BOOKMARK_ID);
                return saved;
            });

            // when
            Bookmark result = bookmarkDomainService.bookmarkUserCard(testUser, testUserCard);

            // then
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getUserCard()).isEqualTo(testUserCard);
            assertThat(result.isForUserCard()).isTrue();
        }

        @Test
        @DisplayName("이미 북마크한 개인 카드를 다시 북마크하면 예외가 발생한다")
        void bookmarkUserCard_이미북마크_예외() {
            // given
            given(bookmarkRepository.existsByUserAndUserCard(testUser, testUserCard)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> bookmarkDomainService.bookmarkUserCard(testUser, testUserCard))
                    .isInstanceOf(BookmarkException.class)
                    .satisfies(exception -> {
                        BookmarkException ex = (BookmarkException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(BookmarkErrorCode.ALREADY_BOOKMARKED);
                    });
        }
    }

    @Nested
    @DisplayName("unbookmarkCard")
    class UnbookmarkCardTest {

        @Test
        @DisplayName("공용 카드 북마크를 해제한다")
        void unbookmarkCard_성공() {
            // given
            Bookmark bookmark = createBookmark(testCard, null);
            given(bookmarkRepository.findByUserAndCard(testUser, testCard)).willReturn(Optional.of(bookmark));

            // when
            bookmarkDomainService.unbookmarkCard(testUser, testCard);

            // then
            verify(bookmarkRepository).delete(bookmark);
        }

        @Test
        @DisplayName("북마크하지 않은 카드를 해제하면 예외가 발생한다")
        void unbookmarkCard_북마크없음_예외() {
            // given
            given(bookmarkRepository.findByUserAndCard(testUser, testCard)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookmarkDomainService.unbookmarkCard(testUser, testCard))
                    .isInstanceOf(BookmarkException.class)
                    .satisfies(exception -> {
                        BookmarkException ex = (BookmarkException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(BookmarkErrorCode.BOOKMARK_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("unbookmarkUserCard")
    class UnbookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드 북마크를 해제한다")
        void unbookmarkUserCard_성공() {
            // given
            Bookmark bookmark = createBookmark(null, testUserCard);
            given(bookmarkRepository.findByUserAndUserCard(testUser, testUserCard)).willReturn(Optional.of(bookmark));

            // when
            bookmarkDomainService.unbookmarkUserCard(testUser, testUserCard);

            // then
            verify(bookmarkRepository).delete(bookmark);
        }

        @Test
        @DisplayName("북마크하지 않은 개인 카드를 해제하면 예외가 발생한다")
        void unbookmarkUserCard_북마크없음_예외() {
            // given
            given(bookmarkRepository.findByUserAndUserCard(testUser, testUserCard)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookmarkDomainService.unbookmarkUserCard(testUser, testUserCard))
                    .isInstanceOf(BookmarkException.class)
                    .satisfies(exception -> {
                        BookmarkException ex = (BookmarkException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(BookmarkErrorCode.BOOKMARK_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("isCardBookmarked")
    class IsCardBookmarkedTest {

        @Test
        @DisplayName("북마크된 카드는 true를 반환한다")
        void isCardBookmarked_북마크됨_true() {
            // given
            given(bookmarkRepository.existsByUserAndCard(testUser, testCard)).willReturn(true);

            // when
            boolean result = bookmarkDomainService.isCardBookmarked(testUser, testCard);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("북마크되지 않은 카드는 false를 반환한다")
        void isCardBookmarked_북마크안됨_false() {
            // given
            given(bookmarkRepository.existsByUserAndCard(testUser, testCard)).willReturn(false);

            // when
            boolean result = bookmarkDomainService.isCardBookmarked(testUser, testCard);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findBookmarks")
    class FindBookmarksTest {

        @Test
        @DisplayName("사용자의 북마크 목록을 조회한다")
        void findBookmarks_성공() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Bookmark bookmark = createBookmark(testCard, null);
            Page<Bookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);
            given(bookmarkRepository.findByUser(testUser, null, pageable)).willReturn(bookmarkPage);

            // when
            Page<Bookmark> result = bookmarkDomainService.findBookmarks(testUser, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("카테고리 필터로 북마크 목록을 조회한다")
        void findBookmarks_카테고리필터_성공() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Bookmark bookmark = createBookmark(testCard, null);
            Page<Bookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);
            given(bookmarkRepository.findByUser(testUser, testCategory, pageable)).willReturn(bookmarkPage);

            // when
            Page<Bookmark> result = bookmarkDomainService.findBookmarks(testUser, testCategory, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

}
