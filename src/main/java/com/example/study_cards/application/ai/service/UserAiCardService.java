package com.example.study_cards.application.ai.service;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.ai.dto.response.AiLimitResponse;
import com.example.study_cards.application.ai.dto.response.UserAiGenerationResponse;
import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.infra.redis.service.AiLimitService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAiCardService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final AiLimitService aiLimitService;
    private final AiGenerationService aiGenerationService;
    private final UserCardRepository userCardRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final CategoryDomainService categoryDomainService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserAiGenerationResponse generateCards(User user, GenerateUserCardRequest request) {
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);

        if (!plan.isCanGenerateAiCards()) {
            throw new AiException(AiErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }

        if (!aiLimitService.tryAcquireSlot(user.getId(), plan)) {
            throw new AiException(AiErrorCode.GENERATION_LIMIT_EXCEEDED);
        }

        Category category = categoryDomainService.findByCode(request.categoryCode());
        String prompt = buildUserCardPrompt(request);

        String aiResponse;
        try {
            aiResponse = aiGenerationService.generateContent(prompt);
        } catch (Exception e) {
            aiLimitService.releaseSlot(user.getId(), plan);
            saveFailureLog(user, request, prompt, e.getMessage());
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        List<UserCard> cards;
        try {
            cards = parseAndCreateUserCards(user, aiResponse, category);
            userCardRepository.saveAll(cards);
        } catch (AiException e) {
            aiLimitService.releaseSlot(user.getId(), plan);
            saveFailureLog(user, request, prompt, "응답 파싱 실패: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            aiLimitService.releaseSlot(user.getId(), plan);
            saveFailureLog(user, request, prompt, "카드 저장 실패: " + e.getMessage());
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        AiGenerationLog aiLog = AiGenerationLog.builder()
                .user(user)
                .type(AiGenerationType.USER_CARD)
                .prompt(request.sourceText())
                .response(aiResponse)
                .model(aiGenerationService.getDefaultModel())
                .cardsGenerated(cards.size())
                .success(true)
                .build();
        aiGenerationLogRepository.save(aiLog);

        int remaining = aiLimitService.getRemainingCount(user.getId(), plan);

        return UserAiGenerationResponse.from(cards, remaining);
    }

    public AiLimitResponse getGenerationLimit(User user) {
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);
        int limit = plan.getAiGenerationDailyLimit();
        int used = aiLimitService.getUsedCount(user.getId(), plan);
        int remaining = Math.max(0, limit - used);
        boolean isLifetime = (plan == SubscriptionPlan.FREE);

        return new AiLimitResponse(limit, used, remaining, isLifetime);
    }

    private String buildUserCardPrompt(GenerateUserCardRequest request) {
        String difficulty = request.difficulty() != null ? request.difficulty() : "보통";
        return String.format("""
                당신은 학습 전문가입니다.
                아래 텍스트를 분석하여 %d개의 학습 카드를 생성하세요.

                입력 텍스트:
                %s

                요구사항:
                1. 핵심 개념을 질문-답변 형식으로 변환
                2. 난이도: %s
                3. 각 카드는 독립적으로 이해 가능해야 함
                4. 한글로 작성

                출력 형식 (JSON 배열만 출력, 다른 텍스트 없이):
                [
                  {
                    "question": "질문",
                    "questionSub": "부가 설명 (선택, 없으면 null)",
                    "answer": "답변",
                    "answerSub": "부가 정보 (선택, 없으면 null)"
                  }
                ]
                """,
                request.count(),
                request.sourceText(),
                difficulty
        );
    }

    private List<UserCard> parseAndCreateUserCards(User user, String aiResponse, Category category) {
        String json = extractJson(aiResponse);
        List<Map<String, String>> parsed = parseJsonArray(json);

        List<UserCard> cards = new ArrayList<>();
        for (Map<String, String> data : parsed) {
            String question = data.get("question");
            String answer = data.get("answer");
            if (question == null || answer == null || question.isBlank() || answer.isBlank()) {
                continue;
            }

            UserCard card = UserCard.builder()
                    .user(user)
                    .question(question)
                    .questionSub(data.get("questionSub"))
                    .answer(answer)
                    .answerSub(data.get("answerSub"))
                    .category(category)
                    .aiGenerated(true)
                    .build();
            cards.add(card);
        }

        if (cards.isEmpty()) {
            throw new AiException(AiErrorCode.INVALID_AI_RESPONSE);
        }

        return cards;
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private List<Map<String, String>> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage());
            throw new AiException(AiErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private void saveFailureLog(User user, GenerateUserCardRequest request, String prompt, String errorMessage) {
        try {
            AiGenerationLog failLog = AiGenerationLog.builder()
                    .user(user)
                    .type(AiGenerationType.USER_CARD)
                    .prompt(request.sourceText())
                    .model(aiGenerationService.getDefaultModel())
                    .cardsGenerated(0)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
            aiGenerationLogRepository.save(failLog);
        } catch (Exception e) {
            log.error("AI 실패 로그 저장 실패: {}", e.getMessage());
        }
    }
}
