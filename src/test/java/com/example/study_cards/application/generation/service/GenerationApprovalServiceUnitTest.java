package com.example.study_cards.application.generation.service;

import com.example.study_cards.application.generation.dto.request.ApprovalRequest;
import com.example.study_cards.application.generation.dto.response.GeneratedCardResponse;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.service.GeneratedCardDomainService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GenerationApprovalServiceUnitTest extends BaseUnitTest {

    @Mock
    private GeneratedCardDomainService generatedCardDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @InjectMocks
    private GenerationApprovalService approvalService;

    private GeneratedCard testGeneratedCard;
    private Category testCategory;

    private static final Long CARD_ID = 1L;

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
        ReflectionTestUtils.setField(category, "id", 1L);
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
    @DisplayName("getGeneratedCards")
    class GetGeneratedCardsTest {

        @Test
        @DisplayName("상태와 모델로 생성된 카드를 조회한다")
        void getGeneratedCards_withStatusAndModel_returnsFilteredCards() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedCard> cardPage = new PageImpl<>(List.of(testGeneratedCard), pageable, 1);
            given(generatedCardDomainService.findByStatusAndModel(GenerationStatus.PENDING, "gpt-5-mini", pageable))
                    .willReturn(cardPage);

            // when
            Page<GeneratedCardResponse> result = approvalService.getGeneratedCards(
                    GenerationStatus.PENDING, "gpt-5-mini", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).model()).isEqualTo("gpt-5-mini");
        }

        @Test
        @DisplayName("상태만으로 생성된 카드를 조회한다")
        void getGeneratedCards_withStatusOnly_returnsFilteredCards() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedCard> cardPage = new PageImpl<>(List.of(testGeneratedCard), pageable, 1);
            given(generatedCardDomainService.findByStatus(GenerationStatus.PENDING, pageable)).willReturn(cardPage);

            // when
            Page<GeneratedCardResponse> result = approvalService.getGeneratedCards(
                    GenerationStatus.PENDING, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("모델만으로 생성된 카드를 조회한다")
        void getGeneratedCards_withModelOnly_returnsFilteredCards() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedCard> cardPage = new PageImpl<>(List.of(testGeneratedCard), pageable, 1);
            given(generatedCardDomainService.findByModel("gpt-5-mini", pageable)).willReturn(cardPage);

            // when
            Page<GeneratedCardResponse> result = approvalService.getGeneratedCards(
                    null, "gpt-5-mini", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("필터 없이 모든 생성된 카드를 조회한다")
        void getGeneratedCards_withoutFilters_returnsAllCards() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedCard> cardPage = new PageImpl<>(List.of(testGeneratedCard), pageable, 1);
            given(generatedCardDomainService.findAll(pageable)).willReturn(cardPage);

            // when
            Page<GeneratedCardResponse> result = approvalService.getGeneratedCards(null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getGeneratedCard")
    class GetGeneratedCardTest {

        @Test
        @DisplayName("ID로 생성된 카드를 조회한다")
        void getGeneratedCard_returnsCard() {
            // given
            given(generatedCardDomainService.findById(CARD_ID)).willReturn(testGeneratedCard);

            // when
            GeneratedCardResponse result = approvalService.getGeneratedCard(CARD_ID);

            // then
            assertThat(result.id()).isEqualTo(CARD_ID);
            assertThat(result.model()).isEqualTo("gpt-5-mini");
        }
    }

    @Nested
    @DisplayName("approve")
    class ApproveTest {

        @Test
        @DisplayName("생성된 카드를 승인한다")
        void approve_approvesCard() {
            // given
            GeneratedCard approvedCard = createTestGeneratedCard();
            approvedCard.approve();
            given(generatedCardDomainService.approve(CARD_ID)).willReturn(approvedCard);

            // when
            GeneratedCardResponse result = approvalService.approve(CARD_ID);

            // then
            assertThat(result.status()).isEqualTo(GenerationStatus.APPROVED);
            assertThat(result.approvedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTest {

        @Test
        @DisplayName("생성된 카드를 거부한다")
        void reject_rejectsCard() {
            // given
            GeneratedCard rejectedCard = createTestGeneratedCard();
            rejectedCard.reject();
            given(generatedCardDomainService.reject(CARD_ID)).willReturn(rejectedCard);

            // when
            GeneratedCardResponse result = approvalService.reject(CARD_ID);

            // then
            assertThat(result.status()).isEqualTo(GenerationStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("batchApprove")
    class BatchApproveTest {

        @Test
        @DisplayName("여러 카드를 일괄 승인한다")
        void batchApprove_approvesMultipleCards() {
            // given
            ApprovalRequest request = new ApprovalRequest(List.of(1L, 2L, 3L));

            GeneratedCard card1 = createTestGeneratedCard();
            card1.approve();
            GeneratedCard card2 = createTestGeneratedCard();
            ReflectionTestUtils.setField(card2, "id", 2L);
            card2.approve();
            GeneratedCard card3 = createTestGeneratedCard();
            ReflectionTestUtils.setField(card3, "id", 3L);
            card3.approve();

            given(generatedCardDomainService.approve(1L)).willReturn(card1);
            given(generatedCardDomainService.approve(2L)).willReturn(card2);
            given(generatedCardDomainService.approve(3L)).willReturn(card3);

            // when
            List<GeneratedCardResponse> result = approvalService.batchApprove(request);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).allMatch(card -> card.status() == GenerationStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("migrateApprovedToCards")
    class MigrateApprovedToCardsTest {

        @Test
        @DisplayName("승인된 카드를 Card 테이블로 이동한다")
        void migrateApprovedToCards_migratesCards() {
            // given
            GeneratedCard approvedCard = createTestGeneratedCard();
            approvedCard.approve();
            Page<GeneratedCard> pageWithData = new PageImpl<>(List.of(approvedCard), PageRequest.of(0, 100), 1);
            Page<GeneratedCard> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            given(generatedCardDomainService.findApprovedCards(any(Pageable.class)))
                    .willReturn(pageWithData)
                    .willReturn(emptyPage);

            // when
            int result = approvalService.migrateApprovedToCards();

            // then
            assertThat(result).isEqualTo(1);
            verify(cardDomainService).createCard(
                    eq("The company has _____ resources."),
                    eq("(A) abundant (B) abundance (C) abundantly (D) abound"),
                    eq("A"),
                    eq("형용사 자리이므로 abundant가 정답입니다."),
                    eq(testCategory),
                    eq(true)
            );
            verify(generatedCardDomainService).markAsMigrated(approvedCard);
        }

        @Test
        @DisplayName("승인된 카드가 없으면 0을 반환한다")
        void migrateApprovedToCards_withNoCards_returnsZero() {
            // given
            Page<GeneratedCard> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            given(generatedCardDomainService.findApprovedCards(any(Pageable.class))).willReturn(emptyPage);

            // when
            int result = approvalService.migrateApprovedToCards();

            // then
            assertThat(result).isEqualTo(0);
        }
    }
}
