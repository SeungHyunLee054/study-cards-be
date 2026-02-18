package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyRecordStatsRepositoryCustom {

    int countDueCards(User user, LocalDate date);

    int countTotalStudiedCards(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countStudiedByCategory(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countStudiedUserCardsByCategory(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countLearningByCategory(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countLearningUserCardsByCategory(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countDueByCategory(User user, LocalDate date);

    List<StudyRecordRepositoryCustom.CategoryCount> countDueUserCardsByCategory(User user, LocalDate date);

    List<StudyRecordRepositoryCustom.DailyActivity> findDailyActivity(User user, LocalDateTime since);

    StudyRecordRepositoryCustom.TotalAndCorrect countTotalAndCorrect(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countMasteredByCategory(User user);

    List<StudyRecordRepositoryCustom.CategoryCount> countMasteredUserCardsByCategory(User user);

    StudyRecordRepositoryCustom.TodayStudyCount countTodayStudy(User user, LocalDate date);

    long countMasteredCardsInCategory(User user, Category category);

    List<StudyRecordRepositoryCustom.CategoryAccuracy> calculateCategoryAccuracy(User user);
}
