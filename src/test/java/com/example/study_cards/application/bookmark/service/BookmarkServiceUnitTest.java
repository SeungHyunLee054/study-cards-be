package com.example.study_cards.application.bookmark.service;

import com.example.study_cards.application.bookmark.dto.response.BookmarkResponse;
import com.example.study_cards.application.bookmark.dto.response.BookmarkStatusResponse;
import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.bookmark.service.BookmarkDomainService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class BookmarkServiceUnitTest extends BaseUnitTest {

    @Mock
    private BookmarkDomainService bookmarkDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private UserCardDomainService userCardDomainService;

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @InjectMocks
    private BookmarkService bookmarkService;

    private User testUser;
    private Card testCard;
    private UserCard testUserCard;
    private Category testCategory;
    private Bookmark cardBookmark;
    private Bookmark userCardBookmark;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;
    private static final Long USER_CARD_ID = 2L;

    @BeforeEach
    void setUp() {
        testCategory = createCategory();
        testUser = createTestUser();
        testCard = createTestCard();
        testUserCard = createTestUserCard();
        cardBookmark = createCardBookmark();
        userCardBookmark = createUserCardBookmark();
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

    private Bookmark createCardBookmark() {
        Bookmark bookmark = Bookmark.builder()
                .user(testUser)
                .card(testCard)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", 1L);
        return bookmark;
    }

    private Bookmark createUserCardBookmark() {
        Bookmark bookmark = Bookmark.builder()
                .user(testUser)
                .userCard(testUserCard)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", 2L);
        return bookmark;
    }

    @Nested
    @DisplayName("bookmarkCard")
    class BookmarkCardTest {

        @Test
        @DisplayName("공용 카드를 북마크하고 응답을 반환한다")
        void bookmarkCard_성공_응답반환() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(bookmarkDomainService.bookmarkCard(testUser, testCard)).willReturn(cardBookmark);

            // when
            BookmarkResponse result = bookmarkService.bookmarkCard(USER_ID, CARD_ID);

            // then
            assertThat(result.bookmarkId()).isEqualTo(1L);
            assertThat(result.cardId()).isEqualTo(CARD_ID);
            assertThat(result.question()).isEqualTo("자바란 무엇인가?");
        }
    }

    @Nested
    @DisplayName("unbookmarkCard")
    class UnbookmarkCardTest {

        @Test
        @DisplayName("공용 카드 북마크를 해제한다")
        void unbookmarkCard_성공() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);

            // when
            bookmarkService.unbookmarkCard(USER_ID, CARD_ID);

            // then
            verify(bookmarkDomainService).unbookmarkCard(testUser, testCard);
        }
    }

    @Nested
    @DisplayName("bookmarkUserCard")
    class BookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드를 북마크하고 응답을 반환한다")
        void bookmarkUserCard_성공_응답반환() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, testUser)).willReturn(testUserCard);
            given(bookmarkDomainService.bookmarkUserCard(testUser, testUserCard)).willReturn(userCardBookmark);

            // when
            BookmarkResponse result = bookmarkService.bookmarkUserCard(USER_ID, USER_CARD_ID);

            // then
            assertThat(result.bookmarkId()).isEqualTo(2L);
            assertThat(result.cardId()).isEqualTo(USER_CARD_ID);
            assertThat(result.question()).isEqualTo("나만의 질문");
        }
    }

    @Nested
    @DisplayName("unbookmarkUserCard")
    class UnbookmarkUserCardTest {

        @Test
        @DisplayName("개인 카드 북마크를 해제한다")
        void unbookmarkUserCard_성공() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, testUser)).willReturn(testUserCard);

            // when
            bookmarkService.unbookmarkUserCard(USER_ID, USER_CARD_ID);

            // then
            verify(bookmarkDomainService).unbookmarkUserCard(testUser, testUserCard);
        }
    }

    @Nested
    @DisplayName("getBookmarks")
    class GetBookmarksTest {

        @Test
        @DisplayName("북마크 목록을 조회한다")
        void getBookmarks_전체조회_성공() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Bookmark> bookmarkPage = new PageImpl<>(List.of(cardBookmark), pageable, 1);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(bookmarkDomainService.findBookmarks(testUser, null, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkResponse> result = bookmarkService.getBookmarks(USER_ID, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).bookmarkId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("카테고리 필터로 북마크 목록을 조회한다")
        void getBookmarks_카테고리필터_성공() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Bookmark> bookmarkPage = new PageImpl<>(List.of(cardBookmark), pageable, 1);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(categoryDomainService.findByCodeOrNull("CS")).willReturn(testCategory);
            given(bookmarkDomainService.findBookmarks(testUser, testCategory, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkResponse> result = bookmarkService.getBookmarks(USER_ID, "CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getCardBookmarkStatus")
    class GetCardBookmarkStatusTest {

        @Test
        @DisplayName("카드 북마크 상태를 반환한다")
        void getCardBookmarkStatus_북마크됨_true() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(bookmarkDomainService.isCardBookmarked(testUser, testCard)).willReturn(true);

            // when
            BookmarkStatusResponse result = bookmarkService.getCardBookmarkStatus(USER_ID, CARD_ID);

            // then
            assertThat(result.bookmarked()).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserCardBookmarkStatus")
    class GetUserCardBookmarkStatusTest {

        @Test
        @DisplayName("개인 카드 북마크 상태를 반환한다")
        void getUserCardBookmarkStatus_북마크됨_true() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.findByIdAndValidateOwner(USER_CARD_ID, testUser)).willReturn(testUserCard);
            given(bookmarkDomainService.isUserCardBookmarked(testUser, testUserCard)).willReturn(true);

            // when
            BookmarkStatusResponse result = bookmarkService.getUserCardBookmarkStatus(USER_ID, USER_CARD_ID);

            // then
            assertThat(result.bookmarked()).isTrue();
        }
    }
}
