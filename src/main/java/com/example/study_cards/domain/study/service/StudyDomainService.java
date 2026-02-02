package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords = studyRecordRepository.findDueRecordsByCategory(user, today, category);
        List<Card> dueCards = dueRecords.stream()
                .map(StudyRecord::getCard)
                .limit(limit)
                .collect(Collectors.toList());

        if (dueCards.size() >= limit) {
            return dueCards;
        }

        List<Long> studiedCardIds = studyRecordRepository.findStudiedCardIdsByUser(user);
        List<Card> newCards = cardRepository.findByCategoryOrderByEfFactorAsc(category).stream()
                .filter(card -> !studiedCardIds.contains(card.getId()))
                .limit(limit - dueCards.size())
                .collect(Collectors.toList());

        dueCards.addAll(newCards);
        return dueCards;
    }

    public List<Card> findTodayStudyCards(User user, Category category) {
        return findTodayStudyCards(user, category, DEFAULT_STUDY_LIMIT);
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
            int quality = isCorrect ? 4 : 2;
            double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
            double newEfFactor = Math.max(initialEfFactor + delta, 1.3);

            int initialInterval = 1;
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
            return 1;
        }

        if (repetitionCount == 1) {
            return 1;
        } else if (repetitionCount == 2) {
            return 6;
        } else {
            int prevInterval = (int) Math.round(6 * Math.pow(efFactor, repetitionCount - 3));
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
            int quality = isCorrect ? 4 : 2;
            double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
            double newEfFactor = Math.max(initialEfFactor + delta, 1.3);

            int initialInterval = 1;
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
}
