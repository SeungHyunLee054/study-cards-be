package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.study_cards.domain.card.entity.QCard.card;

@RequiredArgsConstructor
public class CardRepositoryCustomImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoryCount> countByCategory() {
        return queryFactory
                .select(card.category.id, card.category.code, card.count())
                .from(card)
                .join(card.category)
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .groupBy(card.category.id, card.category.code)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        tuple.get(card.category.id),
                        tuple.get(card.category.code),
                        toNullableLong(tuple.get(2, Object.class))
                ))
                .toList();
    }

    @Override
    public List<Card> findAllByOrderByEfFactorAsc() {
        return queryFactory
                .selectFrom(card)
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoryOrderByEfFactorAsc(Category category) {
        return queryFactory
                .selectFrom(card)
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoriesOrderByEfFactorAsc(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(card)
                .where(
                        card.category.in(categories),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category) {
        return queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public Page<Card> findByCategoriesOrderByEfFactorAscWithCategory(List<Category> categories, Pageable pageable) {
        if (categories == null || categories.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.category.in(categories),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.category.in(categories),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findAllWithCategory(Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findByCategoryWithCategory(Category category, Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findByCategoriesWithCategory(List<Category> categories, Pageable pageable) {
        if (categories == null || categories.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.category.in(categories),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.category.in(categories),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findAllByOrderByEfFactorAscWithCategory(Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category, Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(
                        card.category.eq(category),
                        card.status.eq(CardStatus.ACTIVE),
                        card.category.status.eq(CategoryStatus.ACTIVE)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> searchByKeyword(String keyword, List<Category> categories, Pageable pageable) {
        BooleanExpression keywordCondition = card.question.containsIgnoreCase(keyword)
                .or(card.answer.containsIgnoreCase(keyword));

        BooleanExpression visibilityCondition = card.status.eq(CardStatus.ACTIVE)
                .and(card.category.status.eq(CategoryStatus.ACTIVE));

        BooleanExpression whereCondition = categories != null
                ? keywordCondition.and(card.category.in(categories)).and(visibilityCondition)
                : keywordCondition.and(visibilityCondition);

        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(whereCondition)
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
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
}
