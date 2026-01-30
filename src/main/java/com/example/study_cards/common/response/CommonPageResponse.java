package com.example.study_cards.common.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record CommonPageResponse<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        boolean isLast,
        boolean isFirst
) {

    public static <T> CommonPageResponse<T> from(Page<T> page) {
        return CommonPageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isLast(page.isLast())
                .isFirst(page.isFirst())
                .build();
    }
}
