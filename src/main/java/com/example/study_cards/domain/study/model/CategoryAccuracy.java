package com.example.study_cards.domain.study.model;

public record CategoryAccuracy(
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long totalCount,
        Long correctCount,
        Double accuracy
) {
}
