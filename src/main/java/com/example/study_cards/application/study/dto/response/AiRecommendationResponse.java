package com.example.study_cards.application.study.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AiRecommendationResponse(
        List<RecommendationResponse.RecommendedCard> recommendations,
        int totalCount,
        List<WeakConcept> weakConcepts,
        String reviewStrategy,
        boolean aiUsed,
        boolean algorithmFallback,
        FallbackReason fallbackReason,
        Quota quota
) {
    public record WeakConcept(
            String concept,
            String reason
    ) {
    }

    public record Quota(
            int limit,
            int used,
            int remaining,
            LocalDateTime resetAt
    ) {
    }

    public enum FallbackReason {
        NONE,
        NO_DUE_CARDS,
        INSUFFICIENT_STUDY_DATA,
        INSUFFICIENT_RECOMMENDATION_POOL,
        QUOTA_EXCEEDED,
        AI_ERROR
    }

    public static AiRecommendationResponse of(
            List<RecommendationResponse.RecommendedCard> recommendations,
            List<WeakConcept> weakConcepts,
            String reviewStrategy,
            boolean aiUsed,
            boolean algorithmFallback,
            FallbackReason fallbackReason,
            Quota quota
    ) {
        return new AiRecommendationResponse(
                recommendations,
                recommendations.size(),
                weakConcepts,
                reviewStrategy,
                aiUsed,
                algorithmFallback,
                fallbackReason,
                quota
        );
    }
}
