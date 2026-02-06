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
import com.example.study_cards.domain.user.service.UserDomainService;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final Long CARD_ID = 1L;

    @BeforeEach
    void setUp() {
        csCategory = createCategory("CS", "CS", 1L);
        englishCategory = createCategory("ENGLISH", "영어", 2L);
        testCard = createTestCard();
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
            given(cardDomainService.findByCategory(csCategory, pageable)).willReturn(cardPage);

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
            given(cardDomainService.findCardsForStudyByCategory(csCategory, pageable)).willReturn(cardPage);

            // when
            Page<CardResponse> result = cardService.getCardsForStudy("CS", true, "127.0.0.1", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(cardDomainService).findCardsForStudyByCategory(csCategory, pageable);
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
                    csCategory
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
}
