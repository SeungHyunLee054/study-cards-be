package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;

public record CategoryAccuracyResponse(
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long totalCount,
        Long correctCount,
        Double accuracy
) {
    public static CategoryAccuracyResponse from(CategoryAccuracy ca) {
        return new CategoryAccuracyResponse(
                ca.categoryId(),
                ca.categoryCode(),
                ca.categoryName(),
                ca.totalCount(),
                ca.correctCount(),
                ca.accuracy()
        );
    }
}
