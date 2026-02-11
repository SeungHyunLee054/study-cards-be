package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
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
    public List<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category) {
        return queryFactory
                .selectFrom(userCard)
                .where(userCard.user.eq(user), userCard.category.eq(category))
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
    public Page<UserCard> findByUserAndCategoryWithCategory(User user, Category category, Pageable pageable) {
        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user), userCard.category.eq(category))
                .orderBy(userCard.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.eq(category))
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
    public Page<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category, Pageable pageable) {
        List<UserCard> content = queryFactory
                .selectFrom(userCard)
                .join(userCard.category).fetchJoin()
                .where(userCard.user.eq(user), userCard.category.eq(category))
                .orderBy(userCard.efFactor.asc(), Expressions.numberTemplate(Double.class, "random()").asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.eq(category))
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
    public long countByUserAndCategory(User user, Category category) {
        Long count = queryFactory
                .select(userCard.count())
                .from(userCard)
                .where(userCard.user.eq(user), userCard.category.eq(category))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
