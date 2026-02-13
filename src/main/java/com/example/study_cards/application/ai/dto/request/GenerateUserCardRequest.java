package com.example.study_cards.application.ai.dto.request;

import jakarta.validation.constraints.*;

public record GenerateUserCardRequest(
        @NotBlank(message = "텍스트를 입력하세요")
        @Size(max = 5000, message = "입력 텍스트는 5000자 이하여야 합니다")
        String sourceText,

        @NotBlank(message = "카테고리를 선택하세요")
        String categoryCode,

        @NotNull(message = "생성할 카드 수를 입력하세요")
        @Min(value = 1, message = "최소 1개 이상 생성해야 합니다")
        @Max(value = 20, message = "최대 20개까지 생성 가능합니다")
        Integer count,

        String difficulty
) {
}
