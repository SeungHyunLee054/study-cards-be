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

    public static AiRecommendationResponse of(
            List<RecommendationResponse.RecommendedCard> recommendations,
            List<WeakConcept> weakConcepts,
            String reviewStrategy,
            boolean aiUsed,
            boolean algorithmFallback,
            Quota quota
    ) {
        return new AiRecommendationResponse(
                recommendations,
                recommendations.size(),
                weakConcepts,
                reviewStrategy,
                aiUsed,
                algorithmFallback,
                quota
        );
    }
}
