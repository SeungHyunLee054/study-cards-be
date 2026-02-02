package com.example.study_cards.application.stats.dto.response;

import java.time.LocalDate;

public record DailyActivity(
        LocalDate date,
        int studied,
        int correct
) {
}
