package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.card.entity.QCard.card;
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
                        studyRecord.nextReviewDate.loe(date)
                )
                .fetchOne();
        return Optional.ofNullable(count).orElse(0L).intValue();
    }

    @Override
    public int countTotalStudiedCards(User user) {
        Long count = queryFactory
                .select(studyRecord.card.countDistinct())
                .from(studyRecord)
                .where(studyRecord.user.eq(user))
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
                .where(studyRecord.user.eq(user))
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
                        studyRecord.repetitionCount.between(1, 2)
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
                        studyRecord.repetitionCount.gt(2)
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
                        card.category.eq(category)
                )
                .fetch();
    }

    @Override
    public List<Long> findStudiedCardIdsByUser(User user) {
        return queryFactory
                .select(studyRecord.card.id)
                .from(studyRecord)
                .where(studyRecord.user.eq(user))
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
                        studyRecord.repetitionCount.goe(5)
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
    public long countMasteredCardsInCategory(User user, Category category) {
        Long count = queryFactory
                .select(studyRecord.card.countDistinct())
                .from(studyRecord)
                .join(studyRecord.card, card)
                .where(
                        studyRecord.user.eq(user),
                        card.category.eq(category),
                        studyRecord.repetitionCount.goe(3)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }
}
