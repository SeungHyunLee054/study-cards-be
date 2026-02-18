package com.example.study_cards.application.generation.service;

import com.example.study_cards.application.ai.prompt.AiPromptTemplateFactory;
import com.example.study_cards.application.generation.dto.request.GenerationRequest;
import com.example.study_cards.application.generation.dto.response.GeneratedCardResponse;
import com.example.study_cards.application.generation.dto.response.GenerationResultResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse.ModelStats;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse.OverallStats;
import com.example.study_cards.common.util.AiCategoryType;
import com.example.study_cards.common.util.AiResponseUtils;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class GenerationService {

    private final GeneratedCardDomainService generatedCardDomainService;
    private final CardDomainService cardDomainService;
    private final CategoryDomainService categoryDomainService;
    private final AiGenerationService aiGenerationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenerationResultResponse generateCards(GenerationRequest request) {
        Category category = categoryDomainService.findByCode(request.categoryCode());
        String model = aiGenerationService.getDefaultModel();

        List<Card> sourceCards = resolveSourceCards(request, category);
        List<GeneratedCard> generatedCards = new ArrayList<>();

        for (Card sourceCard : sourceCards) {
            String prompt = AiPromptTemplateFactory.buildPrompt(sourceCard, category);
            String aiResponse = aiGenerationService.generateContent(prompt);
            GeneratedCard generatedCard = parseAndCreateGeneratedCard(aiResponse, sourceCard, category, model, prompt);
            generatedCards.add(generatedCard);
        }

        List<GeneratedCard> savedCards = generatedCardDomainService.saveAll(generatedCards);
        List<GeneratedCardResponse> responses = savedCards.stream()
                .map(GeneratedCardResponse::from)
                .toList();

        String mode = hasSelectedSourceCards(request) ? "manual" : "random";
        log.info("AI 문제 생성 완료 - category: {}, mode: {}, count: {}",
                request.categoryCode(), mode, savedCards.size());

        return GenerationResultResponse.of(responses, request.categoryCode(), model);
    }

    public GenerationStatsResponse getStats() {
        List<Object[]> rawStats = generatedCardDomainService.countByModelGroupByStatus();

        Map<String, Map<GenerationStatus, Long>> modelStatsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            String model = (String) row[0];
            GenerationStatus status = (GenerationStatus) row[1];
            Long count = (Long) row[2];

            modelStatsMap.computeIfAbsent(model, k -> new EnumMap<>(GenerationStatus.class))
                    .put(status, count);
        }

        List<ModelStats> modelStatsList = modelStatsMap.entrySet().stream()
                .map(entry -> {
                    String model = entry.getKey();
                    Map<GenerationStatus, Long> counts = entry.getValue();
                    long total = counts.values().stream().mapToLong(Long::longValue).sum();
                    long approved = counts.getOrDefault(GenerationStatus.APPROVED, 0L);
                    long rejected = counts.getOrDefault(GenerationStatus.REJECTED, 0L);
                    long pending = counts.getOrDefault(GenerationStatus.PENDING, 0L);
                    long migrated = counts.getOrDefault(GenerationStatus.MIGRATED, 0L);
                    return ModelStats.of(model, total, approved, rejected, pending, migrated);
                })
                .sorted(Comparator.comparing(ModelStats::model))
                .collect(Collectors.toList());

        long totalGenerated = generatedCardDomainService.count();
        long totalApproved = generatedCardDomainService.countByStatus(GenerationStatus.APPROVED);
        long totalRejected = generatedCardDomainService.countByStatus(GenerationStatus.REJECTED);
        long totalPending = generatedCardDomainService.countByStatus(GenerationStatus.PENDING);
        long totalMigrated = generatedCardDomainService.countByStatus(GenerationStatus.MIGRATED);

        OverallStats overallStats = OverallStats.of(totalGenerated, totalApproved, totalRejected, totalPending, totalMigrated);

        return GenerationStatsResponse.of(modelStatsList, overallStats);
    }

    private List<Card> selectRandomCards(List<Card> cards, int count) {
        List<Card> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private List<Card> resolveSourceCards(GenerationRequest request, Category category) {
        if (!hasSelectedSourceCards(request)) {
            List<Card> existingCards = cardDomainService.findByCategory(category);
            if (existingCards.isEmpty()) {
                throw new GenerationException(GenerationErrorCode.NO_CARDS_TO_GENERATE);
            }
            return selectRandomCards(existingCards, request.count());
        }

        List<Long> requestedIds = request.sourceCardIds().stream()
                .distinct()
                .toList();
        List<Card> selectedCards = cardDomainService.findByIdsInCategory(requestedIds, category);
        if (selectedCards.size() != requestedIds.size()) {
            throw new GenerationException(GenerationErrorCode.INVALID_SOURCE_CARD_SELECTION);
        }
        return selectedCards;
    }

    private boolean hasSelectedSourceCards(GenerationRequest request) {
        return request.sourceCardIds() != null && !request.sourceCardIds().isEmpty();
    }

    private GeneratedCard parseAndCreateGeneratedCard(String aiResponse, Card sourceCard,
                                                       Category category, String model, String prompt) {
        try {
            String cleanedResponse = AiResponseUtils.extractJsonPayload(aiResponse);
            JsonNode json = objectMapper.readTree(cleanedResponse);

            if (AiCategoryType.fromCode(category.getCode()).isQuizType()) {
                return parseQuizResponse(json, sourceCard, category, model, prompt);
            } else {
                return parseQaResponse(json, sourceCard, category, model, prompt);
            }

        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("AI 응답 파싱 실패 - response: {}", aiResponse, e);
            throw new GenerationException(GenerationErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private GeneratedCard parseQuizResponse(JsonNode json, Card sourceCard,
                                             Category category, String model, String prompt) {
        String question = getRequiredField(json, "question");
        JsonNode optionsNode = json.get("options");
        String answer = getRequiredField(json, "answer");
        String explanation = getFieldOrDefault(json, "explanation");

        StringBuilder questionSub = new StringBuilder();
        if (optionsNode != null && optionsNode.isArray()) {
            for (int i = 0; i < optionsNode.size(); i++) {
                if (i > 0) questionSub.append(" ");
                char optionLabel = (char) ('A' + i);
                questionSub.append("(").append(optionLabel).append(") ").append(optionsNode.get(i).asText());
            }
        }

        return GeneratedCard.builder()
                .model(model)
                .sourceWord(sourceCard.getQuestion())
                .prompt(prompt)
                .question(question)
                .questionSub(questionSub.toString())
                .answer(answer)
                .answerSub(explanation)
                .category(category)
                .build();
    }

    private GeneratedCard parseQaResponse(JsonNode json, Card sourceCard,
                                           Category category, String model, String prompt) {
        String question = getRequiredField(json, "question");
        String questionSub = getFieldOrDefault(json, "questionSub");
        String answer = getRequiredField(json, "answer");
        String answerSub = getFieldOrDefault(json, "answerSub");

        return GeneratedCard.builder()
                .model(model)
                .sourceWord(sourceCard.getQuestion())
                .prompt(prompt)
                .question(question)
                .questionSub(questionSub)
                .answer(answer)
                .answerSub(answerSub)
                .category(category)
                .build();
    }

    private String getRequiredField(JsonNode json, String fieldName) {
        JsonNode node = json.get(fieldName);
        if (node == null || node.isNull()) {
            log.error("AI 응답에 필수 필드 누락 - field: {}", fieldName);
            throw new GenerationException(GenerationErrorCode.INVALID_AI_RESPONSE);
        }
        return node.asText();
    }

    private String getFieldOrDefault(JsonNode json, String fieldName) {
        JsonNode node = json.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText();
    }

}
