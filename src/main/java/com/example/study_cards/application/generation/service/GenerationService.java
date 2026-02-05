package com.example.study_cards.application.generation.service;

import com.example.study_cards.application.generation.dto.request.GenerationRequest;
import com.example.study_cards.application.generation.dto.response.GeneratedCardResponse;
import com.example.study_cards.application.generation.dto.response.GenerationResultResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse.ModelStats;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse.OverallStats;
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
        String model = resolveModel(request.model());

        List<Card> existingCards = cardDomainService.findByCategory(category);
        if (existingCards.isEmpty()) {
            throw new GenerationException(GenerationErrorCode.NO_CARDS_TO_GENERATE);
        }

        List<Card> sourceCards = selectRandomCards(existingCards, request.count());
        List<GeneratedCard> generatedCards = new ArrayList<>();

        for (Card sourceCard : sourceCards) {
            String prompt = buildPrompt(sourceCard, category);
            String aiResponse = aiGenerationService.generateContent(prompt, model);
            GeneratedCard generatedCard = parseAndCreateGeneratedCard(aiResponse, sourceCard, category, model, prompt);
            generatedCards.add(generatedCard);
        }

        List<GeneratedCard> savedCards = generatedCardDomainService.saveAll(generatedCards);
        List<GeneratedCardResponse> responses = savedCards.stream()
                .map(GeneratedCardResponse::from)
                .toList();

        log.info("AI 문제 생성 완료 - category: {}, count: {}, model: {}",
                request.categoryCode(), savedCards.size(), model);

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

    private String resolveModel(String requestedModel) {
        return requestedModel != null && !requestedModel.isBlank()
                ? requestedModel
                : aiGenerationService.getDefaultModel();
    }

    private List<Card> selectRandomCards(List<Card> cards, int count) {
        List<Card> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private String buildPrompt(Card sourceCard, Category category) {
        String categoryCode = category.getCode().toUpperCase();

        if (categoryCode.startsWith("JLPT") || categoryCode.startsWith("JN_")) {
            return buildJlptPrompt(sourceCard, categoryCode);
        } else if (categoryCode.equals("TOEIC") || categoryCode.startsWith("EN_")) {
            return buildToeicPrompt(sourceCard);
        } else if (categoryCode.equals("CS") || categoryCode.startsWith("CS_")) {
            return buildCsPrompt(sourceCard);
        } else {
            return buildGenericPrompt(sourceCard, category);
        }
    }

    private boolean isQuizType(Category category) {
        String code = category.getCode().toUpperCase();
        return code.startsWith("JLPT") || code.startsWith("JN_") ||
               code.equals("TOEIC") || code.startsWith("EN_");
    }

    private String buildToeicPrompt(Card sourceCard) {
        return """
            당신은 TOEIC 출제 전문가입니다.
            다음 단어/문장을 사용하여 TOEIC Part 5 스타일의 문제를 생성하세요.

            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 실제 비즈니스/일상 상황의 예문 작성
            2. 4개의 선택지 (정답 1개 + 오답 3개)
            3. 오답은 비슷한 형태의 단어로 구성 (품사 변형, 유사어 등)
            4. 난이도: 중급

            JSON 형식으로 응답 (한글 해설 포함):
            {
              "question": "예문 (빈칸 포함)",
              "options": ["A", "B", "C", "D"],
              "answer": "정답 알파벳",
              "explanation": "간단한 해설"
            }
            """.formatted(sourceCard.getQuestion(), sourceCard.getAnswer());
    }

    private String buildJlptPrompt(Card sourceCard, String level) {
        String jlptLevel = level.replace("JN_", "N").replace("JLPT_", "");
        return """
            당신은 JLPT 출제 전문가입니다.
            다음 단어/문장을 사용하여 JLPT %s 스타일의 문제를 생성하세요.

            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 자연스러운 일본어 예문 작성
            2. 4개의 선택지 (정답 1개 + 오답 3개)
            3. 오답은 문맥상 헷갈릴 수 있는 단어로 구성
            4. 난이도: %s 레벨에 맞게

            JSON 형식으로 응답 (한국어 해설 포함):
            {
              "question": "예문 (빈칸 포함)",
              "options": ["1", "2", "3", "4"],
              "answer": "정답 번호",
              "explanation": "간단한 해설 (한국어)"
            }
            """.formatted(jlptLevel, sourceCard.getQuestion(), sourceCard.getAnswer(), jlptLevel);
    }

    private String buildCsPrompt(Card sourceCard) {
        return """
            당신은 컴퓨터 공학 교육 전문가입니다.
            다음 주제를 기반으로 새로운 학습 카드를 생성하세요.

            참고 질문: %s
            참고 답변: %s

            요구사항:
            1. 참고 내용과 관련되지만 다른 관점의 새로운 질문 작성
            2. 한글로 작성
            3. 답변은 명확하고 이해하기 쉽게 작성

            JSON 형식으로 응답:
            {
              "question": "질문",
              "answer": "답변"
            }
            """.formatted(sourceCard.getQuestion(), sourceCard.getAnswer());
    }

    private String buildGenericPrompt(Card sourceCard, Category category) {
        return """
            다음 학습 자료를 기반으로 4지선다 문제를 생성하세요.

            카테고리: %s
            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 원본 내용을 기반으로 새로운 관점의 문제 작성
            2. 4개의 선택지 (정답 1개 + 오답 3개)
            3. 오답은 그럴듯하지만 틀린 내용으로 구성

            JSON 형식으로 응답:
            {
              "question": "문제",
              "options": ["A", "B", "C", "D"],
              "answer": "정답 알파벳",
              "explanation": "간단한 해설"
            }
            """.formatted(category.getName(), sourceCard.getQuestion(), sourceCard.getAnswer());
    }

    private GeneratedCard parseAndCreateGeneratedCard(String aiResponse, Card sourceCard,
                                                       Category category, String model, String prompt) {
        try {
            String cleanedResponse = stripMarkdownCodeBlock(aiResponse);
            JsonNode json = objectMapper.readTree(cleanedResponse);

            if (isQuizType(category)) {
                return parseQuizResponse(json, sourceCard, category, model, prompt);
            } else {
                return parseQaResponse(json, sourceCard, category, model, prompt);
            }

        } catch (JsonProcessingException e) {
            log.error("AI 응답 파싱 실패 - response: {}", aiResponse, e);
            throw new GenerationException(GenerationErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private GeneratedCard parseQuizResponse(JsonNode json, Card sourceCard,
                                             Category category, String model, String prompt) {
        String question = getRequiredField(json, "question");
        JsonNode optionsNode = json.get("options");
        String answer = getRequiredField(json, "answer");
        String explanation = getFieldOrDefault(json, "explanation", "");

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
        String questionSub = getFieldOrDefault(json, "questionSub", "");
        String answer = getRequiredField(json, "answer");
        String answerSub = getFieldOrDefault(json, "answerSub", "");

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

    private String getFieldOrDefault(JsonNode json, String fieldName, String defaultValue) {
        JsonNode node = json.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asText();
    }

    private String stripMarkdownCodeBlock(String response) {
        if (response == null) {
            return response;
        }

        String trimmed = response.trim();

        // Check for ```json or ``` at the start
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        // Check for ``` at the end
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
