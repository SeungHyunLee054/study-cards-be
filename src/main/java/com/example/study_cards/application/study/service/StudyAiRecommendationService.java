package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.AiRecommendationResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse.RecommendedCard;
import com.example.study_cards.common.util.AiResponseUtils;
import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService.ScoredRecord;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyAiRecommendationService {

    private static final int UNLIMITED_COUNT = Integer.MAX_VALUE;
    private static final LocalDateTime UNLIMITED_RESET_AT = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    private static final int MAX_WEAK_CONCEPTS = 5;
    private static final int MAX_PROMPT_CARD_COUNT = 8;

    private final StudyDomainService studyDomainService;
    private final SubscriptionDomainService subscriptionDomainService;
    private final AiReviewQuotaService aiReviewQuotaService;
    private final AiGenerationService aiGenerationService;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiRecommendationResponse getAiRecommendations(User user, int limit) {
        boolean isAdmin = isAdmin(user);
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);
        if (!isAdmin && !plan.isCanUseAiRecommendations()) {
            throw new AiException(AiErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }

        Subscription subscription = getSubscriptionIfNeeded(user, isAdmin);

        List<RecommendedCard> recommendations = toRecommendedCards(user, limit);
        List<CategoryAccuracy> accuracies = studyDomainService.calculateCategoryAccuracy(user);
        List<AiRecommendationResponse.WeakConcept> ruleWeakConcepts = buildRuleWeakConcepts(accuracies);
        String fallbackStrategy = buildFallbackStrategy(ruleWeakConcepts, recommendations);

        if (recommendations.isEmpty()) {
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(recommendations, ruleWeakConcepts, fallbackStrategy, false, quota);
        }

        if (!tryAcquireQuotaIfNeeded(user, subscription, isAdmin)) {
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(recommendations, ruleWeakConcepts, fallbackStrategy, true, quota);
        }

        String prompt = buildAiPrompt(accuracies, recommendations, ruleWeakConcepts);

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
                    quota
            );
        } catch (Exception e) {
            releaseQuotaIfNeeded(user, subscription, isAdmin);
            saveFailureLog(user, prompt, e.getMessage(), recommendations.size());
            AiRecommendationResponse.Quota quota = resolveQuota(user, subscription, isAdmin);
            return buildFallbackResponse(recommendations, ruleWeakConcepts, fallbackStrategy, true, quota);
        }
    }

    private List<RecommendedCard> toRecommendedCards(User user, int limit) {
        List<ScoredRecord> scoredRecords = studyDomainService.findPrioritizedDueRecords(user, limit);
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
            AiRecommendationResponse.Quota quota
    ) {
        return AiRecommendationResponse.of(
                recommendations,
                weakConcepts,
                strategy,
                false,
                algorithmFallback,
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
            aiGenerationLogRepository.save(AiGenerationLog.builder()
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

    private void saveFailureLog(User user, String prompt, String errorMessage, int recommendationCount) {
        try {
            aiGenerationLogRepository.save(AiGenerationLog.builder()
                    .user(user)
                    .type(AiGenerationType.WEAKNESS_ANALYSIS)
                    .prompt(prompt)
                    .model(aiGenerationService.getDefaultModel())
                    .cardsGenerated(recommendationCount)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build());
        } catch (Exception e) {
            log.warn("[AI] 복습 실패 로그 저장 실패: {}", e.getMessage());
        }
    }

    private record ParsedAiReview(
            List<AiRecommendationResponse.WeakConcept> weakConcepts,
            String reviewStrategy
    ) {
    }
}
