package com.example.study_cards.application.dashboard.dto.response;

public record TodayStudyInfo(
        Integer dueCards,
        Integer newCardsAvailable,
        Integer studiedToday,
        Double todayAccuracy
) {

    public static TodayStudyInfo of(int dueCards, int newCardsAvailable, int studiedToday, double todayAccuracy) {
        return new TodayStudyInfo(dueCards, newCardsAvailable, studiedToday, todayAccuracy);
    }
}
