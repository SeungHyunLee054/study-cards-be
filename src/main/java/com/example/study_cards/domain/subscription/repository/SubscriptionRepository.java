package com.example.study_cards.domain.subscription.repository;

import com.example.study_cards.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, SubscriptionRepositoryCustom {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByCustomerKey(String customerKey);

    Optional<Subscription> findByBillingKey(String billingKey);
}
