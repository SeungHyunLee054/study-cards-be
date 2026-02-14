package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryCount;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.DailyActivity;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TotalAndCorrect;

@RequiredArgsConstructor
@Service
public class StudyDomainService {

    private static final int DEFAULT_STUDY_LIMIT = 20;

    private final StudySessionRepository studySessionRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;

    public StudySession createSession(User user) {
        StudySession session = StudySession.builder()
                .user(user)
                .build();
        return studySessionRepository.save(session);
    }

    public StudySession findSessionById(Long sessionId) {
        return studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.SESSION_NOT_FOUND));
    }

    public void endSession(Long sessionId) {
        StudySession session = findSessionById(sessionId);
        session.endSession();
    }

    public record StudyCardItem(Long id, Card card, UserCard userCard) {
        public boolean isPublicCard() { return card != null; }
        public boolean isUserCard() { return userCard != null; }

        public static StudyCardItem ofCard(Card card) { return new StudyCardItem(card.getId(), card, null); }
        public static StudyCardItem ofUserCard(UserCard uc) { return new StudyCardItem(uc.getId(), null, uc); }
    }

    public List<StudyCardItem> findTodayAllStudyCards(User user, Category category, int limit) {
        return findTodayAllStudyCards(user, category != null ? List.of(category) : null, limit);
    }

    public List<StudyCardItem> findTodayAllStudyCards(User user, List<Category> categories, int limit) {
        LocalDate today = LocalDate.now();

        boolean hasCategoryScope = categories != null && !categories.isEmpty();

        List<StudyRecord> dueUserCardRecords = hasCategoryScope
                ? studyRecordRepository.findDueUserCardRecordsByCategories(user, today, categories)
                : studyRecordRepository.findDueUserCardRecords(user, today);
        List<StudyRecord> dueCardRecords = hasCategoryScope
                ? studyRecordRepository.findDueRecordsByCategories(user, today, categories)
                : studyRecordRepository.findDueRecordsByCategory(user, today, null);

        List<StudyCardItem> result = new java.util.ArrayList<>();

        dueUserCardRecords.stream()
                .map(StudyRecord::getUserCard)
                .map(StudyCardItem::ofUserCard)
                .forEach(result::add);

        dueCardRecords.stream()
                .map(StudyRecord::getCard)
                .map(StudyCardItem::ofCard)
                .forEach(result::add);

        if (result.size() >= limit) {
            return result.subList(0, limit);
        }

        List<Long> studiedUserCardIds = studyRecordRepository.findStudiedUserCardIdsByUser(user);
        List<UserCard> newUserCards = (hasCategoryScope
                ? userCardRepository.findByUserAndCategoriesOrderByEfFactorAsc(user, categories)
                : userCardRepository.findByUserOrderByEfFactorAsc(user))
                .stream()
                .filter(uc -> !studiedUserCardIds.contains(uc.getId()))
                .limit(limit - result.size())
                .toList();
        newUserCards.stream().map(StudyCardItem::ofUserCard).forEach(result::add);

        if (result.size() >= limit) {
            return result.subList(0, limit);
        }

        List<Long> studiedCardIds = studyRecordRepository.findStudiedCardIdsByUser(user);
        List<Card> newCards = (hasCategoryScope
                ? cardRepository.findByCategoriesOrderByEfFactorAsc(categories)
                : cardRepository.findAllByOrderByEfFactorAsc())
                .stream()
                .filter(card -> !studiedCardIds.contains(card.getId()))
                .limit(limit - result.size())
                .toList();
        newCards.stream().map(StudyCardItem::ofCard).forEach(result::add);

        return result.size() > limit ? result.subList(0, limit) : result;
    }

    public StudyRecord processAnswer(User user, Card card, StudySession session, Boolean isCorrect) {
        Optional<StudyRecord> existingRecord = studyRecordRepository.findByUserAndCard(user, card);

        if (existingRecord.isPresent()) {
            StudyRecord record = existingRecord.get();
            record.updateEfFactor(isCorrect);
            int newInterval = calculateInterval(record.getRepetitionCount(), record.getEfFactor(), isCorrect);
            LocalDate newNextReviewDate = LocalDate.now().plusDays(newInterval);
            record.updateForReview(isCorrect, newNextReviewDate, newInterval);

            if (session != null) {
                session.incrementTotalCards();
                if (isCorrect) {
                    session.incrementCorrectCount();
                }
            }

            return record;
        } else {
            double initialEfFactor = card.getEfFactor();
            double newEfFactor = SM2Constants.calculateNewEfFactor(initialEfFactor, isCorrect);

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

            if (session != null) {
                session.incrementTotalCards();
                if (isCorrect) {
                    session.incrementCorrectCount();
                }
            }

            return studyRecordRepository.save(newRecord);
        }
    }

    public int calculateInterval(int repetitionCount, double efFactor, boolean isCorrect) {
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

    public StudyRecord processUserCardAnswer(User user, UserCard userCard, StudySession session, Boolean isCorrect) {
        Optional<StudyRecord> existingRecord = studyRecordRepository.findByUserAndUserCard(user, userCard);

        if (existingRecord.isPresent()) {
            StudyRecord record = existingRecord.get();
            record.updateEfFactor(isCorrect);
            int newInterval = calculateInterval(record.getRepetitionCount(), record.getEfFactor(), isCorrect);
            LocalDate newNextReviewDate = LocalDate.now().plusDays(newInterval);
            record.updateForReview(isCorrect, newNextReviewDate, newInterval);

            if (session != null) {
                session.incrementTotalCards();
                if (isCorrect) {
                    session.incrementCorrectCount();
                }
            }

            return record;
        } else {
            double initialEfFactor = userCard.getEfFactor();
            double newEfFactor = SM2Constants.calculateNewEfFactor(initialEfFactor, isCorrect);

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

            if (session != null) {
                session.incrementTotalCards();
                if (isCorrect) {
                    session.incrementCorrectCount();
                }
            }

            return studyRecordRepository.save(newRecord);
        }
    }

    public int countDueCards(User user, LocalDate date) {
        return studyRecordRepository.countDueCards(user, date);
    }

    public int countTotalStudiedCards(User user) {
        return studyRecordRepository.countTotalStudiedCards(user);
    }

    public TotalAndCorrect countTotalAndCorrect(User user) {
        return studyRecordRepository.countTotalAndCorrect(user);
    }

    public List<CategoryCount> countStudiedByCategory(User user) {
        return studyRecordRepository.countStudiedByCategory(user);
    }

    public List<CategoryCount> countLearningByCategory(User user) {
        return studyRecordRepository.countLearningByCategory(user);
    }

    public List<CategoryCount> countDueByCategory(User user, LocalDate date) {
        return studyRecordRepository.countDueByCategory(user, date);
    }

    public List<CategoryCount> countMasteredByCategory(User user) {
        return studyRecordRepository.countMasteredByCategory(user);
    }

    public List<DailyActivity> findDailyActivity(User user, LocalDateTime since) {
        return studyRecordRepository.findDailyActivity(user, since);
    }

    public Optional<StudySession> findActiveSession(User user) {
        return studySessionRepository.findByUserAndEndedAtIsNull(user);
    }

    public Page<StudySession> findSessionHistory(User user, Pageable pageable) {
        return studySessionRepository.findByUserOrderByStartedAtDesc(user, pageable);
    }

    public void validateSessionOwnership(StudySession session, User user) {
        if (!session.getUser().getId().equals(user.getId())) {
            throw new StudyException(StudyErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    public List<StudyRecord> findRecordsBySession(StudySession session) {
        return studyRecordRepository.findBySessionWithDetails(session);
    }

    public TotalAndCorrect countTodayStudy(User user, LocalDate date) {
        var result = studyRecordRepository.countTodayStudy(user, date);
        return new TotalAndCorrect(result.totalCount(), result.correctCount());
    }

    public boolean isCategoryFullyMastered(User user, Category category) {
        long totalCardsInCategory = cardRepository.countByCategoryAndStatus(category, CardStatus.ACTIVE);
        if (totalCardsInCategory == 0) {
            return false;
        }
        long masteredCardsInCategory = studyRecordRepository.countMasteredCardsInCategory(user, category);
        return masteredCardsInCategory >= totalCardsInCategory;
    }

    // === 복습 추천 관련 ===

    private static final int REPEATED_MISTAKE_THRESHOLD = 3;
    private static final int OVERDUE_DAYS = 7;
    private static final int SCORE_REPEATED_MISTAKE = 1000;
    private static final int SCORE_OVERDUE = 500;
    private static final int SCORE_RECENT_WRONG = 300;
    private static final int SCORE_EF_FACTOR_MAX = 120;

    public int calculatePriorityScore(StudyRecord record, List<Long> repeatedMistakeCardIds,
                                       List<Long> overdueCardIds, List<Long> recentWrongCardIds) {
        int score = 0;
        Long cardId = record.isForPublicCard()
                ? record.getCard().getId()
                : (record.isForUserCard() ? record.getUserCard().getId() : null);

        if (cardId == null) return 0;

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
        int efScore = (int) Math.round(SCORE_EF_FACTOR_MAX * (1.0 - (efFactor - SM2Constants.MIN_EF_FACTOR) / (2.5 - SM2Constants.MIN_EF_FACTOR)));
        score += Math.max(0, Math.min(SCORE_EF_FACTOR_MAX, efScore));

        return score;
    }

    public record ScoredRecord(StudyRecord record, int score) {}

    public List<ScoredRecord> findPrioritizedDueRecords(User user, int limit) {
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords = studyRecordRepository.findDueRecordsByCategory(user, today, null);
        List<StudyRecord> dueUserCardRecords = studyRecordRepository.findDueUserCardRecords(user, today);

        List<Long> repeatedMistakeIds = extractCardIds(
                studyRecordRepository.findRepeatedMistakeRecords(user, REPEATED_MISTAKE_THRESHOLD));
        List<Long> overdueIds = extractCardIds(
                studyRecordRepository.findOverdueRecords(user, today, OVERDUE_DAYS));
        List<Long> recentWrongIds = extractCardIds(
                studyRecordRepository.findRecentWrongRecords(user, 20));

        List<StudyRecord> allDue = new java.util.ArrayList<>(dueRecords);
        allDue.addAll(dueUserCardRecords);

        return allDue.stream()
                .map(r -> new ScoredRecord(r,
                        calculatePriorityScore(r, repeatedMistakeIds, overdueIds, recentWrongIds)))
                .sorted(Comparator.comparingInt(ScoredRecord::score).reversed())
                .limit(limit)
                .toList();
    }

    public List<CategoryAccuracy> calculateCategoryAccuracy(User user) {
        return studyRecordRepository.calculateCategoryAccuracy(user);
    }

    private List<Long> extractCardIds(List<StudyRecord> records) {
        return records.stream()
                .map(r -> r.isForPublicCard() ? r.getCard().getId() : r.getUserCard().getId())
                .distinct()
                .toList();
    }
}
