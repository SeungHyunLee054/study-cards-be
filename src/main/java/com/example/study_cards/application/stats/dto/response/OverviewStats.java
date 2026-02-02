package com.example.study_cards.application.stats.dto.response;

public record OverviewStats(
        int dueToday,
        int totalStudied,
        int newCards,
        int streak,
        double accuracyRate
) {
}
