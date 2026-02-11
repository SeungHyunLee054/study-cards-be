package com.example.study_cards.domain.subscription.repository;

import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepositoryCustom {

    Optional<Subscription> findActiveByUserId(Long userId);

    boolean existsActiveByUserId(Long userId);

    List<Subscription> findExpired(SubscriptionStatus status, LocalDateTime now);

    List<Subscription> findRenewableSubscriptions(LocalDateTime now, LocalDateTime threshold);

    List<Subscription> findExpiringOn(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
