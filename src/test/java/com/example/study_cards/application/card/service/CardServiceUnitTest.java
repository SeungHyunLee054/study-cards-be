package com.example.study_cards.application.card.service;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import com.example.study_cards.infra.redis.service.RateLimitService;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CardServiceUnitTest extends BaseUnitTest {

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private UserCardDomainService userCardDomainService;

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private Category csCategory;
    private Category englishCategory;
    private User testUser;
    private UserCard testUserCard;

    private static final Long CARD_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long USER_CARD_ID = 10L;

    @BeforeEach
    void setUp() {
        csCategory = createCategory("CS", "CS", 1L);
        englishCategory = createCategory("ENGLISH", "영어", 2L);
        testCard = createTestCard();
        testUser = createTestUser();
        testUserCard = createTestUserCard();
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

    private UserCard createTestUserCard() {
        UserCard userCard = UserCard.builder()
                .user(testUser)
                .question("나만의 자바 질문")
                .answer("나만의 자바 답변")
                .efFactor(2.5)
                .aiGenerated(false)
                .category(csCategory)
                .build();
        ReflectionTestUtils.setField(userCard, "id", USER_CARD_ID);
        return userCard;
    }

    private Category createCategory(String code, String name, Long id) {
        Category category = Category.builder()
                .code(code)
                .name(name)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Card createTestCard() {
        Card card = Card.builder()
                .question("자바란 무엇인가?")
                .questionSub("What is Java?")
                .answer("프로그래밍 언어")
                .answerSub("A programming language")
                .efFactor(2.5)
                .category(csCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
    }

    @Nested
    @DisplayName("getCards")
    class GetCardsTest {

        @Test
        @DisplayName("모든 카드를 페이지네이션하여 조회한다")
        void getCards_returnsPagedCards() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(cardDomainService.findAll(pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.getCards(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(CARD_ID);
            assertThat(result.getContent().get(0).question()).isEqualTo("자바란 무엇인가?");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getCardsByCategory")
    class GetCardsByCategoryTest {

        @Test
        @DisplayName("카테고리별 카드를 페이지네이션하여 조회한다")
        void getCardsByCategory_returnsPagedCardsInCategory() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(categoryDomainService.findByCode("CS")).willReturn(csCategory);
            given(categoryDomainService.findSelfAndDescendants(csCategory)).willReturn(List.of(csCategory));
            given(cardDomainService.findByCategories(List.of(csCategory), pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.getCardsByCategory("CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category().code()).isEqualTo("CS");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getCard")
    class GetCardTest {

        @Test
        @DisplayName("ID로 카드를 조회한다")
        void getCard_returnsCard() {
            // given
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);

            // when
            CardResponse result = cardService.getCard(CARD_ID);

            // then
            assertThat(result.id()).isEqualTo(CARD_ID);
            assertThat(result.question()).isEqualTo("자바란 무엇인가?");
        }
    }

    @Nested
    @DisplayName("getCardsForStudy")
    class GetCardsForStudyTest {

        private Pageable pageable;

        @BeforeEach
        void setUpPageable() {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "efFactor"));
        }

        @Test
        @DisplayName("인증된 사용자는 제한 없이 카드를 조회한다")
        void getCardsForStudy_authenticated_returnsAllCards() {
            // given
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(cardDomainService.findCardsForStudy(pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.getCardsForStudy(null, true, "127.0.0.1", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(rateLimitService, never()).getRemainingCards(anyString());
        }

        @Test
        @DisplayName("인증된 사용자는 카테고리별로 카드를 조회한다")
        void getCardsForStudy_authenticated_withCategory_returnsFilteredCards() {
            // given
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(categoryDomainService.findByCodeOrNull("CS")).willReturn(csCategory);
            given(categoryDomainService.findSelfAndDescendants(csCategory)).willReturn(List.of(csCategory));
            given(cardDomainService.findCardsForStudyByCategories(List.of(csCategory), pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.getCardsForStudy("CS", true, "127.0.0.1", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(cardDomainService).findCardsForStudyByCategories(List.of(csCategory), pageable);
        }

        @Test
        @DisplayName("비인증 사용자는 일일 한도 내에서 카드를 조회한다")
        void getCardsForStudy_unauthenticated_respectsRateLimit() {
            // given
            String ipAddress = "192.168.1.1";
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(cardDomainService.findCardsForStudy(pageable)).willReturn(cardPage);
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(10);

            // when
            Page<CardResponse> result = cardService.getCardsForStudy(null, false, ipAddress, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(rateLimitService).getRemainingCards(ipAddress);
            verify(rateLimitService).incrementCardCount(ipAddress, 1);
        }

        @Test
        @DisplayName("비인증 사용자가 일일 한도를 초과하면 예외를 발생시킨다")
        void getCardsForStudy_unauthenticated_rateLimitExceeded_throwsException() {
            // given
            String ipAddress = "192.168.1.1";
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(cardDomainService.findCardsForStudy(pageable)).willReturn(cardPage);
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> cardService.getCardsForStudy(null, false, ipAddress, pageable))
                    .isInstanceOf(CardException.class)
                    .satisfies(exception -> {
                        CardException cardException = (CardException) exception;
                        assertThat(cardException.getErrorCode()).isEqualTo(CardErrorCode.RATE_LIMIT_EXCEEDED);
                    });
        }

        @Test
        @DisplayName("비인증 사용자는 남은 한도만큼만 카드를 조회한다")
        void getCardsForStudy_unauthenticated_limitsCards() {
            // given
            String ipAddress = "192.168.1.1";
            Card card2 = Card.builder()
                    .question("질문 2")
                    .answer("답변 2")
                    .efFactor(2.5)
                    .category(csCategory)
                    .build();
            ReflectionTestUtils.setField(card2, "id", 2L);

            Page<Card> cardPage = new PageImpl<>(List.of(testCard, card2), pageable, 2);
            given(cardDomainService.findCardsForStudy(pageable)).willReturn(cardPage);
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(1);

            // when
            Page<CardResponse> result = cardService.getCardsForStudy(null, false, ipAddress, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(rateLimitService).incrementCardCount(ipAddress, 1);
        }
    }

    @Nested
    @DisplayName("createCard")
    class CreateCardTest {

        @Test
        @DisplayName("카드를 생성한다")
        void createCard_createsAndReturnsCard() {
            // given
            CardCreateRequest request = new CardCreateRequest(
                    "자바란 무엇인가?",
                    "What is Java?",
                    "프로그래밍 언어",
                    "A programming language",
                    "CS"
            );
            given(categoryDomainService.findByCode("CS")).willReturn(csCategory);
            given(cardDomainService.createCard(
                    request.question(),
                    request.questionSub(),
                    request.answer(),
                    request.answerSub(),
                    csCategory,
                    false
            )).willReturn(testCard);

            // when
            CardResponse result = cardService.createCard(request);

            // then
            assertThat(result.question()).isEqualTo("자바란 무엇인가?");
            assertThat(result.category().code()).isEqualTo("CS");
        }
    }

    @Nested
    @DisplayName("updateCard")
    class UpdateCardTest {

        @Test
        @DisplayName("카드를 수정한다")
        void updateCard_updatesAndReturnsCard() {
            // given
            CardUpdateRequest request = new CardUpdateRequest(
                    "수정된 질문",
                    "Updated question",
                    "수정된 답변",
                    "Updated answer",
                    "ENGLISH"
            );

            Card updatedCard = Card.builder()
                    .question("수정된 질문")
                    .questionSub("Updated question")
                    .answer("수정된 답변")
                    .answerSub("Updated answer")
                    .efFactor(2.5)
                    .category(englishCategory)
                    .build();
            ReflectionTestUtils.setField(updatedCard, "id", CARD_ID);

            given(categoryDomainService.findByCode("ENGLISH")).willReturn(englishCategory);
            given(cardDomainService.updateCard(
                    CARD_ID,
                    request.question(),
                    request.questionSub(),
                    request.answer(),
                    request.answerSub(),
                    englishCategory
            )).willReturn(updatedCard);

            // when
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // then
            assertThat(result.question()).isEqualTo("수정된 질문");
            assertThat(result.category().code()).isEqualTo("ENGLISH");
        }
    }

    @Nested
    @DisplayName("deleteCard")
    class DeleteCardTest {

        @Test
        @DisplayName("카드를 삭제한다")
        void deleteCard_deletesCard() {
            // when
            cardService.deleteCard(CARD_ID);

            // then
            verify(cardDomainService).deleteCard(CARD_ID);
        }
    }

    @Nested
    @DisplayName("searchCards")
    class SearchCardsTest {

        private Pageable pageable;

        @BeforeEach
        void setUpPageable() {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("비인증 사용자는 공용 카드만 검색한다")
        void searchCards_비인증_공용카드만검색() {
            // given
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(cardDomainService.searchByKeyword("자바", null, pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.searchCards(null, "자바", null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).question()).isEqualTo("자바란 무엇인가?");
        }

        @Test
        @DisplayName("인증 사용자는 개인카드 우선으로 검색한다")
        void searchCards_인증_개인카드우선() {
            // given
            Page<UserCard> userCardPage = new PageImpl<>(List.of(testUserCard), PageRequest.of(0, 1), 1);
            Page<Card> publicCountPage = new PageImpl<>(List.of(testCard), PageRequest.of(0, 1), 1);
            Page<UserCard> userCardSearchPage = new PageImpl<>(List.of(testUserCard), PageRequest.of(0, 20), 1);
            Page<Card> publicSearchPage = new PageImpl<>(List.of(testCard), PageRequest.of(0, 19), 1);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(userCardDomainService.searchByKeyword(eq(testUser), eq("자바"), eq(null), any(Pageable.class)))
                    .willReturn(userCardPage)
                    .willReturn(userCardSearchPage);
            given(cardDomainService.searchByKeyword(eq("자바"), eq(null), any(Pageable.class)))
                    .willReturn(publicCountPage)
                    .willReturn(publicSearchPage);

            // when
            Page<CardResponse> result = cardService.searchCards(USER_ID, "자바", null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("카테고리 필터와 함께 검색한다")
        void searchCards_카테고리필터_성공() {
            // given
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            given(categoryDomainService.findByCodeOrNull("CS")).willReturn(csCategory);
            given(categoryDomainService.findSelfAndDescendants(csCategory)).willReturn(List.of(csCategory));
            given(cardDomainService.searchByKeyword("자바", List.of(csCategory), pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.searchCards(null, "자바", "CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category().code()).isEqualTo("CS");
        }

        @Test
        @DisplayName("검색어가 null이면 예외를 발생시킨다")
        void searchCards_null키워드_예외() {
            // when & then
            assertThatThrownBy(() -> cardService.searchCards(null, null, null, pageable))
                    .isInstanceOf(CardException.class)
                    .satisfies(exception -> {
                        CardException ex = (CardException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(CardErrorCode.INVALID_SEARCH_KEYWORD);
                    });
        }

        @Test
        @DisplayName("검색어가 2자 미만이면 예외를 발생시킨다")
        void searchCards_짧은키워드_예외() {
            // when & then
            assertThatThrownBy(() -> cardService.searchCards(null, "자", null, pageable))
                    .isInstanceOf(CardException.class)
                    .satisfies(exception -> {
                        CardException ex = (CardException) exception;
                        assertThat(ex.getErrorCode()).isEqualTo(CardErrorCode.INVALID_SEARCH_KEYWORD);
                    });
        }
    }
}
