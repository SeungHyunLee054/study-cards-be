package com.example.study_cards.application.stats.service;

import com.example.study_cards.application.stats.dto.response.DailyActivity;
import com.example.study_cards.application.stats.dto.response.DeckStats;
import com.example.study_cards.application.stats.dto.response.OverviewStats;
import com.example.study_cards.application.stats.dto.response.StatsResponse;
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

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StudyDomainService studyDomainService;
    private final CardDomainService cardDomainService;
    private final CategoryDomainService categoryDomainService;

    public StatsResponse getStats(User user) {
        LocalDate today = LocalDate.now();

        OverviewStats overview = calculateOverview(user, today);
        List<DeckStats> deckStats = calculateDeckStats(user, today);
        List<DailyActivity> recentActivity = calculateRecentActivity(user);

        return new StatsResponse(overview, deckStats, recentActivity);
    }

    private OverviewStats calculateOverview(User user, LocalDate today) {
        int dueToday = studyDomainService.countDueCards(user, today);
        int totalStudied = studyDomainService.countTotalStudiedCards(user);
        long totalCards = cardDomainService.count();
        int newCards = (int) totalCards - totalStudied;
        int streak = user.getStreak();
        double accuracyRate = calculateAccuracyRate(user);

        return new OverviewStats(dueToday, totalStudied, newCards, streak, accuracyRate);
    }

    private double calculateAccuracyRate(User user) {
        var result = studyDomainService.countTotalAndCorrect(user);
        if (result.totalCount() == 0) {
            return 0.0;
        }
        return Math.round((double) result.correctCount() / result.totalCount() * 1000.0) / 10.0;
    }

    private List<DeckStats> calculateDeckStats(User user, LocalDate today) {
        List<Category> allCategories = categoryDomainService.findLeafCategories();

        // 카테고리별 전체 카드 수
        Map<String, Long> totalByCategory = new HashMap<>();
        for (var row : cardDomainService.countAllByCategory()) {
            totalByCategory.put(row.categoryCode(), row.count());
        }

        // 카테고리별 학습한 카드 수
        Map<String, Long> studiedByCategory = new HashMap<>();
        for (var row : studyDomainService.countStudiedByCategory(user)) {
            studiedByCategory.put(row.categoryCode(), row.count());
        }

        // 카테고리별 학습 중인 카드 수
        Map<String, Long> learningByCategory = new HashMap<>();
        for (var row : studyDomainService.countLearningByCategory(user)) {
            learningByCategory.put(row.categoryCode(), row.count());
        }

        // 카테고리별 복습 카드 수
        Map<String, Long> reviewByCategory = new HashMap<>();
        for (var row : studyDomainService.countDueByCategory(user, today)) {
            reviewByCategory.put(row.categoryCode(), row.count());
        }

        // 카테고리별 마스터 카드 수
        Map<String, Long> masteredByCategory = new HashMap<>();
        for (var row : studyDomainService.countMasteredByCategory(user)) {
            masteredByCategory.put(row.categoryCode(), row.count());
        }

        List<DeckStats> deckStatsList = new ArrayList<>();
        for (Category category : allCategories) {
            String code = category.getCode();
            long total = totalByCategory.getOrDefault(code, 0L);
            long studied = studiedByCategory.getOrDefault(code, 0L);
            int newCount = (int) (total - studied);
            int learningCount = learningByCategory.getOrDefault(code, 0L).intValue();
            int reviewCount = reviewByCategory.getOrDefault(code, 0L).intValue();
            double masteryRate = calculateMasteryRate(total, masteredByCategory.getOrDefault(code, 0L));

            deckStatsList.add(new DeckStats(code, newCount, learningCount, reviewCount, masteryRate));
        }

        return deckStatsList;
    }

    private double calculateMasteryRate(long total, long mastered) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((double) mastered / total * 1000.0) / 10.0;
    }

    private List<DailyActivity> calculateRecentActivity(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        var results = studyDomainService.findDailyActivity(user, since);

        return results.stream()
                .map(row -> new DailyActivity(
                        row.date(),
                        row.totalCount().intValue(),
                        row.correctCount().intValue()
                ))
                .toList();
    }
}
