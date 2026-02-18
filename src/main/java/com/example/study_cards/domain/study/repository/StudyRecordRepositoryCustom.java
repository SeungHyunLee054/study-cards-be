package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface StudyRecordRepositoryCustom {

    List<StudyRecord> findDueRecordsByCategory(User user, LocalDate date, Category category);

    List<StudyRecord> findDueRecordsByCategories(User user, LocalDate date, List<Category> categories);

    List<Long> findStudiedCardIdsByUser(User user);

    List<StudyRecord> findDueUserCardRecordsByCategory(User user, LocalDate date, Category category);

    List<StudyRecord> findDueUserCardRecordsByCategories(User user, LocalDate date, List<Category> categories);

    List<Long> findStudiedUserCardIdsByUser(User user);

    List<StudyRecord> findDueUserCardRecords(User user, LocalDate date);

    List<StudyRecord> findBySessionWithDetails(StudySession session);

    record CategoryCount(Long categoryId, String categoryCode, Long count) {}

    record TodayStudyCount(Long totalCount, Long correctCount) {}

    record DailyActivity(LocalDate date, Long totalCount, Long correctCount) {}

    record TotalAndCorrect(Long totalCount, Long correctCount) {}

    List<StudyRecord> findRecentWrongRecords(User user, int limit);

    List<StudyRecord> findOverdueRecords(User user, LocalDate today, int overdueDays);

    List<StudyRecord> findRepeatedMistakeRecords(User user, int mistakeThreshold);

    record CategoryAccuracy(Long categoryId, String categoryCode, String categoryName,
                            Long totalCount, Long correctCount, Double accuracy) {}
}
