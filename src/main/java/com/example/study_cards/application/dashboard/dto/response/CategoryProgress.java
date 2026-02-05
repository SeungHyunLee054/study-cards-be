package com.example.study_cards.application.dashboard.dto.response;

public record CategoryProgress(
        String categoryCode,
        Long totalCards,
        Long studiedCards,
        Double progressRate,
        Double masteryRate
) {

    public static CategoryProgress of(String categoryCode, long totalCards, long studiedCards, long masteredCards) {
        double progressRate = totalCards > 0
                ? Math.round((double) studiedCards / totalCards * 1000.0) / 10.0
                : 0.0;

        double masteryRate = totalCards > 0
                ? Math.round((double) masteredCards / totalCards * 1000.0) / 10.0
                : 0.0;

        return new CategoryProgress(categoryCode, totalCards, studiedCards, progressRate, masteryRate);
    }
}
