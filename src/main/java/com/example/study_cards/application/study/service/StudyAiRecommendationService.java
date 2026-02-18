package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.AiRecommendationHistoryResponse;
import com.example.study_cards.application.study.dto.response.AiRecommendationResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse.RecommendedCard;
import com.example.study_cards.common.util.AiResponseUtils;
import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.service.AiGenerationLogDomainService;
import com.example.study_cards.domain.study.model.CategoryAccuracy;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.study.service.StudyRecordDomainService.ScoredRecord;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.infra.redis.service.AiReviewQuotaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyAiRecommendationService {

    private static final int UNLIMITED_COUNT = Integer.MAX_VALUE;
    private static final LocalDateTime UNLIMITED_RESET_AT = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    private static final int DEFAULT_MIN_STUDIED_CARDS = 10;
    private static final int DEFAULT_MIN_RECOMMENDATION_CARDS = 3;
    private static final List<AiGenerationType> AI_RECOMMENDATION_LOG_TYPES =
            List.of(AiGenerationType.RECOMMENDATION, AiGenerationType.WEAKNESS_ANALYSIS);
    private static final int MAX_WEAK_CONCEPTS = 5;
    private static final int MAX_PROMPT_CARD_COUNT = 8;
    private static final String RULE_BASED_MODEL = "rule-based";

    private final StudyRecordDomainService studyRecordDomainService;
    private final SubscriptionDomainService subscriptionDomainService;
    private final AiReviewQuotaService aiReviewQuotaService;
    private final AiGenerationService aiGenerationService;
    private final AiGenerationLogDomainService aiGenerationLogDomainService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.recommendation.min-studied-cards:10}")
    private int minStudiedCards;

    @Value("${app.ai.recommendation.min-recommendation-cards:3}")
    private int minRecommendationCards;

    @Transactional
    public AiRecommendationResponse getAiRecommendations(User user, int limit) {
        boolean isAdmin = isAdmin(user);
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);
        if (!isAdmin && !plan.isCanUseAiRecommendations()) {
            throw new AiException(AiErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }

        Subscription subscription = getSubscriptionIfNeeded(user, isAdmin);

        int normalizedLimit = normalizeRequestLimit(limit);
        int requiredStudyCount = normalizePositiveThreshold(
                "min-studied-cards", minStudiedCards, DEFAULT_MIN_STUDIED_CARDS
        );
        int requiredRecommendationPool = normalizePositiveThreshold(
                "min-recommendation-cards", minRecommendationCards, DEFAULT_MIN_RECOMMENDATION_CARDS
        );

        int recommendationPoolSize = Math.max(normalizedLimit, requiredRecommendationPool);
        List<RecommendedCard> recommendationPool = toRecommendedCards(user, recommendationPoolSize);
        List<RecommendedCard> recommendations = recommendationPool.stream()
                .limit(normalizedLimit)
                .toList();
        List<CategoryAccuracy> accuracies = studyRecordDomainService.calculateCategoryAccuracy(user);
        List<AiRecommendationResponse.WeakConcept> ruleWeakConcepts = buildRuleWeakConcepts(accuracies);
        String fallbackStrategy = buildFallbackStrategy(ruleWeakConcepts, recommendations);

        if (recommendationPool.isEmpty()) {
            saveFallbackLog(user, recommendations.size(), fallbackStrategy, AiRecommendationResponse.FallbackReason.NO_DUE_CARDS);
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(
                    recommendations,
                    ruleWeakConcepts,
                    fallbackStrategy,
                    false,
                    AiRecommendationResponse.FallbackReason.NO_DUE_CARDS,
                    quota
            );
        }

        long totalStudyCount = resolveTotalStudyCount(user);
        if (totalStudyCount < requiredStudyCount) {
            saveFallbackLog(user, recommendations.size(), fallbackStrategy, AiRecommendationResponse.FallbackReason.INSUFFICIENT_STUDY_DATA);
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(
                    recommendations,
                    ruleWeakConcepts,
                    fallbackStrategy,
                    false,
                    AiRecommendationResponse.FallbackReason.INSUFFICIENT_STUDY_DATA,
                    quota
            );
        }

        if (recommendationPool.size() < requiredRecommendationPool) {
            saveFallbackLog(
                    user,
                    recommendations.size(),
                    fallbackStrategy,
                    AiRecommendationResponse.FallbackReason.INSUFFICIENT_RECOMMENDATION_POOL
            );
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(
                    recommendations,
                    ruleWeakConcepts,
                    fallbackStrategy,
                    false,
                    AiRecommendationResponse.FallbackReason.INSUFFICIENT_RECOMMENDATION_POOL,
                    quota
            );
        }

        if (!tryAcquireQuotaIfNeeded(user, subscription, isAdmin)) {
            saveFallbackLog(user, recommendations.size(), fallbackStrategy, AiRecommendationResponse.FallbackReason.QUOTA_EXCEEDED);
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(
                    recommendations,
                    ruleWeakConcepts,
                    fallbackStrategy,
                    true,
                    AiRecommendationResponse.FallbackReason.QUOTA_EXCEEDED,
                    quota
            );
        }

        String prompt = buildAiPrompt(accuracies, recommendationPool, ruleWeakConcepts);

        try {
            String aiResponse = aiGenerationService.generateContent(prompt);
            ParsedAiReview parsed = parseAiReview(aiResponse);
            List<AiRecommendationResponse.WeakConcept> weakConcepts =
                    parsed.weakConcepts().isEmpty() ? ruleWeakConcepts : parsed.weakConcepts();
            String reviewStrategy =
                    parsed.reviewStrategy() == null || parsed.reviewStrategy().isBlank()
                            ? fallbackStrategy
                            : parsed.reviewStrategy();

            saveSuccessLog(user, prompt, aiResponse, recommendations.size());
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return AiRecommendationResponse.of(
                    recommendations,
                    weakConcepts,
                    reviewStrategy,
                    true,
                    false,
                    AiRecommendationResponse.FallbackReason.NONE,
                    quota
            );
        } catch (Exception e) {
            releaseQuotaIfNeeded(user, subscription, isAdmin);
            saveFailureLog(user, prompt, e.getMessage(), recommendations.size(), fallbackStrategy);
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(
                    recommendations,
                    ruleWeakConcepts,
                    fallbackStrategy,
                    true,
                    AiRecommendationResponse.FallbackReason.AI_ERROR,
                    quota
            );
        }
    }

    public Page<AiRecommendationHistoryResponse> getAiRecommendationHistory(User user, Pageable pageable) {
        return aiGenerationLogDomainService.findByUserIdAndTypeInOrderByCreatedAtDesc(
                        user.getId(),
                        AI_RECOMMENDATION_LOG_TYPES,
                        pageable
                )
                .map(AiRecommendationHistoryResponse::from);
    }

    private List<RecommendedCard> toRecommendedCards(User user, int limit) {
        List<ScoredRecord> scoredRecords = studyRecordDomainService.findPrioritizedDueRecords(user, limit);
        return scoredRecords.stream()
                .map(sr -> RecommendedCard.from(sr.record(), sr.score()))
                .toList();
    }

    private List<AiRecommendationResponse.WeakConcept> buildRuleWeakConcepts(List<CategoryAccuracy> accuracies) {
        if (accuracies == null || accuracies.isEmpty()) {
            return List.of();
        }

        return accuracies.stream()
                .filter(ca -> ca.totalCount() != null && ca.totalCount() > 0)
                .sorted(Comparator
                        .comparingDouble((CategoryAccuracy ca) -> ca.accuracy() == null ? 100.0 : ca.accuracy())
                        .thenComparingLong(ca -> -(ca.totalCount() == null ? 0L : ca.totalCount())))
                .limit(3)
                .map(ca -> {
                    double accuracy = ca.accuracy() == null ? 0.0 : ca.accuracy();
                    long correctCount = ca.correctCount() == null ? 0L : ca.correctCount();
                    long totalCount = ca.totalCount() == null ? 0L : ca.totalCount();
                    String reason = String.format("정답률 %.1f%% (%d/%d)", accuracy, correctCount, totalCount);
                    return new AiRecommendationResponse.WeakConcept(ca.categoryName(), reason);
                })
                .toList();
    }

    private String buildFallbackStrategy(
            List<AiRecommendationResponse.WeakConcept> weakConcepts,
            List<RecommendedCard> recommendations
    ) {
        if (recommendations.isEmpty()) {
            return "오늘은 오답 카드 위주로 가볍게 복습한 뒤 다음 학습을 준비하세요.";
        }

        int topCount = Math.min(5, recommendations.size());
        if (weakConcepts.isEmpty()) {
            return "추천 카드 상위 " + topCount + "개를 먼저 풀고, 틀린 카드를 다시 복습하세요.";
        }

        String firstConcept = weakConcepts.get(0).concept();
        return firstConcept + " 영역을 먼저 복습한 뒤, 추천 카드 상위 " + topCount + "개를 학습하세요.";
    }

    private String buildAiPrompt(
            List<CategoryAccuracy> accuracies,
            List<RecommendedCard> recommendations,
            List<AiRecommendationResponse.WeakConcept> weakConcepts
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("학습 데이터를 기반으로 개인 맞춤 복습 전략을 JSON으로 작성하세요.\n");
        prompt.append("반드시 JSON만 출력하고, 설명 문장은 출력하지 마세요.\n");
        prompt.append("출력 형식:\n");
        prompt.append("{\"weakConcepts\":[{\"concept\":\"개념\",\"reason\":\"근거\"}],\"reviewStrategy\":\"전략\"}\n\n");

        if (accuracies != null && !accuracies.isEmpty()) {
            prompt.append("[카테고리 정답률]\n");
            for (CategoryAccuracy ca : accuracies) {
                prompt.append("- ").append(ca.categoryName())
                        .append(": ").append(ca.accuracy())
                        .append("% (").append(ca.correctCount()).append("/")
                        .append(ca.totalCount()).append(")\n");
            }
        }

        if (!weakConcepts.isEmpty()) {
            prompt.append("\n[규칙 기반 취약 영역]\n");
            for (AiRecommendationResponse.WeakConcept weakConcept : weakConcepts) {
                prompt.append("- ").append(weakConcept.concept())
                        .append(": ").append(weakConcept.reason()).append("\n");
            }
        }

        prompt.append("\n[오늘 복습 추천 카드]\n");
        recommendations.stream()
                .limit(MAX_PROMPT_CARD_COUNT)
                .forEach(card -> {
                    String question = abbreviate(card.question(), 60);
                    prompt.append("- ").append(question)
                            .append(" | score=").append(card.priorityScore())
                            .append(" | lastCorrect=").append(card.lastCorrect())
                            .append("\n");
                });

        prompt.append("\n요구사항:\n");
        prompt.append("1) weakConcepts는 최대 ").append(MAX_WEAK_CONCEPTS).append("개\n");
        prompt.append("2) reviewStrategy는 오늘 바로 실행 가능한 2~3문장\n");
        prompt.append("3) 한국어로 작성\n");

        return prompt.toString();
    }

    private ParsedAiReview parseAiReview(String aiResponse) {
        try {
            String cleaned = AiResponseUtils.extractJsonPayload(aiResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            List<AiRecommendationResponse.WeakConcept> weakConcepts = new ArrayList<>();
            JsonNode weakNode = root.get("weakConcepts");
            if (weakNode != null && weakNode.isArray()) {
                for (JsonNode item : weakNode) {
                    String concept = item.path("concept").asText("");
                    String reason = item.path("reason").asText("");
                    if (!concept.isBlank()) {
                        weakConcepts.add(new AiRecommendationResponse.WeakConcept(concept, reason));
                    }
                    if (weakConcepts.size() >= MAX_WEAK_CONCEPTS) {
                        break;
                    }
                }
            }

            String reviewStrategy = root.path("reviewStrategy").asText("");
            return new ParsedAiReview(weakConcepts, reviewStrategy);
        } catch (Exception e) {
            throw new AiException(AiErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private AiRecommendationResponse buildFallbackResponse(
            List<RecommendedCard> recommendations,
            List<AiRecommendationResponse.WeakConcept> weakConcepts,
            String strategy,
            boolean algorithmFallback,
            AiRecommendationResponse.FallbackReason fallbackReason,
            AiRecommendationResponse.Quota quota
    ) {
        return AiRecommendationResponse.of(
                recommendations,
                weakConcepts,
                strategy,
                false,
                algorithmFallback,
                fallbackReason,
                quota
        );
    }

    private AiRecommendationResponse.Quota toResponseQuota(AiReviewQuotaService.ReviewQuota quota) {
        return new AiRecommendationResponse.Quota(
                quota.limit(),
                quota.used(),
                quota.remaining(),
                quota.resetAt()
        );
    }

    private boolean isAdmin(User user) {
        return user.hasRole(Role.ROLE_ADMIN);
    }

    private Subscription getSubscriptionIfNeeded(User user, boolean isAdmin) {
        if (isAdmin) {
            return null;
        }
        return subscriptionDomainService.getSubscription(user.getId());
    }

    private boolean tryAcquireQuotaIfNeeded(User user, Subscription subscription, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        return aiReviewQuotaService.tryAcquireSlot(user.getId(), subscription);
    }

    private void releaseQuotaIfNeeded(User user, Subscription subscription, boolean isAdmin) {
        if (!isAdmin) {
            aiReviewQuotaService.releaseSlot(user.getId(), subscription);
        }
    }

    private AiRecommendationResponse.Quota resolveQuota(User user, Subscription subscription, boolean isAdmin) {
        if (isAdmin) {
            return unlimitedQuota();
        }
        return toResponseQuota(aiReviewQuotaService.getQuota(user.getId(), subscription));
    }

    private AiRecommendationResponse.Quota unlimitedQuota() {
        return new AiRecommendationResponse.Quota(
                UNLIMITED_COUNT,
                0,
                UNLIMITED_COUNT,
                UNLIMITED_RESET_AT
        );
    }

    private void saveSuccessLog(User user, String prompt, String response, int recommendationCount) {
        try {
            aiGenerationLogDomainService.save(AiGenerationLog.builder()
                    .user(user)
                    .type(AiGenerationType.RECOMMENDATION)
                    .prompt(prompt)
                    .response(response)
                    .model(aiGenerationService.getDefaultModel())
                    .cardsGenerated(recommendationCount)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.warn("[AI] 복습 로그 저장 실패: {}", e.getMessage());
        }
    }

    private void saveFailureLog(
            User user,
            String prompt,
            String errorMessage,
            int recommendationCount,
            String fallbackStrategy
    ) {
        try {
            aiGenerationLogDomainService.save(AiGenerationLog.builder()
                    .user(user)
                    .type(AiGenerationType.RECOMMENDATION)
                    .prompt(prompt)
                    .model(aiGenerationService.getDefaultModel())
                    .cardsGenerated(recommendationCount)
                    .success(false)
                    .response(buildFallbackResponsePayload(AiRecommendationResponse.FallbackReason.AI_ERROR, fallbackStrategy))
                    .errorMessage(AiRecommendationResponse.FallbackReason.AI_ERROR + ": " + errorMessage)
                    .build());
        } catch (Exception e) {
            log.warn("[AI] 복습 실패 로그 저장 실패: {}", e.getMessage());
        }
    }

    private void saveFallbackLog(
            User user,
            int recommendationCount,
            String fallbackStrategy,
            AiRecommendationResponse.FallbackReason fallbackReason
    ) {
        try {
            aiGenerationLogDomainService.save(AiGenerationLog.builder()
                    .user(user)
                    .type(AiGenerationType.RECOMMENDATION)
                    .model(RULE_BASED_MODEL)
                    .cardsGenerated(recommendationCount)
                    .success(false)
                    .response(buildFallbackResponsePayload(fallbackReason, fallbackStrategy))
                    .errorMessage("fallback:" + fallbackReason)
                    .build());
        } catch (Exception e) {
            log.warn("[AI] 복습 폴백 로그 저장 실패: {}", e.getMessage());
        }
    }

    private String buildFallbackResponsePayload(
            AiRecommendationResponse.FallbackReason fallbackReason,
            String reviewStrategy
    ) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "fallbackReason", fallbackReason.name(),
                    "reviewStrategy", reviewStrategy
            ));
        } catch (Exception e) {
            return "{\"fallbackReason\":\"" + fallbackReason.name() + "\",\"reviewStrategy\":\"" + reviewStrategy + "\"}";
        }
    }

    private long resolveTotalStudyCount(User user) {
        var totalAndCorrect = studyRecordDomainService.countTotalAndCorrect(user);
        if (totalAndCorrect == null || totalAndCorrect.totalCount() == null) {
            return 0L;
        }
        return totalAndCorrect.totalCount();
    }

    private int normalizeRequestLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return limit;
    }

    private int normalizePositiveThreshold(String propertyName, int configured, int defaultValue) {
        if (configured < 1) {
            log.warn("잘못된 AI 추천 최소 조건 설정({}={}), {}으로 보정합니다.",
                    propertyName, configured, defaultValue);
            return defaultValue;
        }
        return configured;
    }

    private record ParsedAiReview(
            List<AiRecommendationResponse.WeakConcept> weakConcepts,
            String reviewStrategy
    ) {
    }
}
