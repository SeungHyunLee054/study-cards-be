package com.example.study_cards.application.generation.dto.response;

import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;

import java.time.LocalDateTime;

public record GeneratedCardResponse(
        Long id,
        String model,
        String sourceWord,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        CategoryResponse category,
        GenerationStatus status,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {
    public static GeneratedCardResponse from(GeneratedCard generatedCard) {
        return new GeneratedCardResponse(
                generatedCard.getId(),
                generatedCard.getModel(),
                generatedCard.getSourceWord(),
                generatedCard.getQuestion(),
                generatedCard.getQuestionSub(),
                generatedCard.getAnswer(),
                generatedCard.getAnswerSub(),
                CategoryResponse.from(generatedCard.getCategory()),
                generatedCard.getStatus(),
                generatedCard.getApprovedAt(),
                generatedCard.getCreatedAt()
        );
    }
}
