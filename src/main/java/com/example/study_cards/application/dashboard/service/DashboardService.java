package com.example.study_cards.application.dashboard.service;

import com.example.study_cards.application.dashboard.dto.response.*;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.*;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TOP_CATEGORIES_LIMIT = 5;

    private final StudyDomainService studyDomainService;
    private final CardDomainService cardDomainService;
    private final CategoryDomainService categoryDomainService;

    public DashboardResponse getDashboard(User user) {
        LocalDate today = LocalDate.now();

        UserSummary userSummary = buildUserSummary(user);
        TodayStudyInfo todayInfo = buildTodayStudyInfo(user, today);
        List<CategoryProgress> categoryProgress = buildCategoryProgress(user, today);
        List<RecentActivitySummary> recentActivity = buildRecentActivity(user);
        StudyRecommendation recommendation = buildRecommendation(user, today, todayInfo, categoryProgress);

        return DashboardResponse.of(userSummary, todayInfo, categoryProgress, recentActivity, recommendation);
    }

    private UserSummary buildUserSummary(User user) {
        int totalStudied = studyDomainService.countTotalStudiedCards(user);
        return UserSummary.from(user, totalStudied);
    }

    private TodayStudyInfo buildTodayStudyInfo(User user, LocalDate today) {
        int dueCards = studyDomainService.countDueCards(user, today);

        long totalCards = cardDomainService.count();
        int totalStudied = studyDomainService.countTotalStudiedCards(user);
        int newCardsAvailable = (int) totalCards - totalStudied;

        var todayStudyCount = studyDomainService.countTodayStudy(user, today);
        int studiedToday = todayStudyCount.totalCount().intValue();
        double todayAccuracy = studiedToday > 0
                ? Math.round((double) todayStudyCount.correctCount() / studiedToday * 1000.0) / 10.0
                : 0.0;

        return TodayStudyInfo.of(dueCards, newCardsAvailable, studiedToday, todayAccuracy);
    }

    private List<CategoryProgress> buildCategoryProgress(User user, LocalDate today) {
        List<Category> allCategories = categoryDomainService.findAll();

        Map<String, Long> totalByCategory = new HashMap<>();
        for (var row : cardDomainService.countAllByCategory()) {
            totalByCategory.put(row.categoryCode(), row.count());
        }

        Map<String, Long> studiedByCategory = new HashMap<>();
        for (var row : studyDomainService.countStudiedByCategory(user)) {
            studiedByCategory.put(row.categoryCode(), row.count());
        }

        Map<String, Long> masteredByCategory = new HashMap<>();
        for (var row : studyDomainService.countMasteredByCategory(user)) {
            masteredByCategory.put(row.categoryCode(), row.count());
        }

        List<CategoryProgress> progressList = new ArrayList<>();
        for (Category category : allCategories) {
            String code = category.getCode();
            long total = totalByCategory.getOrDefault(code, 0L);
            long studied = studiedByCategory.getOrDefault(code, 0L);
            long mastered = masteredByCategory.getOrDefault(code, 0L);

            progressList.add(CategoryProgress.of(code, total, studied, mastered));
        }

        progressList.sort((a, b) -> {
            int studiedCompare = Long.compare(b.studiedCards(), a.studiedCards());
            if (studiedCompare != 0) return studiedCompare;
            return Long.compare(b.totalCards(), a.totalCards());
        });

        return progressList.stream().limit(TOP_CATEGORIES_LIMIT).toList();
    }

    private List<RecentActivitySummary> buildRecentActivity(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<DailyActivity> activities = studyDomainService.findDailyActivity(user, since);

        return activities.stream()
                .map(activity -> RecentActivitySummary.of(
                        activity.date(),
                        activity.totalCount().intValue(),
                        activity.correctCount().intValue()
                ))
                .toList();
    }

    private StudyRecommendation buildRecommendation(User user, LocalDate today, TodayStudyInfo todayInfo, List<CategoryProgress> categoryProgress) {
        if (todayInfo.dueCards() > 0) {
            String category = findCategoryWithMostDueCards(user, today);
            return StudyRecommendation.review(todayInfo.dueCards(), category);
        }

        if (user.getStreak() > 0 && todayInfo.studiedToday() == 0) {
            return StudyRecommendation.streakKeep(user.getStreak());
        }

        if (todayInfo.newCardsAvailable() > 0) {
            String category = findCategoryWithMostNewCards(categoryProgress);
            return StudyRecommendation.newCards(todayInfo.newCardsAvailable(), category);
        }

        return StudyRecommendation.complete();
    }

    private String findCategoryWithMostDueCards(User user, LocalDate today) {
        List<CategoryCount> dueByCategory = studyDomainService.countDueByCategory(user, today);

        return dueByCategory.stream()
                .max(Comparator.comparingLong(CategoryCount::count))
                .map(CategoryCount::categoryCode)
                .orElse(null);
    }

    private String findCategoryWithMostNewCards(List<CategoryProgress> categoryProgress) {
        return categoryProgress.stream()
                .filter(cp -> cp.totalCards() > cp.studiedCards())
                .max(Comparator.comparingLong(cp -> cp.totalCards() - cp.studiedCards()))
                .map(CategoryProgress::categoryCode)
                .orElse(null);
    }
}
