package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;

import java.time.LocalDateTime;

public record AiRecommendationHistoryResponse(
        Long id,
        AiGenerationType type,
        String model,
        Integer cardsGenerated,
        boolean success,
        String errorMessage,
        String prompt,
        String response,
        LocalDateTime createdAt
) {

    public static AiRecommendationHistoryResponse from(AiGenerationLog log) {
        return new AiRecommendationHistoryResponse(
                log.getId(),
                log.getType(),
                log.getModel(),
                log.getCardsGenerated(),
                Boolean.TRUE.equals(log.getSuccess()),
                log.getErrorMessage(),
                log.getPrompt(),
                log.getResponse(),
                log.getCreatedAt()
        );
    }
}
