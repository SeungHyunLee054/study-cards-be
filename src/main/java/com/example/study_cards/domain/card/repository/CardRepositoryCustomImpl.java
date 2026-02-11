package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
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
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoryOrderByEfFactorAsc(Category category) {
        return queryFactory
                .selectFrom(card)
                .where(card.category.eq(category))
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category) {
        return queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(card.category.eq(category))
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public Page<Card> findAllWithCategory(Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findByCategoryWithCategory(Category category, Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(card.category.eq(category))
                .orderBy(card.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(card.category.eq(category))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findAllByOrderByEfFactorAscWithCategory(Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category, Pageable pageable) {
        List<Card> content = queryFactory
                .selectFrom(card)
                .join(card.category).fetchJoin()
                .where(card.category.eq(category))
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(card.count())
                .from(card)
                .where(card.category.eq(category))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<Card> findAllByOrderByEfFactorAsc(boolean includeAiCards) {
        var query = queryFactory
                .selectFrom(card);

        if (!includeAiCards) {
            query.where(card.aiGenerated.eq(false));
        }

        return query
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<Card> findByCategoryOrderByEfFactorAsc(Category category, boolean includeAiCards) {
        var query = queryFactory
                .selectFrom(card)
                .where(card.category.eq(category));

        if (!includeAiCards) {
            query.where(card.aiGenerated.eq(false));
        }

        return query
                .orderBy(card.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
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
