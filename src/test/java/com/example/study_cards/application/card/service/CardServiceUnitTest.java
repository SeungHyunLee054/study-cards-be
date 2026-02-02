package com.example.study_cards.application.card.service;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.infra.redis.service.RateLimitService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CardServiceUnitTest extends BaseUnitTest {

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private CardService cardService;

    private Card testCard;

    private static final Long CARD_ID = 1L;

    @BeforeEach
    void setUp() {
        testCard = createTestCard();
    }

    private Card createTestCard() {
        Card card = Card.builder()
                .questionEn("What is Java?")
                .questionKo("자바란 무엇인가?")
                .answerEn("A programming language")
                .answerKo("프로그래밍 언어")
                .efFactor(2.5)
                .category(Category.CS)
                .build();
        setId(card, CARD_ID);
        return card;
    }

    private void setId(Card card, Long id) {
        try {
            var idField = Card.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(card, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getCards")
    class GetCardsTest {

        @Test
        @DisplayName("모든 카드를 조회한다")
        void getCards_returnsAllCards() {
            // given
            given(cardDomainService.findAll()).willReturn(List.of(testCard));

            // when
            List<CardResponse> result = cardService.getCards();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CARD_ID);
            assertThat(result.get(0).questionEn()).isEqualTo("What is Java?");
        }
    }

    @Nested
    @DisplayName("getCardsByCategory")
    class GetCardsByCategoryTest {

        @Test
        @DisplayName("카테고리별 카드를 조회한다")
        void getCardsByCategory_returnsCardsInCategory() {
            // given
            given(cardDomainService.findByCategory(Category.CS)).willReturn(List.of(testCard));

            // when
            List<CardResponse> result = cardService.getCardsByCategory(Category.CS);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo(Category.CS);
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
            assertThat(result.questionEn()).isEqualTo("What is Java?");
        }
    }

    @Nested
    @DisplayName("getCardsForStudy")
    class GetCardsForStudyTest {

        @Test
        @DisplayName("인증된 사용자는 제한 없이 카드를 조회한다")
        void getCardsForStudy_authenticated_returnsAllCards() {
            // given
            given(cardDomainService.findCardsForStudy()).willReturn(List.of(testCard));

            // when
            List<CardResponse> result = cardService.getCardsForStudy(null, true, "127.0.0.1");

            // then
            assertThat(result).hasSize(1);
            verify(rateLimitService, never()).getRemainingCards(anyString());
        }

        @Test
        @DisplayName("인증된 사용자는 카테고리별로 카드를 조회한다")
        void getCardsForStudy_authenticated_withCategory_returnsFilteredCards() {
            // given
            given(cardDomainService.findCardsForStudyByCategory(Category.CS)).willReturn(List.of(testCard));

            // when
            List<CardResponse> result = cardService.getCardsForStudy(Category.CS, true, "127.0.0.1");

            // then
            assertThat(result).hasSize(1);
            verify(cardDomainService).findCardsForStudyByCategory(Category.CS);
        }

        @Test
        @DisplayName("비인증 사용자는 일일 한도 내에서 카드를 조회한다")
        void getCardsForStudy_unauthenticated_respectsRateLimit() {
            // given
            String ipAddress = "192.168.1.1";
            given(cardDomainService.findCardsForStudy()).willReturn(List.of(testCard));
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(10);

            // when
            List<CardResponse> result = cardService.getCardsForStudy(null, false, ipAddress);

            // then
            assertThat(result).hasSize(1);
            verify(rateLimitService).getRemainingCards(ipAddress);
            verify(rateLimitService).incrementCardCount(ipAddress, 1);
        }

        @Test
        @DisplayName("비인증 사용자가 일일 한도를 초과하면 예외를 발생시킨다")
        void getCardsForStudy_unauthenticated_rateLimitExceeded_throwsException() {
            // given
            String ipAddress = "192.168.1.1";
            given(cardDomainService.findCardsForStudy()).willReturn(List.of(testCard));
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> cardService.getCardsForStudy(null, false, ipAddress))
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
                    .questionEn("Question 2")
                    .answerEn("Answer 2")
                    .efFactor(2.5)
                    .category(Category.CS)
                    .build();
            setId(card2, 2L);

            given(cardDomainService.findCardsForStudy()).willReturn(List.of(testCard, card2));
            given(rateLimitService.getRemainingCards(ipAddress)).willReturn(1);

            // when
            List<CardResponse> result = cardService.getCardsForStudy(null, false, ipAddress);

            // then
            assertThat(result).hasSize(1);
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
                    "What is Java?",
                    "자바란 무엇인가?",
                    "A programming language",
                    "프로그래밍 언어",
                    Category.CS
            );
            given(cardDomainService.createCard(
                    request.questionEn(),
                    request.questionKo(),
                    request.answerEn(),
                    request.answerKo(),
                    request.category()
            )).willReturn(testCard);

            // when
            CardResponse result = cardService.createCard(request);

            // then
            assertThat(result.questionEn()).isEqualTo("What is Java?");
            assertThat(result.category()).isEqualTo(Category.CS);
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
                    "Updated question",
                    "수정된 질문",
                    "Updated answer",
                    "수정된 답변",
                    Category.ENGLISH
            );

            Card updatedCard = Card.builder()
                    .questionEn("Updated question")
                    .questionKo("수정된 질문")
                    .answerEn("Updated answer")
                    .answerKo("수정된 답변")
                    .efFactor(2.5)
                    .category(Category.ENGLISH)
                    .build();
            setId(updatedCard, CARD_ID);

            given(cardDomainService.updateCard(
                    CARD_ID,
                    request.questionEn(),
                    request.questionKo(),
                    request.answerEn(),
                    request.answerKo(),
                    request.category()
            )).willReturn(updatedCard);

            // when
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // then
            assertThat(result.questionEn()).isEqualTo("Updated question");
            assertThat(result.category()).isEqualTo(Category.ENGLISH);
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
