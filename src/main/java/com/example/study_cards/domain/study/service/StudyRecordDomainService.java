package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.model.CategoryAccuracy;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryCount;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.DailyActivity;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TotalAndCorrect;

@RequiredArgsConstructor
@Service
public class StudyRecordDomainService {

    private final StudyRecordRepository studyRecordRepository;

    private static final int REPEATED_MISTAKE_THRESHOLD = 3;
    private static final int OVERDUE_DAYS = 7;
    private static final int SCORE_REPEATED_MISTAKE = 1000;
    private static final int SCORE_OVERDUE = 500;
    private static final int SCORE_RECENT_WRONG = 300;
    private static final int SCORE_EF_FACTOR_MAX = 120;
    private static final int RECENT_WRONG_LIMIT = 20;

    public List<StudyRecord> findDueUserCardRecordsByCategories(User user, LocalDate date, List<Category> categories) {
        return studyRecordRepository.findDueUserCardRecordsByCategories(user, date, categories);
    }

    public List<StudyRecord> findDueUserCardRecords(User user, LocalDate date) {
        return studyRecordRepository.findDueUserCardRecords(user, date);
    }

    public List<StudyRecord> findDueRecordsByCategories(User user, LocalDate date, List<Category> categories) {
        return studyRecordRepository.findDueRecordsByCategories(user, date, categories);
    }

    public List<StudyRecord> findDueRecords(User user, LocalDate date) {
        return studyRecordRepository.findDueRecordsByCategory(user, date, null);
    }

    public List<Long> findStudiedUserCardIdsByUser(User user) {
        return studyRecordRepository.findStudiedUserCardIdsByUser(user);
    }

    public List<Long> findStudiedCardIdsByUser(User user) {
        return studyRecordRepository.findStudiedCardIdsByUser(user);
    }

    public StudyRecord processAnswer(User user, Card card, StudySession session, Boolean isCorrect) {
        Optional<StudyRecord> existingRecord = studyRecordRepository.findByUserAndCard(user, card);
        return existingRecord.map(studyRecord -> updateExistingRecord(studyRecord, isCorrect, session))
                .orElseGet(() -> createAndSaveCardRecord(user, card, session, isCorrect));
    }

    public StudyRecord processUserCardAnswer(User user, UserCard userCard, StudySession session, Boolean isCorrect) {
        Optional<StudyRecord> existingRecord = studyRecordRepository.findByUserAndUserCard(user, userCard);
        return existingRecord.map(studyRecord -> updateExistingRecord(studyRecord, isCorrect, session))
                .orElseGet(() -> createAndSaveUserCardRecord(user, userCard, session, isCorrect));
    }

    private int calculateInterval(int repetitionCount, double efFactor, boolean isCorrect) {
        if (!isCorrect) {
            return SM2Constants.FIRST_INTERVAL;
        }

        if (repetitionCount == 1) {
            return SM2Constants.FIRST_INTERVAL;
        } else if (repetitionCount == 2) {
            return SM2Constants.SECOND_INTERVAL;
        } else {
            int prevInterval = (int) Math.round(SM2Constants.SECOND_INTERVAL * Math.pow(efFactor, repetitionCount - 3));
            return (int) Math.round(prevInterval * efFactor);
        }
    }

    public int countDueCards(User user, LocalDate date) {
        return studyRecordRepository.countDueCards(user, date);
    }

    public int countTotalStudiedCards(User user) {
        return studyRecordRepository.countTotalStudiedCards(user);
    }

    public int countTotalStudiedUserCards(User user) {
        return (int) studyRecordRepository.findStudiedUserCardIdsByUser(user).stream()
                .distinct()
                .count();
    }

    public TotalAndCorrect countTotalAndCorrect(User user) {
        return studyRecordRepository.countTotalAndCorrect(user);
    }

    private List<CategoryCount> countStudiedByCategory(User user) {
        return studyRecordRepository.countStudiedByCategory(user);
    }

    public List<CategoryCount> countStudiedByCategoryWithUserCards(User user) {
        return mergeCategoryCounts(
                countStudiedByCategory(user),
                studyRecordRepository.countStudiedUserCardsByCategory(user)
        );
    }

    private List<CategoryCount> countLearningByCategory(User user) {
        return studyRecordRepository.countLearningByCategory(user);
    }

    public List<CategoryCount> countLearningByCategoryWithUserCards(User user) {
        return mergeCategoryCounts(
                countLearningByCategory(user),
                studyRecordRepository.countLearningUserCardsByCategory(user)
        );
    }

    private List<CategoryCount> countDueByCategory(User user, LocalDate date) {
        return studyRecordRepository.countDueByCategory(user, date);
    }

    public List<CategoryCount> countDueByCategoryWithUserCards(User user, LocalDate date) {
        return mergeCategoryCounts(
                countDueByCategory(user, date),
                studyRecordRepository.countDueUserCardsByCategory(user, date)
        );
    }

    private List<CategoryCount> countMasteredByCategory(User user) {
        return studyRecordRepository.countMasteredByCategory(user);
    }

    public List<CategoryCount> countMasteredByCategoryWithUserCards(User user) {
        return mergeCategoryCounts(
                countMasteredByCategory(user),
                studyRecordRepository.countMasteredUserCardsByCategory(user)
        );
    }

    public List<DailyActivity> findDailyActivity(User user, LocalDateTime since) {
        return studyRecordRepository.findDailyActivity(user, since);
    }

    public List<StudyRecord> findRecordsBySession(StudySession session) {
        return studyRecordRepository.findBySessionWithDetails(session);
    }

    public TotalAndCorrect countTodayStudy(User user, LocalDate date) {
        var result = studyRecordRepository.countTodayStudy(user, date);
        return new TotalAndCorrect(result.totalCount(), result.correctCount());
    }

    public long countMasteredCardsInCategory(User user, Category category) {
        return studyRecordRepository.countMasteredCardsInCategory(user, category);
    }

    public List<CategoryAccuracy> calculateCategoryAccuracy(User user) {
        return studyRecordRepository.calculateCategoryAccuracy(user).stream()
                .map(ca -> new CategoryAccuracy(
                        ca.categoryId(),
                        ca.categoryCode(),
                        ca.categoryName(),
                        ca.totalCount(),
                        ca.correctCount(),
                        ca.accuracy()
                ))
                .toList();
    }

    private int calculatePriorityScore(
            StudyRecord record,
            Set<Long> repeatedMistakeCardIds,
            Set<Long> overdueCardIds,
            Set<Long> recentWrongCardIds
    ) {
        int score = 0;
        Long cardId = extractCardId(record);

        if (cardId == null) {
            return 0;
        }

        if (repeatedMistakeCardIds.contains(cardId)) {
            score += SCORE_REPEATED_MISTAKE;
        }

        if (overdueCardIds.contains(cardId)) {
            score += SCORE_OVERDUE;
        }

        if (recentWrongCardIds.contains(cardId)) {
            score += SCORE_RECENT_WRONG;
        }

        double efFactor = record.getEfFactor();
        int efScore = (int) Math.round(SCORE_EF_FACTOR_MAX
                * (1.0 - (efFactor - SM2Constants.MIN_EF_FACTOR) / (2.5 - SM2Constants.MIN_EF_FACTOR)));
        score += Math.max(0, Math.min(SCORE_EF_FACTOR_MAX, efScore));

        return score;
    }

    public record ScoredRecord(StudyRecord record, int score) {}

    public List<ScoredRecord> findPrioritizedDueRecords(User user, int limit) {
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords = findDueRecords(user, today);
        List<StudyRecord> dueUserCardRecords = findDueUserCardRecords(user, today);

        Set<Long> repeatedMistakeIds = extractCardIds(
                studyRecordRepository.findRepeatedMistakeRecords(user, REPEATED_MISTAKE_THRESHOLD));
        Set<Long> overdueIds = extractCardIds(
                studyRecordRepository.findOverdueRecords(user, today, OVERDUE_DAYS));
        Set<Long> recentWrongIds = extractCardIds(
                studyRecordRepository.findRecentWrongRecords(user, RECENT_WRONG_LIMIT));

        List<StudyRecord> allDue = new java.util.ArrayList<>(dueRecords);
        allDue.addAll(dueUserCardRecords);

        return allDue.stream()
                .map(r -> new ScoredRecord(r,
                        calculatePriorityScore(r, repeatedMistakeIds, overdueIds, recentWrongIds)))
                .sorted(Comparator.comparingInt(ScoredRecord::score).reversed())
                .limit(limit)
                .toList();
    }

    private StudyRecord updateExistingRecord(StudyRecord record, Boolean isCorrect, StudySession session) {
        record.updateEfFactor(isCorrect);
        int newInterval = calculateInterval(record.getRepetitionCount(), record.getEfFactor(), isCorrect);
        LocalDate newNextReviewDate = LocalDate.now().plusDays(newInterval);
        record.updateForReview(isCorrect, newNextReviewDate, newInterval);
        incrementSessionProgress(session, isCorrect);
        return record;
    }

    private StudyRecord createAndSaveCardRecord(User user, Card card, StudySession session, Boolean isCorrect) {
        double newEfFactor = SM2Constants.calculateNewEfFactor(card.getEfFactor(), isCorrect);
        int initialInterval = SM2Constants.FIRST_INTERVAL;
        LocalDate nextReviewDate = LocalDate.now().plusDays(initialInterval);

        StudyRecord newRecord = StudyRecord.builder()
                .user(user)
                .card(card)
                .session(session)
                .isCorrect(isCorrect)
                .nextReviewDate(nextReviewDate)
                .interval(initialInterval)
                .efFactor(newEfFactor)
                .build();

        incrementSessionProgress(session, isCorrect);
        return studyRecordRepository.save(newRecord);
    }

    private StudyRecord createAndSaveUserCardRecord(User user, UserCard userCard, StudySession session, Boolean isCorrect) {
        double newEfFactor = SM2Constants.calculateNewEfFactor(userCard.getEfFactor(), isCorrect);
        int initialInterval = SM2Constants.FIRST_INTERVAL;
        LocalDate nextReviewDate = LocalDate.now().plusDays(initialInterval);

        StudyRecord newRecord = StudyRecord.builder()
                .user(user)
                .userCard(userCard)
                .session(session)
                .isCorrect(isCorrect)
                .nextReviewDate(nextReviewDate)
                .interval(initialInterval)
                .efFactor(newEfFactor)
                .build();

        incrementSessionProgress(session, isCorrect);
        return studyRecordRepository.save(newRecord);
    }

    private void incrementSessionProgress(StudySession session, Boolean isCorrect) {
        if (session != null) {
            session.incrementTotalCards();
            if (isCorrect) {
                session.incrementCorrectCount();
            }
        }
    }

    private Set<Long> extractCardIds(List<StudyRecord> records) {
        return records.stream()
                .map(this::extractCardId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Long extractCardId(StudyRecord record) {
        if (record.isForPublicCard()) {
            return record.getCard() != null ? record.getCard().getId() : null;
        }
        if (record.isForUserCard()) {
            return record.getUserCard() != null ? record.getUserCard().getId() : null;
        }
        return null;
    }

    private List<CategoryCount> mergeCategoryCounts(List<CategoryCount> publicCounts, List<CategoryCount> userCardCounts) {
        Map<Long, CategoryCountAccumulator> merged = new LinkedHashMap<>();
        addCategoryCounts(merged, publicCounts);
        addCategoryCounts(merged, userCardCounts);

        return merged.values().stream()
                .map(CategoryCountAccumulator::toCategoryCount)
                .toList();
    }

    private void addCategoryCounts(Map<Long, CategoryCountAccumulator> merged, List<CategoryCount> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (CategoryCount row : source) {
            if (row == null || row.categoryId() == null) {
                continue;
            }
            merged.compute(row.categoryId(), (key, existing) -> {
                if (existing == null) {
                    return new CategoryCountAccumulator(row.categoryId(), row.categoryCode(), row.count());
                }
                existing.add(row.count());
                return existing;
            });
        }
    }

    private static class CategoryCountAccumulator {
        private final Long categoryId;
        private final String categoryCode;
        private long count;

        private CategoryCountAccumulator(Long categoryId, String categoryCode, Long count) {
            this.categoryId = categoryId;
            this.categoryCode = categoryCode;
            this.count = count != null ? count : 0L;
        }

        private void add(Long value) {
            this.count += value != null ? value : 0L;
        }

        private CategoryCount toCategoryCount() {
            return new CategoryCount(categoryId, categoryCode, count);
        }
    }
}
