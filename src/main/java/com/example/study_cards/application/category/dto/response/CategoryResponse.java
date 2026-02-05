package com.example.study_cards.application.category.dto.response;

import com.example.study_cards.domain.category.entity.Category;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        Long parentId,
        String parentCode
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getCode() : null
        );
    }
}
