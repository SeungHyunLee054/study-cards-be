package com.example.study_cards.domain.subscription.repository;

import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.subscription.entity.QSubscription.subscription;
import static com.example.study_cards.domain.subscription.entity.Subscription.AUTO_RENEWAL_DISABLED_MARKER;

@RequiredArgsConstructor
public class SubscriptionRepositoryCustomImpl implements SubscriptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Subscription> findActiveByUserId(Long userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(subscription)
                        .where(
                                subscription.user.id.eq(userId),
                                subscription.status.eq(SubscriptionStatus.ACTIVE),
                                subscription.endDate.after(LocalDateTime.now())
                        )
                        .fetchOne()
        );
    }

    @Override
    public boolean existsActiveByUserId(Long userId) {
        return queryFactory
                .selectFrom(subscription)
                .where(
                        subscription.user.id.eq(userId),
                        subscription.status.eq(SubscriptionStatus.ACTIVE),
                        subscription.endDate.after(LocalDateTime.now())
                )
                .fetchFirst() != null;
    }

    @Override
    public List<Subscription> findExpired(SubscriptionStatus status, LocalDateTime now) {
        return queryFactory
                .selectFrom(subscription)
                .where(
                        subscription.status.eq(status),
                        subscription.endDate.before(now)
                )
                .fetch();
    }

    @Override
    public List<Subscription> findRenewableSubscriptions(LocalDateTime now, LocalDateTime threshold) {
        return queryFactory
                .selectFrom(subscription)
                .where(
                        subscription.status.eq(SubscriptionStatus.ACTIVE),
                        subscription.billingCycle.eq(BillingCycle.MONTHLY),
                        subscription.billingKey.isNotNull(),
                        subscription.cancelReason.isNull().or(subscription.cancelReason.ne(AUTO_RENEWAL_DISABLED_MARKER)),
                        subscription.endDate.between(now, threshold)
                )
                .fetch();
    }

    @Override
    public List<Subscription> findExpiringOn(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return queryFactory
                .selectFrom(subscription)
                .join(subscription.user).fetchJoin()
                .where(
                        subscription.status.eq(SubscriptionStatus.ACTIVE),
                        subscription.endDate.goe(startOfDay),
                        subscription.endDate.lt(endOfDay)
                )
                .fetch();
    }
}
