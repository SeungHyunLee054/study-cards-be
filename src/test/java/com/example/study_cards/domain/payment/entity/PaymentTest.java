package com.example.study_cards.domain.payment.entity;

import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .orderId("ORDER_12345")
                .amount(3900)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .plan(SubscriptionPlan.PRO)
                .billingCycle(BillingCycle.MONTHLY)
                .customerKey("customer_123")
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("status 미지정 시 PENDING으로 설정된다")
        void builder_withoutStatus_defaultsToPending() {
            // when
            Payment newPayment = Payment.builder()
                    .orderId("ORDER_99999")
                    .amount(3900)
                    .type(PaymentType.INITIAL)
                    .build();

            // then
            assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("모든 필드가 정상적으로 설정된다")
        void builder_allFields_setCorrectly() {
            // then
            assertThat(payment.getOrderId()).isEqualTo("ORDER_12345");
            assertThat(payment.getAmount()).isEqualTo(3900);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getType()).isEqualTo(PaymentType.INITIAL);
            assertThat(payment.getPlan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(payment.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        }
    }

    @Nested
    @DisplayName("complete")
    class CompleteTest {

        @Test
        @DisplayName("결제를 완료하면 상태가 COMPLETED로 변경되고 결제 정보가 설정된다")
        void complete_updatesStatusAndPaymentInfo() {
            // given
            LocalDateTime paidAt = LocalDateTime.now();

            // when
            payment.complete("payment_key_abc", "카드", paidAt);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPaymentKey()).isEqualTo("payment_key_abc");
            assertThat(payment.getMethod()).isEqualTo("카드");
            assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("결제를 취소하면 상태가 CANCELED로 변경되고 취소 사유가 설정된다")
        void cancel_updatesStatusAndReason() {
            // when
            payment.cancel("고객 요청에 의한 취소");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(payment.getCancelReason()).isEqualTo("고객 요청에 의한 취소");
            assertThat(payment.getCanceledAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("fail")
    class FailTest {

        @Test
        @DisplayName("결제를 실패 처리하면 상태가 FAILED로 변경되고 실패 사유가 설정된다")
        void fail_updatesStatusAndReason() {
            // when
            payment.fail("잔액 부족");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailReason()).isEqualTo("잔액 부족");
        }
    }

    @Nested
    @DisplayName("isCompleted")
    class IsCompletedTest {

        @Test
        @DisplayName("COMPLETED 상태이면 true를 반환한다")
        void isCompleted_completedStatus_returnsTrue() {
            // given
            payment.complete("key", "카드", LocalDateTime.now());

            // then
            assertThat(payment.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("PENDING 상태이면 false를 반환한다")
        void isCompleted_pendingStatus_returnsFalse() {
            // then
            assertThat(payment.isCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPending")
    class IsPendingTest {

        @Test
        @DisplayName("PENDING 상태이면 true를 반환한다")
        void isPending_pendingStatus_returnsTrue() {
            // then
            assertThat(payment.isPending()).isTrue();
        }

        @Test
        @DisplayName("COMPLETED 상태이면 false를 반환한다")
        void isPending_completedStatus_returnsFalse() {
            // given
            payment.complete("key", "카드", LocalDateTime.now());

            // then
            assertThat(payment.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("linkSubscription")
    class LinkSubscriptionTest {

        @Test
        @DisplayName("구독을 연결한다")
        void linkSubscription_setsSubscription() {
            // given
            Subscription subscription = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .build();

            // when
            payment.linkSubscription(subscription);

            // then
            assertThat(payment.getSubscription()).isEqualTo(subscription);
        }
    }
}
