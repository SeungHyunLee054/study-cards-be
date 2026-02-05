package com.example.study_cards.application.category.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(
        @NotBlank(message = "카테고리 코드는 필수입니다.")
        String code,

        @NotBlank(message = "카테고리 이름은 필수입니다.")
        String name,

        Integer displayOrder
) {
}
