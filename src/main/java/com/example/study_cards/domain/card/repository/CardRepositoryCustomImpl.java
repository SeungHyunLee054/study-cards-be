package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Category;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.study_cards.domain.card.entity.QCard.card;

@RequiredArgsConstructor
public class CardRepositoryCustomImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoryCount> countByCategory() {
        return queryFactory
                .select(card.category, card.count())
                .from(card)
                .groupBy(card.category)
                .fetch()
                .stream()
                .map(tuple -> new CategoryCount(
                        (Category) tuple.get(0, Object.class),
                        toNullableLong(tuple.get(1, Object.class))
                ))
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
}
