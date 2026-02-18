package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.example.study_cards.domain.card.entity.QCard.card;
import static com.example.study_cards.domain.study.entity.QStudyRecord.studyRecord;
import static com.example.study_cards.domain.usercard.entity.QUserCard.userCard;

@RequiredArgsConstructor
public class StudyRecordRepositoryCustomImpl implements StudyRecordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StudyRecord> findDueRecordsByCategory(User user, LocalDate date, Category category) {
        return queryFactory
                .selectFrom(studyRecord)
                .join(studyRecord.card, card).fetchJoin()
                .join(card.category).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        activeJoinedCardCondition(),
                        category != null ? card.category.eq(category) : null
                )
                .fetch();
    }

    @Override
    public List<StudyRecord> findDueRecordsByCategories(User user, LocalDate date, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(studyRecord)
                .join(studyRecord.card, card).fetchJoin()
                .join(card.category).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        activeJoinedCardCondition(),
                        card.category.in(categories)
                )
                .fetch();
    }

    @Override
    public List<Long> findStudiedCardIdsByUser(User user) {
        return queryFactory
                .select(studyRecord.card.id)
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.card.isNotNull(),
                        studyRecord.card.status.eq(CardStatus.ACTIVE),
                        studyRecord.card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetch();
    }

    @Override
    public List<StudyRecord> findDueUserCardRecordsByCategory(User user, LocalDate date, Category category) {
        return queryFactory
                .selectFrom(studyRecord)
                .join(studyRecord.userCard, userCard).fetchJoin()
                .join(userCard.category).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        userCard.category.eq(category)
                )
                .fetch();
    }

    @Override
    public List<StudyRecord> findDueUserCardRecordsByCategories(User user, LocalDate date, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(studyRecord)
                .join(studyRecord.userCard, userCard).fetchJoin()
                .join(userCard.category).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        userCard.category.in(categories)
                )
                .fetch();
    }

    @Override
    public List<Long> findStudiedUserCardIdsByUser(User user) {
        return queryFactory
                .select(studyRecord.userCard.id)
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.userCard.isNotNull()
                )
                .fetch();
    }

    @Override
    public List<StudyRecord> findDueUserCardRecords(User user, LocalDate date) {
        return queryFactory
                .selectFrom(studyRecord)
                .join(studyRecord.userCard, userCard).fetchJoin()
                .join(userCard.category).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date)
                )
                .fetch();
    }

    @Override
    public List<StudyRecord> findBySessionWithDetails(StudySession session) {
        return queryFactory
                .selectFrom(studyRecord)
                .leftJoin(studyRecord.card, card).fetchJoin()
                .leftJoin(studyRecord.userCard, userCard).fetchJoin()
                .where(studyRecord.session.eq(session))
                .orderBy(studyRecord.studiedAt.asc())
                .fetch();
    }

    @Override
    public List<StudyRecord> findRecentWrongRecords(User user, int limit) {
        return queryFactory
                .selectFrom(studyRecord)
                .leftJoin(studyRecord.card, card).fetchJoin()
                .leftJoin(studyRecord.userCard, userCard).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.isCorrect.isFalse(),
                        activePublicCardCondition()
                )
                .orderBy(studyRecord.studiedAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<StudyRecord> findOverdueRecords(User user, LocalDate today, int overdueDays) {
        LocalDate overdueDate = today.minusDays(overdueDays);
        return queryFactory
                .selectFrom(studyRecord)
                .leftJoin(studyRecord.card, card).fetchJoin()
                .leftJoin(studyRecord.userCard, userCard).fetchJoin()
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(overdueDate),
                        activePublicCardCondition()
                )
                .orderBy(studyRecord.nextReviewDate.asc())
                .fetch();
    }

    @Override
    public List<StudyRecord> findRepeatedMistakeRecords(User user, int mistakeThreshold) {
        var wrongCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = false THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        List<Long> cardIds = queryFactory
                .select(studyRecord.card.id)
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.card.isNotNull(),
                        studyRecord.card.status.eq(CardStatus.ACTIVE),
                        studyRecord.card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .groupBy(studyRecord.card.id)
                .having(wrongCountExpr.goe(mistakeThreshold))
                .fetch();

        List<Long> userCardIds = queryFactory
                .select(studyRecord.userCard.id)
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.userCard.isNotNull()
                )
                .groupBy(studyRecord.userCard.id)
                .having(wrongCountExpr.goe(mistakeThreshold))
                .fetch();

        if (cardIds.isEmpty() && userCardIds.isEmpty()) {
            return List.of();
        }

        var conditions = studyRecord.user.eq(user);
        var cardCondition = cardIds.isEmpty() ? null : studyRecord.card.id.in(cardIds);
        var userCardCondition = userCardIds.isEmpty() ? null : studyRecord.userCard.id.in(userCardIds);

        var combinedCondition = conditions;
        if (cardCondition != null && userCardCondition != null) {
            combinedCondition = conditions.and(cardCondition.or(userCardCondition));
        } else if (cardCondition != null) {
            combinedCondition = conditions.and(cardCondition);
        } else {
            combinedCondition = conditions.and(userCardCondition);
        }

        return queryFactory
                .selectFrom(studyRecord)
                .leftJoin(studyRecord.card, card).fetchJoin()
                .leftJoin(studyRecord.userCard, userCard).fetchJoin()
                .where(
                        combinedCondition,
                        activePublicCardCondition()
                )
                .orderBy(studyRecord.efFactor.asc())
                .fetch();
    }

    private BooleanExpression activeJoinedCardCondition() {
        return card.status.eq(CardStatus.ACTIVE)
                .and(card.category.status.eq(CategoryStatus.ACTIVE));
    }

    private BooleanExpression activePublicCardCondition() {
        return studyRecord.card.isNull()
                .or(
                        studyRecord.card.status.eq(CardStatus.ACTIVE)
                                .and(studyRecord.card.category.status.eq(CategoryStatus.ACTIVE))
                );
    }
}
