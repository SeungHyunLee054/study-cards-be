package com.example.study_cards.domain.payment.repository;

import com.example.study_cards.domain.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepositoryCustom {

    Optional<Payment> findByOrderIdForUpdate(String orderId);
}
