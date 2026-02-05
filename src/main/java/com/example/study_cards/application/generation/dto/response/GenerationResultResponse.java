package com.example.study_cards.application.generation.dto.response;

import java.util.List;

public record GenerationResultResponse(
        List<GeneratedCardResponse> generatedCards,
        int totalGenerated,
        String categoryCode,
        String model
) {
    public static GenerationResultResponse of(List<GeneratedCardResponse> cards, String categoryCode, String model) {
        return new GenerationResultResponse(
                cards,
                cards.size(),
                categoryCode,
                model
        );
    }
}
