package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyRecordRepositoryCustom {

    int countDueCards(User user, LocalDate date);

    int countTotalStudiedCards(User user);

    List<CategoryCount> countStudiedByCategory(User user);

    List<CategoryCount> countLearningByCategory(User user);

    List<CategoryCount> countDueByCategory(User user, LocalDate date);

    List<DailyActivity> findDailyActivity(User user, LocalDateTime since);

    List<StudyRecord> findDueRecordsByCategory(User user, LocalDate date, Category category);

    List<Long> findStudiedCardIdsByUser(User user);

    TotalAndCorrect countTotalAndCorrect(User user);

    List<CategoryCount> countMasteredByCategory(User user);

    List<StudyRecord> findDueUserCardRecordsByCategory(User user, LocalDate date, Category category);

    List<Long> findStudiedUserCardIdsByUser(User user);

    List<StudyRecord> findDueUserCardRecords(User user, LocalDate date);

    List<StudyRecord> findBySessionWithDetails(StudySession session);

    TodayStudyCount countTodayStudy(User user, LocalDate date);

    record CategoryCount(Long categoryId, String categoryCode, Long count) {}

    record TodayStudyCount(Long totalCount, Long correctCount) {}

    record DailyActivity(LocalDate date, Long totalCount, Long correctCount) {}

    record TotalAndCorrect(Long totalCount, Long correctCount) {}

    long countMasteredCardsInCategory(User user, Category category);
}
