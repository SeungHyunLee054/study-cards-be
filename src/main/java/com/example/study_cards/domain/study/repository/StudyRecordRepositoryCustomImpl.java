package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.study_cards.domain.card.entity.QCard.card;
import static com.example.study_cards.domain.category.entity.QCategory.category;
import static com.example.study_cards.domain.study.entity.QStudyRecord.studyRecord;
import static com.example.study_cards.domain.usercard.entity.QUserCard.userCard;

@RequiredArgsConstructor
public class StudyRecordRepositoryCustomImpl implements StudyRecordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public int countDueCards(User user, LocalDate date) {
        Long count = queryFactory
                .select(studyRecord.count())
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        activePublicCardCondition()
                )
                .fetchOne();
        return Optional.ofNullable(count).orElse(0L).intValue();
    }

    @Override
    public int countTotalStudiedCards(User user) {
        Long count = queryFactory
                .select(studyRecord.card.countDistinct())
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.card.isNotNull(),
                        studyRecord.card.status.eq(CardStatus.ACTIVE),
                        studyRecord.card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();
        return Optional.ofNullable(count).orElse(0L).intValue();
    }

    @Override
    public List<CategoryCount> countStudiedByCategory(User user) {
        return queryFactory
                .select(studyRecord.card.category.id, studyRecord.card.category.code, studyRecord.card.countDistinct())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .join(card.category)
                .where(
                        studyRecord.user.eq(user),
                        activeJoinedCardCondition()
                )
                .groupBy(studyRecord.card.category.id, studyRecord.card.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.card.category.id),
                        tuple.get(studyRecord.card.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
    }

    @Override
    public List<CategoryCount> countLearningByCategory(User user) {
        return queryFactory
                .select(studyRecord.card.category.id, studyRecord.card.category.code, studyRecord.count())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .join(card.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.repetitionCount.between(1, 2),
                        activeJoinedCardCondition()
                )
                .groupBy(studyRecord.card.category.id, studyRecord.card.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.card.category.id),
                        tuple.get(studyRecord.card.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
    }

    @Override
    public List<CategoryCount> countDueByCategory(User user, LocalDate date) {
        return queryFactory
                .select(studyRecord.card.category.id, studyRecord.card.category.code, studyRecord.count())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .join(card.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        studyRecord.repetitionCount.gt(2),
                        activeJoinedCardCondition()
                )
                .groupBy(studyRecord.card.category.id, studyRecord.card.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.card.category.id),
                        tuple.get(studyRecord.card.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
    }

    @Override
    public List<DailyActivity> findDailyActivity(User user, LocalDateTime since) {
        DateTemplate<LocalDate> studyDate = Expressions.dateTemplate(
                LocalDate.class,
                "CAST({0} AS date)",
                studyRecord.studiedAt
        );

        var correctCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = true THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        return queryFactory
                .select(
                        studyDate,
                        studyRecord.count(),
                        correctCountExpr
                )
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.studiedAt.goe(since)
                )
                .groupBy(studyDate)
                .orderBy(studyDate.desc())
                .fetch()
                .stream()
                .map(tuple -> new DailyActivity(
                        toLocalDate(tuple.get(0, Object.class)),
                        toNullableLong(tuple.get(1, Object.class)),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
    }

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
    public TotalAndCorrect countTotalAndCorrect(User user) {
        var correctCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = true THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        var result = queryFactory
                .select(
                        studyRecord.count(),
                        correctCountExpr
                )
                .from(studyRecord)
                .where(studyRecord.user.eq(user))
                .fetchOne();

        if (result == null) {
            return new TotalAndCorrect(0L, 0L);
        }
        return new TotalAndCorrect(
                toNullableLong(result.get(0, Object.class)),
                toNullableLong(result.get(1, Object.class))
        );
    }

    @Override
    public List<CategoryCount> countMasteredByCategory(User user) {
        return queryFactory
                .select(studyRecord.card.category.id, studyRecord.card.category.code, studyRecord.card.countDistinct())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .join(card.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.repetitionCount.goe(SM2Constants.MASTERY_THRESHOLD),
                        activeJoinedCardCondition()
                )
                .groupBy(studyRecord.card.category.id, studyRecord.card.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.card.category.id),
                        tuple.get(studyRecord.card.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
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
    public TodayStudyCount countTodayStudy(User user, LocalDate date) {
        var correctCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = true THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        DateTemplate<LocalDate> studyDate = Expressions.dateTemplate(
                LocalDate.class,
                "CAST({0} AS date)",
                studyRecord.studiedAt
        );

        var result = queryFactory
                .select(
                        studyRecord.count(),
                        correctCountExpr
                )
                .from(studyRecord)
                .where(
                        studyRecord.user.eq(user),
                        studyDate.eq(date)
                )
                .fetchOne();

        if (result == null) {
            return new TodayStudyCount(0L, 0L);
        }
        return new TodayStudyCount(
                toNullableLong(result.get(0, Object.class)),
                toNullableLong(result.get(1, Object.class))
        );
    }

    private Long toNullableLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return null;
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

        // 서브쿼리로 카드별 오답 횟수가 threshold 이상인 카드 ID 조회
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

    @Override
    public List<CategoryAccuracy> calculateCategoryAccuracy(User user) {
        var correctCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = true THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        Map<Long, CategoryAccuracyAccumulator> accumulators = new LinkedHashMap<>();

        queryFactory
                .select(
                        card.category.id,
                        card.category.code,
                        card.category.name,
                        studyRecord.count(),
                        correctCountExpr
                )
                .from(studyRecord)
                .join(studyRecord.card, card)
                .join(card.category, category)
                .where(
                        studyRecord.user.eq(user),
                        activeJoinedCardCondition()
                )
                .groupBy(card.category.id, card.category.code, card.category.name)
                .fetch()
                .stream()
                .map(tuple -> {
                    Long total = toNullableLong(tuple.get(3, Object.class));
                    Long correct = toNullableLong(tuple.get(4, Object.class));
                    return new CategoryAccuracyAccumulator(
                            tuple.get(card.category.id),
                            tuple.get(card.category.code),
                            tuple.get(card.category.name),
                            total,
                            correct
                    );
                })
                .forEach(acc -> accumulators.put(acc.categoryId, acc));

        queryFactory
                .select(
                        userCard.category.id,
                        userCard.category.code,
                        userCard.category.name,
                        studyRecord.count(),
                        correctCountExpr
                )
                .from(studyRecord)
                .join(studyRecord.userCard, userCard)
                .join(userCard.category, category)
                .where(
                        studyRecord.user.eq(user),
                        userCard.category.status.eq(CategoryStatus.ACTIVE)
                )
                .groupBy(userCard.category.id, userCard.category.code, userCard.category.name)
                .fetch()
                .forEach(tuple -> {
                    Long categoryId = tuple.get(userCard.category.id);
                    if (categoryId == null) {
                        return;
                    }

                    Long total = toNullableLong(tuple.get(3, Object.class));
                    Long correct = toNullableLong(tuple.get(4, Object.class));

                    accumulators.compute(categoryId, (id, existing) -> {
                        if (existing == null) {
                            return new CategoryAccuracyAccumulator(
                                    id,
                                    tuple.get(userCard.category.code),
                                    tuple.get(userCard.category.name),
                                    total,
                                    correct
                            );
                        }
                        existing.add(total, correct);
                        return existing;
                    });
                });

        return accumulators.values().stream()
                .map(CategoryAccuracyAccumulator::toResponse)
                .toList();
    }

    @Override
    public long countMasteredCardsInCategory(User user, Category cat) {
        Long count = queryFactory
                .select(studyRecord.card.countDistinct())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .where(
                        studyRecord.user.eq(user),
                        card.category.eq(cat),
                        activeJoinedCardCondition(),
                        studyRecord.repetitionCount.goe(SM2Constants.MASTERY_THRESHOLD)
                )
                .fetchOne();
        return count != null ? count : 0L;
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

    private static class CategoryAccuracyAccumulator {
        private final Long categoryId;
        private final String categoryCode;
        private final String categoryName;
        private long totalCount;
        private long correctCount;

        private CategoryAccuracyAccumulator(
                Long categoryId,
                String categoryCode,
                String categoryName,
                long totalCount,
                long correctCount
        ) {
            this.categoryId = categoryId;
            this.categoryCode = categoryCode;
            this.categoryName = categoryName;
            this.totalCount = totalCount;
            this.correctCount = correctCount;
        }

        private void add(long totalCount, long correctCount) {
            this.totalCount += totalCount;
            this.correctCount += correctCount;
        }

        private CategoryAccuracy toResponse() {
            double accuracy = totalCount > 0 ? (correctCount * 100.0) / totalCount : 0.0;
            return new CategoryAccuracy(
                    categoryId,
                    categoryCode,
                    categoryName,
                    totalCount,
                    correctCount,
                    Math.round(accuracy * 10.0) / 10.0
            );
        }
    }
}
