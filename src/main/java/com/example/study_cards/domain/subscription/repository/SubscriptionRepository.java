package com.example.study_cards.domain.subscription.repository;

import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByCustomerKey(String customerKey);

    Optional<Subscription> findByBillingKey(String billingKey);

    boolean existsByUserId(Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate BETWEEN :now AND :threshold")
    List<Subscription> findExpiringSoon(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate < :now")
    List<Subscription> findExpired(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.billingKey IS NOT NULL AND s.endDate BETWEEN :now AND :threshold")
    List<Subscription> findRenewableSubscriptions(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.status = 'ACTIVE' " +
            "AND s.endDate >= :startOfDay AND s.endDate < :endOfDay")
    List<Subscription> findExpiringOn(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
