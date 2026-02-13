package com.example.study_cards.application.study.service;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.SessionResponse;
import com.example.study_cards.application.study.dto.response.SessionStatsResponse;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.application.card.dto.response.CardType;
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
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService.StudyCardItem;
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
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyService {

    private final StudyDomainService studyDomainService;
    private final CardDomainService cardDomainService;
    private final UserCardDomainService userCardDomainService;
    private final CategoryDomainService categoryDomainService;
    private final NotificationService notificationService;

    public Page<StudyCardResponse> getTodayCards(User user, String categoryCode, Pageable pageable) {
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;
        List<StudyCardItem> cards = studyDomainService.findTodayAllStudyCards(user, category, pageable.getPageSize());

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
        StudySession session = studyDomainService.findActiveSession(user)
                .orElseGet(() -> studyDomainService.createSession(user));

        StudyRecord record;
        Category category;

        if (request.cardType() == CardType.CUSTOM) {
            UserCard userCard = userCardDomainService.findById(request.cardId());
            record = studyDomainService.processUserCardAnswer(user, userCard, session, request.isCorrect());
            category = userCard.getCategory();
        } else {
            Card card = cardDomainService.findById(request.cardId());
            record = studyDomainService.processAnswer(user, card, session, request.isCorrect());
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

        if (studyDomainService.isCategoryFullyMastered(user, category)) {
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
    }

    @Transactional
    public SessionResponse endCurrentSession(User user) {
        StudySession session = studyDomainService.findActiveSession(user)
                .orElseThrow(() -> new StudyException(StudyErrorCode.NO_ACTIVE_SESSION));

        session.endSession();

        return SessionResponse.from(session);
    }

    public SessionResponse getCurrentSession(User user) {
        StudySession session = studyDomainService.findActiveSession(user)
                .orElseThrow(() -> new StudyException(StudyErrorCode.NO_ACTIVE_SESSION));

        return SessionResponse.from(session);
    }

    public SessionResponse getSession(User user, Long sessionId) {
        StudySession session = studyDomainService.findSessionById(sessionId);
        studyDomainService.validateSessionOwnership(session, user);

        return SessionResponse.from(session);
    }

    public Page<SessionResponse> getSessionHistory(User user, Pageable pageable) {
        Page<StudySession> sessions = studyDomainService.findSessionHistory(user, pageable);

        return sessions.map(SessionResponse::from);
    }

    public SessionStatsResponse getSessionStats(User user, Long sessionId) {
        StudySession session = studyDomainService.findSessionById(sessionId);
        studyDomainService.validateSessionOwnership(session, user);

        List<StudyRecord> records = studyDomainService.findRecordsBySession(session);

        return SessionStatsResponse.from(session, records);
    }
}
