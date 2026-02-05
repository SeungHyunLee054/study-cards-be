package com.example.study_cards.application.dashboard.dto.response;

import java.util.List;

public record DashboardResponse(
        UserSummary user,
        TodayStudyInfo today,
        List<CategoryProgress> categoryProgress,
        List<RecentActivitySummary> recentActivity,
        StudyRecommendation recommendation
) {

    public static DashboardResponse of(
            UserSummary user,
            TodayStudyInfo today,
            List<CategoryProgress> categoryProgress,
            List<RecentActivitySummary> recentActivity,
            StudyRecommendation recommendation
    ) {
        return new DashboardResponse(user, today, categoryProgress, recentActivity, recommendation);
    }
}
