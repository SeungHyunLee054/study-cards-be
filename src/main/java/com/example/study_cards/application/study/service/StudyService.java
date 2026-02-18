package com.example.study_cards.application.study.service;

import com.example.study_cards.application.card.dto.response.CardType;
import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.SessionResponse;
import com.example.study_cards.application.study.dto.response.SessionStatsResponse;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.study.service.StudySessionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyService {

    private final StudySessionDomainService studySessionDomainService;
    private final StudyRecordDomainService studyRecordDomainService;
    private final CardDomainService cardDomainService;
    private final UserCardDomainService userCardDomainService;
    private final CategoryDomainService categoryDomainService;
    private final NotificationService notificationService;

    public Page<StudyCardResponse> getTodayCards(User user, String categoryCode, Pageable pageable) {
        String normalizedCategoryCode = categoryCode != null ? categoryCode.trim() : null;
        Category category = (normalizedCategoryCode == null || normalizedCategoryCode.isEmpty())
                ? null
                : categoryDomainService.findByCode(normalizedCategoryCode);
        List<Category> categoryScope = category != null ? categoryDomainService.findSelfAndDescendants(category) : null;
        List<StudyCardItem> cards = findTodayAllStudyCards(user, categoryScope, pageable.getPageSize());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), cards.size());

        List<StudyCardResponse> content = start < cards.size()
                ? cards.subList(start, end).stream()
                .map(item -> item.isPublicCard()
                        ? StudyCardResponse.from(item.card())
                        : StudyCardResponse.fromUserCard(item.userCard()))
                .toList()
                : List.of();

        return new PageImpl<>(content, pageable, cards.size());
    }

    @Transactional
    public StudyResultResponse submitAnswer(User user, StudyAnswerRequest request) {
        StudySession session = studySessionDomainService.findActiveSession(user)
                .orElseGet(() -> studySessionDomainService.createSession(user));

        StudyRecord record;
        Category category;

        if (request.cardType() == CardType.CUSTOM) {
            UserCard userCard = userCardDomainService.findById(request.cardId());
            record = studyRecordDomainService.processUserCardAnswer(user, userCard, session, request.isCorrect());
            category = userCard.getCategory();
        } else {
            Card card = cardDomainService.findById(request.cardId());
            record = studyRecordDomainService.processAnswer(user, card, session, request.isCorrect());
            category = card.getCategory();
        }

        int previousStreak = user.getStreak();
        user.updateStreak(LocalDate.now());
        int newStreak = user.getStreak();

        checkAndSendStreakNotification(user, previousStreak, newStreak);

        if (request.isCorrect() && record.getRepetitionCount() >= SM2Constants.MASTERY_THRESHOLD) {
            checkAndSendCategoryMasteryNotification(user, category);
        }

        return new StudyResultResponse(
                request.cardId(),
                request.cardType(),
                request.isCorrect(),
                record.getNextReviewDate(),
                record.getEfFactor()
        );
    }

    private void checkAndSendStreakNotification(User user, int previousStreak, int newStreak) {
        NotificationType streakType = getStreakMilestoneType(previousStreak, newStreak);
        if (streakType != null) {
            notificationService.sendNotification(
                    user,
                    streakType,
                    "스트릭 달성!",
                    newStreak + "일 연속 학습을 달성했습니다!"
            );
        }
    }

    private NotificationType getStreakMilestoneType(int previous, int current) {
        if (previous < 7 && current >= 7) return NotificationType.STREAK_7;
        if (previous < 30 && current >= 30) return NotificationType.STREAK_30;
        if (previous < 100 && current >= 100) return NotificationType.STREAK_100;
        return null;
    }

    private void checkAndSendCategoryMasteryNotification(User user, Category category) {
        if (category == null) {
            return;
        }

        long totalCardsInCategory = cardDomainService.countByCategories(List.of(category));
        if (totalCardsInCategory == 0) {
            return;
        }

        long masteredCardsInCategory = studyRecordDomainService.countMasteredCardsInCategory(user, category);
        if (masteredCardsInCategory < totalCardsInCategory) {
            return;
        }

        boolean alreadyNotified = notificationService.existsNotification(
                user, NotificationType.CATEGORY_MASTERED, category.getId());

        if (!alreadyNotified) {
            notificationService.sendNotification(
                    user,
                    NotificationType.CATEGORY_MASTERED,
                    "카테고리 마스터!",
                    category.getName() + " 카테고리를 완전히 마스터했습니다!",
                    category.getId()
            );
        }
    }

    @Transactional
    public SessionResponse endCurrentSession(User user) {
        StudySession session = studySessionDomainService.findActiveSession(user)
                .orElseThrow(() -> new StudyException(StudyErrorCode.NO_ACTIVE_SESSION));

        session.endSession();

        return SessionResponse.from(session);
    }

    public SessionResponse getCurrentSession(User user) {
        StudySession session = studySessionDomainService.findActiveSession(user)
                .orElseThrow(() -> new StudyException(StudyErrorCode.NO_ACTIVE_SESSION));

        return SessionResponse.from(session);
    }

    public SessionResponse getSession(User user, Long sessionId) {
        StudySession session = studySessionDomainService.findSessionById(sessionId);
        studySessionDomainService.validateSessionOwnership(session, user);

        return SessionResponse.from(session);
    }

    public Page<SessionResponse> getSessionHistory(User user, Pageable pageable) {
        Page<StudySession> sessions = studySessionDomainService.findSessionHistory(user, pageable);
        return sessions.map(SessionResponse::from);
    }

    public SessionStatsResponse getSessionStats(User user, Long sessionId) {
        StudySession session = studySessionDomainService.findSessionById(sessionId);
        studySessionDomainService.validateSessionOwnership(session, user);

        List<StudyRecord> records = studyRecordDomainService.findRecordsBySession(session);
        return SessionStatsResponse.from(session, records);
    }

    private List<StudyCardItem> findTodayAllStudyCards(User user, List<Category> categories, int limit) {
        LocalDate today = LocalDate.now();
        boolean hasCategoryScope = categories != null && !categories.isEmpty();

        List<StudyRecord> dueUserCardRecords = hasCategoryScope
                ? studyRecordDomainService.findDueUserCardRecordsByCategories(user, today, categories)
                : studyRecordDomainService.findDueUserCardRecords(user, today);
        List<StudyRecord> dueCardRecords = hasCategoryScope
                ? studyRecordDomainService.findDueRecordsByCategories(user, today, categories)
                : studyRecordDomainService.findDueRecords(user, today);

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

        Set<Long> studiedUserCardIds = new HashSet<>(studyRecordDomainService.findStudiedUserCardIdsByUser(user));
        List<UserCard> newUserCards = (hasCategoryScope
                ? userCardDomainService.findByUserAndCategoriesOrderByEfFactorAsc(user, categories)
                : userCardDomainService.findByUserOrderByEfFactorAsc(user))
                .stream()
                .filter(uc -> !studiedUserCardIds.contains(uc.getId()))
                .limit(limit - result.size())
                .toList();
        newUserCards.stream().map(StudyCardItem::ofUserCard).forEach(result::add);

        if (result.size() >= limit) {
            return result.subList(0, limit);
        }

        Set<Long> studiedCardIds = new HashSet<>(studyRecordDomainService.findStudiedCardIdsByUser(user));
        List<Card> newCards = (hasCategoryScope
                ? cardDomainService.findCardsForStudyByCategories(categories)
                : cardDomainService.findCardsForStudy())
                .stream()
                .filter(card -> !studiedCardIds.contains(card.getId()))
                .limit(limit - result.size())
                .toList();
        newCards.stream().map(StudyCardItem::ofCard).forEach(result::add);

        return result.size() > limit ? result.subList(0, limit) : result;
    }

    private record StudyCardItem(Long id, Card card, UserCard userCard) {
        private static StudyCardItem ofCard(Card card) {
            return new StudyCardItem(card.getId(), card, null);
        }

        private static StudyCardItem ofUserCard(UserCard userCard) {
            return new StudyCardItem(userCard.getId(), null, userCard);
        }

        private boolean isPublicCard() {
            return card != null;
        }
    }
}

