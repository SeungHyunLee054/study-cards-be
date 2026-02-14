package com.example.study_cards.domain.card.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class CardDomainServiceTest extends BaseUnitTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @InjectMocks
    private CardDomainService cardDomainService;

    private Card testCard;
    private Category testCategory;
    private Category englishCategory;

    private static final Long CARD_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @BeforeEach
    void setUp() {
        testCategory = createTestCategory("CS", "CS", null, 1);
        englishCategory = createTestCategory("ENGLISH", "영어", null, 2);
        ReflectionTestUtils.setField(englishCategory, "id", 2L);
        testCard = createTestCard();
    }

    private Category createTestCategory(String code, String name, Category parent, int displayOrder) {
        Category category = Category.builder()
                .code(code)
                .name(name)
                .parent(parent)
                .displayOrder(displayOrder)
                .build();
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }

    private Card createTestCard() {
        Card card = Card.builder()
                .question("자바란 무엇인가?")
                .questionSub("What is Java?")
                .answer("프로그래밍 언어")
                .answerSub("A programming language")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
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
                    "자바란 무엇인가?",
                    "What is Java?",
                    "프로그래밍 언어",
                    "A programming language",
                    testCategory,
                    false
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getQuestion()).isEqualTo("자바란 무엇인가?");
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
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.of(testCard));

            // when
            Card result = cardDomainService.findById(CARD_ID);

            // then
            assertThat(result.getId()).isEqualTo(CARD_ID);
            assertThat(result.getQuestion()).isEqualTo("자바란 무엇인가?");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
        void findById_withNonExistentId_throwsException() {
            // given
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.empty());

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
            given(cardRepository.findByStatus(CardStatus.ACTIVE)).willReturn(List.of(testCard));

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
            given(cardRepository.findByCategoryAndStatus(testCategory, CardStatus.ACTIVE)).willReturn(List.of(testCard));

            // when
            List<Card> result = cardDomainService.findByCategory(testCategory);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory().getCode()).isEqualTo("CS");
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
    @DisplayName("updateCard")
    class UpdateCardTest {

        @Test
        @DisplayName("카드 정보를 업데이트한다")
        void updateCard_updatesCardInfo() {
            // given
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.of(testCard));

            // when
            Card result = cardDomainService.updateCard(
                    CARD_ID,
                    "업데이트된 질문",
                    "Updated Question",
                    "업데이트된 답변",
                    "Updated Answer",
                    englishCategory
            );

            // then
            assertThat(result.getQuestion()).isEqualTo("업데이트된 질문");
            assertThat(result.getQuestionSub()).isEqualTo("Updated Question");
            assertThat(result.getCategory().getCode()).isEqualTo("ENGLISH");
        }
    }

    @Nested
    @DisplayName("deleteCard")
    class DeleteCardTest {

        @Test
        @DisplayName("카드를 삭제한다")
        void deleteCard_deletesCard() {
            // given
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.of(testCard));
            given(studyRecordRepository.existsByCard(testCard)).willReturn(false);

            // when
            cardDomainService.deleteCard(CARD_ID);

            // then
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.DELETED);
            assertThat(testCard.getDeletedAt()).isNotNull();
            verify(cardRepository, never()).delete(any(Card.class));
        }

        @Test
        @DisplayName("존재하지 않는 카드 삭제 시 예외를 발생시킨다")
        void deleteCard_withNonExistentId_throwsException() {
            // given
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cardDomainService.deleteCard(CARD_ID))
                    .isInstanceOf(CardException.class);
        }

        @Test
        @DisplayName("학습 기록이 존재하는 카드 삭제 시 예외를 발생시킨다")
        void deleteCard_withStudyRecords_throwsException() {
            // given
            given(cardRepository.findByIdAndStatus(CARD_ID, CardStatus.ACTIVE)).willReturn(Optional.of(testCard));
            given(studyRecordRepository.existsByCard(testCard)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> cardDomainService.deleteCard(CARD_ID))
                    .isInstanceOf(CardException.class)
                    .satisfies(exception -> {
                        CardException cardException = (CardException) exception;
                        assertThat(cardException.getErrorCode()).isEqualTo(CardErrorCode.CARD_HAS_STUDY_RECORDS);
                    });
        }
    }
}
