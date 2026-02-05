package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.category.entity.QCategory.category;

@RequiredArgsConstructor
public class CategoryRepositoryCustomImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Category> findByCodeWithParent(String code) {
        Category result = queryFactory
                .selectFrom(category)
                .leftJoin(category.parent).fetchJoin()
                .where(category.code.eq(code))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Category> findRootCategoriesWithChildren() {
        return queryFactory
                .selectFrom(category)
                .leftJoin(category.children).fetchJoin()
                .where(category.parent.isNull())
                .orderBy(category.displayOrder.asc())
                .fetch();
    }

    @Override
    public List<Category> findAllWithParent() {
        return queryFactory
                .selectFrom(category)
                .leftJoin(category.parent).fetchJoin()
                .orderBy(category.depth.asc(), category.displayOrder.asc())
                .fetch();
    }
}
