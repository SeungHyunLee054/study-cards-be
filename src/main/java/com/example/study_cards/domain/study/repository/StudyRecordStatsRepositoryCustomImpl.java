package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.study.constant.SM2Constants;
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
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryCount;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.DailyActivity;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TodayStudyCount;
import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TotalAndCorrect;
import static com.example.study_cards.domain.usercard.entity.QUserCard.userCard;

@RequiredArgsConstructor
public class StudyRecordStatsRepositoryCustomImpl implements StudyRecordStatsRepositoryCustom {

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
    public List<CategoryCount> countStudiedUserCardsByCategory(User user) {
        return queryFactory
                .select(studyRecord.userCard.category.id, studyRecord.userCard.category.code, studyRecord.userCard.countDistinct())
                .from(studyRecord)
                .join(studyRecord.userCard, userCard)
                .join(userCard.category)
                .where(
                        studyRecord.user.eq(user),
                        activeJoinedUserCardCondition()
                )
                .groupBy(studyRecord.userCard.category.id, studyRecord.userCard.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.userCard.category.id),
                        tuple.get(studyRecord.userCard.category.code),
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
    public List<CategoryCount> countLearningUserCardsByCategory(User user) {
        return queryFactory
                .select(studyRecord.userCard.category.id, studyRecord.userCard.category.code, studyRecord.count())
                .from(studyRecord)
                .join(studyRecord.userCard, userCard)
                .join(userCard.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.repetitionCount.between(1, 2),
                        activeJoinedUserCardCondition()
                )
                .groupBy(studyRecord.userCard.category.id, studyRecord.userCard.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.userCard.category.id),
                        tuple.get(studyRecord.userCard.category.code),
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
    public List<CategoryCount> countDueUserCardsByCategory(User user, LocalDate date) {
        return queryFactory
                .select(studyRecord.userCard.category.id, studyRecord.userCard.category.code, studyRecord.count())
                .from(studyRecord)
                .join(studyRecord.userCard, userCard)
                .join(userCard.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.nextReviewDate.loe(date),
                        studyRecord.repetitionCount.gt(2),
                        activeJoinedUserCardCondition()
                )
                .groupBy(studyRecord.userCard.category.id, studyRecord.userCard.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.userCard.category.id),
                        tuple.get(studyRecord.userCard.category.code),
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
                .select(studyDate, studyRecord.count(), correctCountExpr)
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
    public TotalAndCorrect countTotalAndCorrect(User user) {
        var correctCountExpr = Expressions.numberTemplate(
                Long.class,
                "SUM(CASE WHEN {0} = true THEN 1 ELSE 0 END)",
                studyRecord.isCorrect
        );

        var result = queryFactory
                .select(studyRecord.count(), correctCountExpr)
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
    public List<CategoryCount> countMasteredUserCardsByCategory(User user) {
        return queryFactory
                .select(studyRecord.userCard.category.id, studyRecord.userCard.category.code, studyRecord.userCard.countDistinct())
                .from(studyRecord)
                .join(studyRecord.userCard, userCard)
                .join(userCard.category)
                .where(
                        studyRecord.user.eq(user),
                        studyRecord.repetitionCount.goe(SM2Constants.MASTERY_THRESHOLD),
                        activeJoinedUserCardCondition()
                )
                .groupBy(studyRecord.userCard.category.id, studyRecord.userCard.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(studyRecord.userCard.category.id),
                        tuple.get(studyRecord.userCard.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
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
                .select(studyRecord.count(), correctCountExpr)
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

    private BooleanExpression activeJoinedCardCondition() {
        return card.status.eq(CardStatus.ACTIVE)
                .and(card.category.status.eq(CategoryStatus.ACTIVE));
    }

    private BooleanExpression activeJoinedUserCardCondition() {
        return userCard.category.status.eq(CategoryStatus.ACTIVE);
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
