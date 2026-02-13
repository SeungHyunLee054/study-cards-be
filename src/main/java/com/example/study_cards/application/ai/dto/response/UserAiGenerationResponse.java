package com.example.study_cards.application.ai.dto.response;

import com.example.study_cards.domain.usercard.entity.UserCard;

import java.util.List;

public record UserAiGenerationResponse(
        List<AiCardResponse> generatedCards,
        int count,
        int remainingLimit
) {
    public static UserAiGenerationResponse from(List<UserCard> cards, int remainingLimit) {
        return new UserAiGenerationResponse(
                cards.stream().map(AiCardResponse::from).toList(),
                cards.size(),
                remainingLimit
        );
    }
}
