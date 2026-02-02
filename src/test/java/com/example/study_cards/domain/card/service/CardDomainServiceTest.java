package com.example.study_cards.domain.card.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class CardDomainServiceTest extends BaseUnitTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardDomainService cardDomainService;

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
    @DisplayName("createCard")
    class CreateCardTest {

        @Test
        @DisplayName("카드를 생성하고 저장한다")
        void createCard_savesAndReturnsCard() {
            // given
            given(cardRepository.save(any(Card.class))).willReturn(testCard);

            // when
            Card result = cardDomainService.createCard(
                    "What is Java?",
                    "자바란 무엇인가?",
                    "A programming language",
                    "프로그래밍 언어",
                    Category.CS
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getQuestionEn()).isEqualTo("What is Java?");
            verify(cardRepository).save(any(Card.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("존재하는 ID로 카드를 조회한다")
        void findById_returnsCard() {
            // given
            given(cardRepository.findById(CARD_ID)).willReturn(Optional.of(testCard));

            // when
            Card result = cardDomainService.findById(CARD_ID);

            // then
            assertThat(result.getId()).isEqualTo(CARD_ID);
            assertThat(result.getQuestionEn()).isEqualTo("What is Java?");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
        void findById_withNonExistentId_throwsException() {
            // given
            given(cardRepository.findById(CARD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cardDomainService.findById(CARD_ID))
                    .isInstanceOf(CardException.class)
                    .satisfies(exception -> {
                        CardException cardException = (CardException) exception;
                        assertThat(cardException.getErrorCode()).isEqualTo(CardErrorCode.CARD_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("모든 카드를 조회한다")
        void findAll_returnsAllCards() {
            // given
            given(cardRepository.findAll()).willReturn(List.of(testCard));

            // when
            List<Card> result = cardDomainService.findAll();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCard);
        }
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategoryTest {

        @Test
        @DisplayName("카테고리별 카드를 조회한다")
        void findByCategory_returnsCardsInCategory() {
            // given
            given(cardRepository.findByCategory(Category.CS)).willReturn(List.of(testCard));

            // when
            List<Card> result = cardDomainService.findByCategory(Category.CS);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(Category.CS);
        }
    }

    @Nested
    @DisplayName("findCardsForStudy")
    class FindCardsForStudyTest {

        @Test
        @DisplayName("efFactor 오름차순으로 카드를 조회한다")
        void findCardsForStudy_returnsSortedByEfFactor() {
            // given
            given(cardRepository.findAllByOrderByEfFactorAsc()).willReturn(List.of(testCard));

            // when
            List<Card> result = cardDomainService.findCardsForStudy();

            // then
            assertThat(result).hasSize(1);
            verify(cardRepository).findAllByOrderByEfFactorAsc();
        }
    }

    @Nested
    @DisplayName("findCardsForStudyByCategory")
    class FindCardsForStudyByCategoryTest {

        @Test
        @DisplayName("카테고리별로 efFactor 오름차순으로 카드를 조회한다")
        void findCardsForStudyByCategory_returnsSortedByEfFactor() {
            // given
            given(cardRepository.findByCategoryOrderByEfFactorAsc(Category.CS)).willReturn(List.of(testCard));

            // when
            List<Card> result = cardDomainService.findCardsForStudyByCategory(Category.CS);

            // then
            assertThat(result).hasSize(1);
            verify(cardRepository).findByCategoryOrderByEfFactorAsc(Category.CS);
        }
    }

    @Nested
    @DisplayName("updateCard")
    class UpdateCardTest {

        @Test
        @DisplayName("카드 정보를 업데이트한다")
        void updateCard_updatesCardInfo() {
            // given
            given(cardRepository.findById(CARD_ID)).willReturn(Optional.of(testCard));

            // when
            Card result = cardDomainService.updateCard(
                    CARD_ID,
                    "Updated Question",
                    "업데이트된 질문",
                    "Updated Answer",
                    "업데이트된 답변",
                    Category.ENGLISH
            );

            // then
            assertThat(result.getQuestionEn()).isEqualTo("Updated Question");
            assertThat(result.getQuestionKo()).isEqualTo("업데이트된 질문");
            assertThat(result.getCategory()).isEqualTo(Category.ENGLISH);
        }
    }

    @Nested
    @DisplayName("deleteCard")
    class DeleteCardTest {

        @Test
        @DisplayName("카드를 삭제한다")
        void deleteCard_deletesCard() {
            // given
            given(cardRepository.findById(CARD_ID)).willReturn(Optional.of(testCard));

            // when
            cardDomainService.deleteCard(CARD_ID);

            // then
            verify(cardRepository).delete(testCard);
        }

        @Test
        @DisplayName("존재하지 않는 카드 삭제 시 예외를 발생시킨다")
        void deleteCard_withNonExistentId_throwsException() {
            // given
            given(cardRepository.findById(CARD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cardDomainService.deleteCard(CARD_ID))
                    .isInstanceOf(CardException.class);
        }
    }
}
