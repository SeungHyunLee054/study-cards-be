package com.example.study_cards.domain.generation.service;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.domain.generation.repository.GeneratedCardRepository;
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

class GeneratedCardDomainServiceTest extends BaseUnitTest {

    @Mock
    private GeneratedCardRepository generatedCardRepository;

    @InjectMocks
    private GeneratedCardDomainService generatedCardDomainService;

    private GeneratedCard testGeneratedCard;
    private Category testCategory;

    private static final Long CARD_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @BeforeEach
    void setUp() {
        testCategory = createTestCategory();
        testGeneratedCard = createTestGeneratedCard();
    }

    private Category createTestCategory() {
        Category category = Category.builder()
                .code("TOEIC")
                .name("토익")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }

    private GeneratedCard createTestGeneratedCard() {
        GeneratedCard card = GeneratedCard.builder()
                .model("gpt-5-mini")
                .sourceWord("abundant")
                .prompt("Test prompt")
                .question("The company has _____ resources.")
                .questionSub("(A) abundant (B) abundance (C) abundantly (D) abound")
                .answer("A")
                .answerSub("형용사 자리이므로 abundant가 정답입니다.")
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
    }

    @Nested
    @DisplayName("save")
    class SaveTest {

        @Test
        @DisplayName("생성된 카드를 저장한다")
        void save_savesAndReturnsCard() {
            // given
            given(generatedCardRepository.save(any(GeneratedCard.class))).willReturn(testGeneratedCard);

            // when
            GeneratedCard result = generatedCardDomainService.save(testGeneratedCard);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getModel()).isEqualTo("gpt-5-mini");
            verify(generatedCardRepository).save(any(GeneratedCard.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("ID로 생성된 카드를 조회한다")
        void findById_returnsCard() {
            // given
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when
            GeneratedCard result = generatedCardDomainService.findById(CARD_ID);

            // then
            assertThat(result.getId()).isEqualTo(CARD_ID);
            assertThat(result.getQuestion()).contains("resources");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
        void findById_withNonExistentId_throwsException() {
            // given
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> generatedCardDomainService.findById(CARD_ID))
                    .isInstanceOf(GenerationException.class)
                    .satisfies(exception -> {
                        GenerationException generationException = (GenerationException) exception;
                        assertThat(generationException.getErrorCode())
                                .isEqualTo(GenerationErrorCode.GENERATED_CARD_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTest {

        @Test
        @DisplayName("상태별로 생성된 카드를 조회한다")
        void findByStatus_returnsCardsWithStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedCard> cardPage = new PageImpl<>(List.of(testGeneratedCard), pageable, 1);
            given(generatedCardRepository.findByStatus(GenerationStatus.PENDING, pageable)).willReturn(cardPage);

            // when
            Page<GeneratedCard> result = generatedCardDomainService.findByStatus(GenerationStatus.PENDING, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(GenerationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("approve")
    class ApproveTest {

        @Test
        @DisplayName("생성된 카드를 승인한다")
        void approve_approvesCard() {
            // given
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when
            GeneratedCard result = generatedCardDomainService.approve(CARD_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(GenerationStatus.APPROVED);
            assertThat(result.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 승인된 카드를 승인하면 예외를 발생시킨다")
        void approve_alreadyApproved_throwsException() {
            // given
            testGeneratedCard.approve();
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when & then
            assertThatThrownBy(() -> generatedCardDomainService.approve(CARD_ID))
                    .isInstanceOf(GenerationException.class)
                    .satisfies(exception -> {
                        GenerationException generationException = (GenerationException) exception;
                        assertThat(generationException.getErrorCode())
                                .isEqualTo(GenerationErrorCode.ALREADY_APPROVED);
                    });
        }

        @Test
        @DisplayName("이미 이동된 카드를 승인하면 예외를 발생시킨다")
        void approve_alreadyMigrated_throwsException() {
            // given
            testGeneratedCard.approve();
            testGeneratedCard.markAsMigrated();
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when & then
            assertThatThrownBy(() -> generatedCardDomainService.approve(CARD_ID))
                    .isInstanceOf(GenerationException.class)
                    .satisfies(exception -> {
                        GenerationException generationException = (GenerationException) exception;
                        assertThat(generationException.getErrorCode())
                                .isEqualTo(GenerationErrorCode.ALREADY_MIGRATED);
                    });
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTest {

        @Test
        @DisplayName("생성된 카드를 거부한다")
        void reject_rejectsCard() {
            // given
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when
            GeneratedCard result = generatedCardDomainService.reject(CARD_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(GenerationStatus.REJECTED);
        }

        @Test
        @DisplayName("이미 거부된 카드를 거부하면 예외를 발생시킨다")
        void reject_alreadyRejected_throwsException() {
            // given
            testGeneratedCard.reject();
            given(generatedCardRepository.findById(CARD_ID)).willReturn(Optional.of(testGeneratedCard));

            // when & then
            assertThatThrownBy(() -> generatedCardDomainService.reject(CARD_ID))
                    .isInstanceOf(GenerationException.class)
                    .satisfies(exception -> {
                        GenerationException generationException = (GenerationException) exception;
                        assertThat(generationException.getErrorCode())
                                .isEqualTo(GenerationErrorCode.ALREADY_REJECTED);
                    });
        }
    }

    @Nested
    @DisplayName("findApprovedCards")
    class FindApprovedCardsTest {

        @Test
        @DisplayName("승인된 카드 목록을 조회한다")
        void findApprovedCards_returnsApprovedCards() {
            // given
            testGeneratedCard.approve();
            given(generatedCardRepository.findByStatusWithCategory(GenerationStatus.APPROVED))
                    .willReturn(List.of(testGeneratedCard));

            // when
            List<GeneratedCard> result = generatedCardDomainService.findApprovedCards();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isApproved()).isTrue();
        }
    }

    @Nested
    @DisplayName("countByStatus")
    class CountByStatusTest {

        @Test
        @DisplayName("상태별 카드 수를 조회한다")
        void countByStatus_returnsCount() {
            // given
            given(generatedCardRepository.countByStatus(GenerationStatus.PENDING)).willReturn(5L);

            // when
            long result = generatedCardDomainService.countByStatus(GenerationStatus.PENDING);

            // then
            assertThat(result).isEqualTo(5L);
        }
    }
}
