package com.example.study_cards.application.dashboard.dto.response;

import java.time.LocalDate;

public record RecentActivitySummary(
        LocalDate date,
        Integer studied,
        Integer correct,
        Double accuracy
) {

    public static RecentActivitySummary of(LocalDate date, int studied, int correct) {
        double accuracy = studied > 0
                ? Math.round((double) correct / studied * 1000.0) / 10.0
                : 0.0;

        return new RecentActivitySummary(date, studied, correct, accuracy);
    }
}
