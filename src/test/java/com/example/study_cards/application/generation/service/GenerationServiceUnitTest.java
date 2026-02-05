package com.example.study_cards.application.generation.service;

import com.example.study_cards.application.generation.dto.request.GenerationRequest;
import com.example.study_cards.application.generation.dto.response.GenerationResultResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.domain.generation.service.GeneratedCardDomainService;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.support.BaseUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class GenerationServiceUnitTest extends BaseUnitTest {

    @Mock
    private GeneratedCardDomainService generatedCardDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @Mock
    private AiGenerationService aiGenerationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationService generationService;

    private Category toeicCategory;
    private Category jlptCategory;
    private Card testCard;

    private static final String STUB_RESPONSE = """
            {
              "question": "The company has _____ resources.",
              "options": ["abundant", "abundance", "abundantly", "abound"],
              "answer": "A",
              "explanation": "형용사 자리이므로 abundant가 정답입니다."
            }
            """;

    @BeforeEach
    void setUp() {
        toeicCategory = createCategory("TOEIC", "토익", 1L);
        jlptCategory = createCategory("JN_N3", "JLPT N3", 2L);
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
                .question("abundant")
                .questionSub("풍부한")
                .answer("abundant")
                .answerSub("형용사, 많은, 풍부한")
                .efFactor(2.5)
                .category(toeicCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", 1L);
        return card;
    }

    @Nested
    @DisplayName("generateCards")
    class GenerateCardsTest {

        @Test
        @DisplayName("AI를 사용하여 카드를 생성하고 저장한다")
        void generateCards_generatesAndSavesCards() {
            // given
            GenerationRequest request = new GenerationRequest("TOEIC", 1, null);

            given(categoryDomainService.findByCode("TOEIC")).willReturn(toeicCategory);
            given(cardDomainService.findByCategory(toeicCategory)).willReturn(List.of(testCard));
            given(aiGenerationService.getDefaultModel()).willReturn("gpt-5-mini");
            given(aiGenerationService.generateContent(anyString(), anyString())).willReturn(STUB_RESPONSE);

            GeneratedCard savedCard = GeneratedCard.builder()
                    .model("gpt-5-mini")
                    .sourceWord("abundant")
                    .prompt("Test prompt")
                    .question("The company has _____ resources.")
                    .questionSub("(A) abundant (B) abundance (C) abundantly (D) abound")
                    .answer("A")
                    .answerSub("형용사 자리이므로 abundant가 정답입니다.")
                    .category(toeicCategory)
                    .build();
            ReflectionTestUtils.setField(savedCard, "id", 1L);

            given(generatedCardDomainService.saveAll(any())).willReturn(List.of(savedCard));

            // when
            GenerationResultResponse result = generationService.generateCards(request);

            // then
            assertThat(result.totalGenerated()).isEqualTo(1);
            assertThat(result.categoryCode()).isEqualTo("TOEIC");
            assertThat(result.model()).isEqualTo("gpt-5-mini");
            assertThat(result.generatedCards()).hasSize(1);
        }

        @Test
        @DisplayName("카테고리에 카드가 없으면 예외를 발생시킨다")
        void generateCards_withNoCards_throwsException() {
            // given
            GenerationRequest request = new GenerationRequest("TOEIC", 1, null);

            given(categoryDomainService.findByCode("TOEIC")).willReturn(toeicCategory);
            given(cardDomainService.findByCategory(toeicCategory)).willReturn(new ArrayList<>());

            // when & then
            assertThatThrownBy(() -> generationService.generateCards(request))
                    .isInstanceOf(GenerationException.class)
                    .satisfies(exception -> {
                        GenerationException generationException = (GenerationException) exception;
                        assertThat(generationException.getErrorCode())
                                .isEqualTo(GenerationErrorCode.NO_CARDS_TO_GENERATE);
                    });
        }

        @Test
        @DisplayName("요청에 모델이 지정되면 해당 모델을 사용한다")
        void generateCards_withSpecifiedModel_usesSpecifiedModel() {
            // given
            GenerationRequest request = new GenerationRequest("TOEIC", 1, "gemini-2.5-flash");

            given(categoryDomainService.findByCode("TOEIC")).willReturn(toeicCategory);
            given(cardDomainService.findByCategory(toeicCategory)).willReturn(List.of(testCard));
            given(aiGenerationService.generateContent(anyString(), anyString())).willReturn(STUB_RESPONSE);

            GeneratedCard savedCard = GeneratedCard.builder()
                    .model("gemini-2.5-flash")
                    .sourceWord("abundant")
                    .prompt("Test prompt")
                    .question("The company has _____ resources.")
                    .questionSub("(A) abundant (B) abundance (C) abundantly (D) abound")
                    .answer("A")
                    .answerSub("형용사 자리이므로 abundant가 정답입니다.")
                    .category(toeicCategory)
                    .build();
            ReflectionTestUtils.setField(savedCard, "id", 1L);

            given(generatedCardDomainService.saveAll(any())).willReturn(List.of(savedCard));

            // when
            GenerationResultResponse result = generationService.generateCards(request);

            // then
            assertThat(result.model()).isEqualTo("gemini-2.5-flash");
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTest {

        @Test
        @DisplayName("생성 통계를 조회한다")
        void getStats_returnsStatistics() {
            // given
            List<Object[]> rawStats = List.of(
                    new Object[]{"gpt-5-mini", GenerationStatus.PENDING, 5L},
                    new Object[]{"gpt-5-mini", GenerationStatus.APPROVED, 10L},
                    new Object[]{"gpt-5-mini", GenerationStatus.REJECTED, 2L}
            );

            given(generatedCardDomainService.countByModelGroupByStatus()).willReturn(rawStats);
            given(generatedCardDomainService.count()).willReturn(17L);
            given(generatedCardDomainService.countByStatus(GenerationStatus.APPROVED)).willReturn(10L);
            given(generatedCardDomainService.countByStatus(GenerationStatus.REJECTED)).willReturn(2L);
            given(generatedCardDomainService.countByStatus(GenerationStatus.PENDING)).willReturn(5L);
            given(generatedCardDomainService.countByStatus(GenerationStatus.MIGRATED)).willReturn(0L);

            // when
            GenerationStatsResponse result = generationService.getStats();

            // then
            assertThat(result.overall().totalGenerated()).isEqualTo(17L);
            assertThat(result.overall().approved()).isEqualTo(10L);
            assertThat(result.overall().rejected()).isEqualTo(2L);
            assertThat(result.overall().pending()).isEqualTo(5L);
            assertThat(result.byModel()).hasSize(1);
            assertThat(result.byModel().get(0).model()).isEqualTo("gpt-5-mini");
        }
    }
}
