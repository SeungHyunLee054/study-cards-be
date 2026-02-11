package com.example.study_cards.domain.generation.repository;

import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.study_cards.domain.generation.entity.QGeneratedCard.generatedCard;

@RequiredArgsConstructor
public class GeneratedCardRepositoryCustomImpl implements GeneratedCardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Object[]> countByModelGroupByStatus() {
        return queryFactory
                .select(generatedCard.model, generatedCard.status, generatedCard.count())
                .from(generatedCard)
                .groupBy(generatedCard.model, generatedCard.status)
                .fetch()
                .stream()
                .map(tuple -> new Object[]{
                        tuple.get(generatedCard.model),
                        tuple.get(generatedCard.status),
                        tuple.get(generatedCard.count())
                })
                .toList();
    }

    @Override
    public List<GeneratedCard> findByStatusWithCategory(GenerationStatus status) {
        return queryFactory
                .selectFrom(generatedCard)
                .join(generatedCard.category).fetchJoin()
                .where(generatedCard.status.eq(status))
                .fetch();
    }

    @Override
    public Page<GeneratedCard> findByStatusWithCategory(GenerationStatus status, Pageable pageable) {
        List<GeneratedCard> content = queryFactory
                .selectFrom(generatedCard)
                .join(generatedCard.category).fetchJoin()
                .where(generatedCard.status.eq(status))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(generatedCard.count())
                .from(generatedCard)
                .where(generatedCard.status.eq(status))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
