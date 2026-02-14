package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.study_cards.domain.usercard.entity.QUserCard.userCard;

@RequiredArgsConstructor
public class UserCardRepositoryCustomImpl implements UserCardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserCard> findByUserOrderByEfFactorAsc(User user) {
        return queryFactory
                .selectFrom(userCard)
                .where(userCard.user.eq(user))
                .orderBy(userCard.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public List<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(userCard)
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .orderBy(userCard.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .fetch();
    }

    @Override
    public Page<UserCard> findByUserWithCategory(User user, Pageable pageable) {
        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user))
                .orderBy(userCard.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<UserCard> findByUserAndCategoriesWithCategory(User user, List<Category> categories, Pageable pageable) {
        if (categories == null || categories.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .orderBy(userCard.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<UserCard> findByUserOrderByEfFactorAsc(User user, Pageable pageable) {
        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user))
                .orderBy(userCard.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories, Pageable pageable) {
        if (categories == null || categories.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .orderBy(userCard.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<UserCard> searchByKeyword(User user, String keyword, List<Category> categories, Pageable pageable) {
        BooleanExpression keywordCondition = userCard.question.containsIgnoreCase(keyword)
                .or(userCard.answer.containsIgnoreCase(keyword));

        BooleanExpression whereCondition = userCard.user.eq(user).and(keywordCondition);
        if (categories != null) {
            whereCondition = whereCondition.and(userCard.category.in(categories));
        }

        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(whereCondition)
                .orderBy(userCard.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public long countByUser(User user) {
        Long count = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public long countByUserAndCategories(User user, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return 0L;
        }

        Long count = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.in(categories))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
