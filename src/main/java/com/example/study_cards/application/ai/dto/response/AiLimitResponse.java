package com.example.study_cards.application.ai.dto.response;

public record AiLimitResponse(
        int limit,
        int used,
        int remaining,
        boolean isLifetime
) {
}
