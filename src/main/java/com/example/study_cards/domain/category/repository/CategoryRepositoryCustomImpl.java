package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.category.entity.QCategory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.study_cards.domain.category.entity.QCategory.category;

@RequiredArgsConstructor
public class CategoryRepositoryCustomImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Category> findRootCategoriesWithChildren() {
        QCategory childCategory = new QCategory("childCategory");

        return queryFactory
                .selectDistinct(category)
                .from(category)
                .leftJoin(category.children, childCategory).fetchJoin()
                .where(
                        category.parent.isNull(),
                        category.status.eq(CategoryStatus.ACTIVE),
                        childCategory.status.eq(CategoryStatus.ACTIVE).or(childCategory.id.isNull())
                )
                .orderBy(category.displayOrder.asc(), childCategory.displayOrder.asc())
                .fetch();
    }

    @Override
    public List<Category> findAllWithParent() {
        return queryFactory
                .selectFrom(category)
                .leftJoin(category.parent).fetchJoin()
                .where(
                        category.status.eq(CategoryStatus.ACTIVE),
                        category.parent.isNull().or(category.parent.status.eq(CategoryStatus.ACTIVE))
                )
                .orderBy(category.depth.asc(), category.displayOrder.asc())
                .fetch();
    }
}
