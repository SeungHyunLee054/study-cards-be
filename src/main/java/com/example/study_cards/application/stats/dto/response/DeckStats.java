package com.example.study_cards.application.stats.dto.response;

public record DeckStats(
        String category,
        int newCount,
        int learningCount,
        int reviewCount,
        double masteryRate
) {
}
