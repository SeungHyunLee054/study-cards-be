package com.example.study_cards.domain.bookmark.repository;

import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.bookmark.entity.QBookmark.bookmark;
import static com.example.study_cards.domain.card.entity.QCard.card;
import static com.example.study_cards.domain.usercard.entity.QUserCard.userCard;

@RequiredArgsConstructor
public class BookmarkRepositoryCustomImpl implements BookmarkRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Bookmark> findByUser(User user, Category category, Pageable pageable) {
        BooleanExpression condition = bookmark.user.eq(user)
                .and(visibleBookmarkCondition());

        if (category != null) {
            condition = condition.and(
                    bookmark.card.category.eq(category)
                            .or(bookmark.userCard.category.eq(category))
            );
        }

        List<Bookmark> content = queryFactory
                .selectFrom(bookmark)
                .leftJoin(bookmark.card, card).fetchJoin()
                .leftJoin(bookmark.userCard, userCard).fetchJoin()
                .where(condition)
                .orderBy(bookmark.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public boolean existsByUserAndCard(User user, Card targetCard) {
        Integer result = queryFactory
                .selectOne()
                .from(bookmark)
                .where(bookmark.user.eq(user), bookmark.card.eq(targetCard))
                .fetchFirst();
        return result != null;
    }

    @Override
    public boolean existsByUserAndUserCard(User user, UserCard targetUserCard) {
        Integer result = queryFactory
                .selectOne()
                .from(bookmark)
                .where(bookmark.user.eq(user), bookmark.userCard.eq(targetUserCard))
                .fetchFirst();
        return result != null;
    }

    @Override
    public Optional<Bookmark> findByUserAndCard(User user, Card targetCard) {
        Bookmark result = queryFactory
                .selectFrom(bookmark)
                .where(bookmark.user.eq(user), bookmark.card.eq(targetCard))
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Bookmark> findByUserAndUserCard(User user, UserCard targetUserCard) {
        Bookmark result = queryFactory
                .selectFrom(bookmark)
                .where(bookmark.user.eq(user), bookmark.userCard.eq(targetUserCard))
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    @Override
    public long countByUser(User user) {
        Long count = queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .where(
                        bookmark.user.eq(user),
                        visibleBookmarkCondition()
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression visibleBookmarkCondition() {
        return bookmark.card.isNull()
                .or(
                        bookmark.card.status.eq(CardStatus.ACTIVE)
                                .and(bookmark.card.category.status.eq(CategoryStatus.ACTIVE))
                );
    }
}
