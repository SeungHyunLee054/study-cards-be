package com.example.study_cards.application.ai.service;

import com.example.study_cards.application.ai.prompt.AiInputCategoryMatcher;
import com.example.study_cards.application.ai.prompt.AiPromptTemplateFactory;
import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.ai.dto.response.AiLimitResponse;
import com.example.study_cards.application.ai.dto.response.UserAiGenerationResponse;
import com.example.study_cards.common.util.AiCategoryType;
import com.example.study_cards.common.util.AiResponseUtils;
import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.service.AiGenerationLogDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.exception.CategoryErrorCode;
import com.example.study_cards.domain.category.exception.CategoryException;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
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
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAiCardService {

    private static final int UNLIMITED_COUNT = Integer.MAX_VALUE;
    private static final String EN_MISC_CODE = "EN_MISC";
    private static final String JN_MISC_CODE = "JN_MISC";
    private static final String MISC_GENERAL_CODE = "MISC_GENERAL";
    private static final String LEGACY_MISC_CODE = "ETC";
    private static final String LEGACY_MISC_NAME = "기타";

    private final SubscriptionDomainService subscriptionDomainService;
    private final AiLimitService aiLimitService;
    private final AiGenerationService aiGenerationService;
    private final UserCardDomainService userCardDomainService;
    private final AiGenerationLogDomainService aiGenerationLogDomainService;
    private final CategoryDomainService categoryDomainService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserAiGenerationResponse generateCards(User user, GenerateUserCardRequest request) {
        boolean isAdmin = isAdmin(user);
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);
        boolean slotAcquired = tryAcquireSlotIfNeeded(user, plan, isAdmin);

        Category requestedCategory = categoryDomainService.findByCode(request.categoryCode());
        Category category = resolveEffectiveCategory(requestedCategory, request.sourceText());
        String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

        String aiResponse;
        try {
            aiResponse = aiGenerationService.generateContent(prompt);
        } catch (Exception e) {
            handleFailure(user, request, plan, slotAcquired, e.getMessage());
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        List<UserCard> cards;
        try {
            cards = parseAndCreateUserCards(user, aiResponse, category);
            userCardDomainService.saveAll(cards);
        } catch (AiException e) {
            handleFailure(user, request, plan, slotAcquired, "응답 파싱 실패: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            handleFailure(user, request, plan, slotAcquired, "카드 저장 실패: " + e.getMessage());
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
        aiGenerationLogDomainService.save(aiLog);

        int remaining = isAdmin ? UNLIMITED_COUNT : aiLimitService.getRemainingCount(user.getId(), plan);

        return UserAiGenerationResponse.from(cards, remaining);
    }

    public AiLimitResponse getGenerationLimit(User user) {
        if (isAdmin(user)) {
            return new AiLimitResponse(UNLIMITED_COUNT, 0, UNLIMITED_COUNT, false);
        }

        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);
        int limit = plan.getAiGenerationDailyLimit();
        int used = aiLimitService.getUsedCount(user.getId(), plan);
        int remaining = Math.max(0, limit - used);
        boolean isLifetime = (plan == SubscriptionPlan.FREE);

        return new AiLimitResponse(limit, used, remaining, isLifetime);
    }

    private boolean isAdmin(User user) {
        return user.hasRole(Role.ROLE_ADMIN);
    }

    private boolean tryAcquireSlotIfNeeded(User user, SubscriptionPlan plan, boolean isAdmin) {
        if (isAdmin) {
            return false;
        }

        if (!plan.isCanGenerateAiCards()) {
            throw new AiException(AiErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }

        if (!aiLimitService.tryAcquireSlot(user.getId(), plan)) {
            throw new AiException(AiErrorCode.GENERATION_LIMIT_EXCEEDED);
        }

        return true;
    }

    private void releaseSlotIfAcquired(User user, SubscriptionPlan plan, boolean slotAcquired) {
        if (slotAcquired) {
            aiLimitService.releaseSlot(user.getId(), plan);
        }
    }

    private void handleFailure(
            User user,
            GenerateUserCardRequest request,
            SubscriptionPlan plan,
            boolean slotAcquired,
            String errorMessage
    ) {
        releaseSlotIfAcquired(user, plan, slotAcquired);
        saveFailureLog(user, request, errorMessage);
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
        try {
            return AiResponseUtils.extractJsonPayload(response);
        } catch (IllegalArgumentException e) {
            throw new AiException(AiErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private List<Map<String, String>> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage());
            throw new AiException(AiErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private Category resolveEffectiveCategory(Category requestedCategory, String sourceText) {
        if (!categoryDomainService.isLeafCategory(requestedCategory)) {
            Category mappedLeafCategory = resolveMappedFallbackCategory(requestedCategory);
            if (mappedLeafCategory != null) {
                log.info("[AI] 상위 카테고리 선택 감지, leaf 기타 카테고리로 매핑 - requested: {}, mapped: {}",
                        requestedCategory.getCode(), mappedLeafCategory.getCode());
                return mappedLeafCategory;
            }

            log.info("[AI] 상위 카테고리 선택 감지됐지만 leaf 매핑 카테고리를 찾지 못해 요청 거부 - requested: {}",
                    requestedCategory.getCode());
            throw new CategoryException(CategoryErrorCode.CATEGORY_NOT_LEAF);
        }

        AiCategoryType categoryType = AiCategoryType.fromCode(requestedCategory.getCode());
        if (AiInputCategoryMatcher.isLikelyMatch(categoryType, sourceText)) {
            return requestedCategory;
        }

        Category fallbackCategory = resolveMappedFallbackCategory(requestedCategory);
        if (fallbackCategory == null) {
            log.info("[AI] 카테고리 불일치 감지됐지만 기타 카테고리를 찾지 못해 요청 카테고리 유지 - requested: {}",
                    requestedCategory.getCode());
            return requestedCategory;
        }

        if (fallbackCategory.getId().equals(requestedCategory.getId())) {
            return requestedCategory;
        }

        log.info("[AI] 카테고리-텍스트 불일치 감지, 기타 카테고리로 폴백 - requested: {}, fallback: {}",
                requestedCategory.getCode(), fallbackCategory.getCode());
        return fallbackCategory;
    }

    private Category resolveMappedFallbackCategory(Category requestedCategory) {
        String mappedCode = determineFallbackCode(requestedCategory.getCode());

        Category byCode = categoryDomainService.findByCodeOrNull(mappedCode);
        if (byCode != null && categoryDomainService.isLeafCategory(byCode)) {
            return byCode;
        }

        Category legacyCode = categoryDomainService.findByCodeOrNull(LEGACY_MISC_CODE);
        if (legacyCode != null && categoryDomainService.isLeafCategory(legacyCode)) {
            return legacyCode;
        }

        Category legacyName = categoryDomainService.findByNameOrNull(LEGACY_MISC_NAME);
        if (legacyName != null && categoryDomainService.isLeafCategory(legacyName)) {
            return legacyName;
        }

        return null;
    }

    private String determineFallbackCode(String requestedCategoryCode) {
        if (requestedCategoryCode == null || requestedCategoryCode.isBlank()) {
            return MISC_GENERAL_CODE;
        }

        String normalized = requestedCategoryCode.toUpperCase(Locale.ROOT);
        if (normalized.equals("TOEIC")
                || normalized.equals("TOEFL")
                || normalized.equals("EN")
                || normalized.startsWith("EN_")) {
            return EN_MISC_CODE;
        }
        if (normalized.equals("JN")
                || normalized.startsWith("JN_")
                || normalized.startsWith("JLPT")) {
            return JN_MISC_CODE;
        }
        return MISC_GENERAL_CODE;
    }

    private void saveFailureLog(User user, GenerateUserCardRequest request, String errorMessage) {
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
            aiGenerationLogDomainService.save(failLog);
        } catch (Exception e) {
            log.error("AI 실패 로그 저장 실패: {}", e.getMessage());
        }
    }
}
