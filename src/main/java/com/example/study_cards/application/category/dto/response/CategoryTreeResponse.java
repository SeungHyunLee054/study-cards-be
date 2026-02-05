package com.example.study_cards.application.category.dto.response;

import com.example.study_cards.domain.category.entity.Category;

import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String code,
        String name,
        Integer depth,
        Integer displayOrder,
        List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse from(Category category) {
        return new CategoryTreeResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDepth(),
                category.getDisplayOrder(),
                category.getChildren().stream()
                        .map(CategoryTreeResponse::from)
                        .toList()
        );
    }
}
