package com.example.study_cards.application.generation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GenerationRequest(
        @NotBlank(message = "카테고리 코드는 필수입니다.")
        String categoryCode,

        @Min(value = 1, message = "최소 1개 이상 생성해야 합니다.")
        @Max(value = 20, message = "한 번에 최대 20개까지 생성 가능합니다.")
        int count,

        @Size(max = 20, message = "원본 카드는 최대 20개까지 선택 가능합니다.")
        List<@NotNull(message = "원본 카드 ID는 null일 수 없습니다.") Long> sourceCardIds
) {
    public GenerationRequest(String categoryCode, int count) {
        this(categoryCode, count, null);
    }

    public GenerationRequest {
        if (count == 0) {
            count = 5;
        }
        if (sourceCardIds != null) {
            sourceCardIds = List.copyOf(sourceCardIds);
        }
    }
}
