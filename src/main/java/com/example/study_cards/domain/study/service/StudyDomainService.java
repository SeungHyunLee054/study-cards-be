package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.*;

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

    public List<Card> findTodayStudyCards(User user, Category category, int limit) {
        return findTodayStudyCards(user, category, limit, true);
    }

    public List<Card> findTodayStudyCards(User user, Category category, int limit, boolean includeAiCards) {
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords = studyRecordRepository.findDueRecordsByCategory(user, today, category);
        List<Card> dueCards = dueRecords.stream()
                .map(StudyRecord::getCard)
                .filter(card -> includeAiCards || !card.isAiGenerated())
                .limit(limit)
                .collect(Collectors.toList());

        if (dueCards.size() >= limit) {
            return dueCards;
        }

        List<Long> studiedCardIds = studyRecordRepository.findStudiedCardIdsByUser(user);
        List<Card> newCards = (category != null
                ? cardRepository.findByCategoryOrderByEfFactorAsc(category, includeAiCards)
                : cardRepository.findAllByOrderByEfFactorAsc(includeAiCards))
                .stream()
                .filter(card -> !studiedCardIds.contains(card.getId()))
                .limit(limit - dueCards.size())
                .collect(Collectors.toList());

        dueCards.addAll(newCards);
        return dueCards;
    }

    public List<Card> findTodayStudyCards(User user, Category category) {
        return findTodayStudyCards(user, category, DEFAULT_STUDY_LIMIT, true);
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

    public List<UserCard> findTodayUserCardsForStudy(User user, Category category, int limit) {
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords = category != null
                ? studyRecordRepository.findDueUserCardRecordsByCategory(user, today, category)
                : studyRecordRepository.findDueUserCardRecords(user, today);

        List<UserCard> dueCards = dueRecords.stream()
                .map(StudyRecord::getUserCard)
                .limit(limit)
                .collect(Collectors.toList());

        if (dueCards.size() >= limit) {
            return dueCards;
        }

        List<Long> studiedUserCardIds = studyRecordRepository.findStudiedUserCardIdsByUser(user);
        List<UserCard> newCards = (category != null
                ? userCardRepository.findByUserAndCategoryOrderByEfFactorAsc(user, category)
                : userCardRepository.findByUserOrderByEfFactorAsc(user))
                .stream()
                .filter(card -> !studiedUserCardIds.contains(card.getId()))
                .limit(limit - dueCards.size())
                .collect(Collectors.toList());

        dueCards.addAll(newCards);
        return dueCards;
    }

    public List<UserCard> findTodayUserCardsForStudy(User user, Category category) {
        return findTodayUserCardsForStudy(user, category, DEFAULT_STUDY_LIMIT);
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

    public List<Object> findAllCardsForStudy(User user, Category category, int limit) {
        List<Object> allCards = new ArrayList<>();

        List<Card> publicCards = findTodayStudyCards(user, category, limit);
        allCards.addAll(publicCards);

        if (allCards.size() < limit) {
            List<UserCard> userCards = findTodayUserCardsForStudy(user, category, limit - allCards.size());
            allCards.addAll(userCards);
        }

        return allCards;
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

    public void validateSessionActive(StudySession session) {
        if (session.getEndedAt() != null) {
            throw new StudyException(StudyErrorCode.SESSION_ALREADY_ENDED);
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
        long totalCardsInCategory = cardRepository.countByCategory(category);
        if (totalCardsInCategory == 0) {
            return false;
        }
        long masteredCardsInCategory = studyRecordRepository.countMasteredCardsInCategory(user, category);
        return masteredCardsInCategory >= totalCardsInCategory;
    }
}
