package com.example.study_cards.domain.payment.repository;

import com.example.study_cards.domain.payment.entity.Payment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.example.study_cards.domain.payment.entity.QPayment.payment;

@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Payment> findByOrderIdForUpdate(String orderId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(payment)
                        .where(payment.orderId.eq(orderId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
