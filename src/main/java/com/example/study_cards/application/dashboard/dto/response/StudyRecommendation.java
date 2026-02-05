package com.example.study_cards.application.dashboard.dto.response;

public record StudyRecommendation(
        String message,
        String recommendedCategory,
        Integer cardsToStudy,
        RecommendationType type
) {

    public enum RecommendationType {
        REVIEW,
        STREAK_KEEP,
        NEW,
        COMPLETE
    }

    public static StudyRecommendation review(int dueCards, String category) {
        return new StudyRecommendation(
                dueCards + "개의 복습 카드가 있어요!",
                category,
                dueCards,
                RecommendationType.REVIEW
        );
    }

    public static StudyRecommendation streakKeep(int streak) {
        return new StudyRecommendation(
                streak + "일 연속 학습 중! 오늘도 학습해서 스트릭을 유지하세요!",
                null,
                null,
                RecommendationType.STREAK_KEEP
        );
    }

    public static StudyRecommendation newCards(int newCards, String category) {
        return new StudyRecommendation(
                "새로운 카드를 학습해보세요!",
                category,
                newCards,
                RecommendationType.NEW
        );
    }

    public static StudyRecommendation complete() {
        return new StudyRecommendation(
                "오늘 학습을 모두 완료했어요!",
                null,
                null,
                RecommendationType.COMPLETE
        );
    }
}
