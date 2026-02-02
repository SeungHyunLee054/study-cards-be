package com.example.study_cards.application.stats.dto.response;

import java.util.List;

public record StatsResponse(
        OverviewStats overview,
        List<DeckStats> deckStats,
        List<DailyActivity> recentActivity
) {
}
