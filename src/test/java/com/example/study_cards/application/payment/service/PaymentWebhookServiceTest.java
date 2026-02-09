package com.example.study_cards.application.payment.service;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.repository.PaymentRepository;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.dto.TossWebhookPayload.DataPayload;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class PaymentWebhookServiceTest extends BaseUnitTest {

    @InjectMocks
    private PaymentWebhookService paymentWebhookService;

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private PaymentDomainService paymentDomainService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentService tossPaymentService;

    @Mock
    private NotificationService notificationService;

    @Nested
    @DisplayName("handlePaymentStatusChanged")
    class HandlePaymentStatusChangedTest {

        @Nested
        @DisplayName("DONE 상태")
        class DoneStatusTest {

            @Test
            @DisplayName("PENDING 결제를 완료하고 구독을 생성한다")
            void handleDone_pendingPayment_completesAndCreatesSubscription() {
                User user = createMockUser();
                Payment payment = createPendingPayment(user);
                Subscription subscription = createMockSubscription(user);
                DataPayload data = createDataPayload("DONE", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.of(payment));
                given(paymentDomainService.tryCompletePayment(payment, "pk_123", "카드")).willReturn(true);
                given(subscriptionDomainService.createSubscriptionFromPayment(payment, null)).willReturn(subscription);

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).tryCompletePayment(payment, "pk_123", "카드");
                verify(subscriptionDomainService).createSubscriptionFromPayment(payment, null);
            }

            @Test
            @DisplayName("이미 완료된 결제는 스킵한다 (멱등성)")
            void handleDone_alreadyCompleted_skips() {
                Payment payment = createCompletedPayment();
                DataPayload data = createDataPayload("DONE", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.of(payment));
                given(paymentDomainService.tryCompletePayment(payment, "pk_123", "카드")).willReturn(false);

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(subscriptionDomainService, never()).createSubscriptionFromPayment(any(), any());
            }

            @Test
            @DisplayName("구독이 이미 존재하면 구독 생성을 스킵한다")
            void handleDone_subscriptionAlreadyExists_skipsCreation() {
                User user = createMockUser();
                Payment payment = createPendingPayment(user);
                DataPayload data = createDataPayload("DONE", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.of(payment));
                given(paymentDomainService.tryCompletePayment(payment, "pk_123", "카드")).willReturn(true);
                given(subscriptionDomainService.createSubscriptionFromPayment(payment, null))
                        .willThrow(new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS));

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).tryCompletePayment(payment, "pk_123", "카드");
            }

            @Test
            @DisplayName("결제가 존재하지 않으면 무시한다")
            void handleDone_paymentNotFound_ignores() {
                DataPayload data = createDataPayload("DONE", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.empty());

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService, never()).tryCompletePayment(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("CANCELED 상태")
        class CanceledStatusTest {

            @Test
            @DisplayName("결제를 취소하고 연결된 구독도 취소한다")
            void handleCanceled_cancelsPaymentAndSubscription() {
                User user = createMockUser();
                Subscription subscription = createMockSubscription(user);
                Payment payment = createCompletedPaymentWithSubscription(user, subscription);
                DataPayload data = createCanceledDataPayload("pk_123", "ORDER_123", "고객 요청");

                given(paymentRepository.findByPaymentKey("pk_123")).willReturn(Optional.of(payment));

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).cancelPayment(payment, "고객 요청");
                verify(subscriptionDomainService).cancelSubscription(subscription, "고객 요청");
                verify(notificationService).sendNotification(
                        eq(user), eq(NotificationType.PAYMENT_CANCELED), anyString(), anyString());
            }

            @Test
            @DisplayName("구독이 없는 결제는 결제만 취소한다")
            void handleCanceled_noSubscription_cancelsPaymentOnly() {
                User user = createMockUser();
                Payment payment = createCompletedPaymentWithoutSubscription(user);
                DataPayload data = createCanceledDataPayload("pk_123", "ORDER_123", "고객 요청");

                given(paymentRepository.findByPaymentKey("pk_123")).willReturn(Optional.of(payment));

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).cancelPayment(payment, "고객 요청");
                verify(subscriptionDomainService, never()).cancelSubscription(any(), any());
                verify(notificationService).sendNotification(
                        eq(user), eq(NotificationType.PAYMENT_CANCELED), anyString(), anyString());
            }
        }

        @Nested
        @DisplayName("ABORTED/EXPIRED 상태")
        class FailedStatusTest {

            @Test
            @DisplayName("ABORTED 상태의 결제를 실패 처리하고 알림을 보낸다")
            void handleAborted_failsPaymentAndNotifies() {
                User user = createMockUser();
                Payment payment = createPendingPayment(user);
                DataPayload data = createDataPayload("ABORTED", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.of(payment));

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).failPayment(payment, "Payment aborted");
                verify(notificationService).sendNotification(
                        eq(user), eq(NotificationType.PAYMENT_FAILED), anyString(), anyString());
            }

            @Test
            @DisplayName("EXPIRED 상태의 결제를 실패 처리하고 알림을 보낸다")
            void handleExpired_failsPaymentAndNotifies() {
                User user = createMockUser();
                Payment payment = createPendingPayment(user);
                DataPayload data = createDataPayload("EXPIRED", "ORDER_123", "pk_123", "카드");

                given(paymentRepository.findByOrderId("ORDER_123")).willReturn(Optional.of(payment));

                paymentWebhookService.handlePaymentStatusChanged(data);

                verify(paymentDomainService).failPayment(payment, "Payment expired");
                verify(notificationService).sendNotification(
                        eq(user), eq(NotificationType.PAYMENT_FAILED), anyString(), anyString());
            }
        }
    }

    @Nested
    @DisplayName("handleBillingKeyDeleted")
    class HandleBillingKeyDeletedTest {

        @Test
        @DisplayName("구독의 자동 갱신을 비활성화하고 알림을 보낸다")
        void handleBillingKeyDeleted_disablesAutoRenewalAndNotifies() {
            User user = createMockUser();
            Subscription subscription = createMockSubscription(user);
            DataPayload data = createBillingKeyDeletedPayload("bk_123", "ck_123");

            given(subscriptionDomainService.findSubscriptionByBillingKey("bk_123"))
                    .willReturn(Optional.of(subscription));

            paymentWebhookService.handleBillingKeyDeleted(data);

            verify(subscriptionDomainService).disableAutoRenewal(subscription);
            verify(notificationService).sendNotification(
                    eq(user), eq(NotificationType.AUTO_RENEWAL_DISABLED), anyString(), anyString());
        }

        @Test
        @DisplayName("구독을 찾을 수 없으면 무시한다")
        void handleBillingKeyDeleted_subscriptionNotFound_ignores() {
            DataPayload data = createBillingKeyDeletedPayload("bk_123", "ck_123");

            given(subscriptionDomainService.findSubscriptionByBillingKey("bk_123"))
                    .willReturn(Optional.empty());

            paymentWebhookService.handleBillingKeyDeleted(data);

            verify(subscriptionDomainService, never()).disableAutoRenewal(any());
            verify(notificationService, never()).sendNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("billingKey가 null이면 무시한다")
        void handleBillingKeyDeleted_nullBillingKey_ignores() {
            DataPayload data = createBillingKeyDeletedPayload(null, "ck_123");

            paymentWebhookService.handleBillingKeyDeleted(data);

            verify(subscriptionDomainService, never()).findSubscriptionByBillingKey(any());
        }
    }

    // Helper methods

    private User createMockUser() {
        return fixtureMonkey.giveMeBuilder(User.class)
                .set("id", 1L)
                .set("email", "test@example.com")
                .sample();
    }

    private Payment createPendingPayment(User user) {
        return Payment.builder()
                .user(user)
                .orderId("ORDER_123")
                .amount(3900)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .plan(SubscriptionPlan.PREMIUM)
                .billingCycle(BillingCycle.MONTHLY)
                .customerKey("ck_123")
                .build();
    }

    private Payment createCompletedPayment() {
        return Payment.builder()
                .user(createMockUser())
                .orderId("ORDER_123")
                .paymentKey("pk_123")
                .amount(3900)
                .status(PaymentStatus.COMPLETED)
                .type(PaymentType.INITIAL)
                .build();
    }

    private Payment createCompletedPaymentWithSubscription(User user, Subscription subscription) {
        return Payment.builder()
                .user(user)
                .subscription(subscription)
                .orderId("ORDER_123")
                .paymentKey("pk_123")
                .amount(3900)
                .status(PaymentStatus.COMPLETED)
                .type(PaymentType.INITIAL)
                .build();
    }

    private Payment createCompletedPaymentWithoutSubscription(User user) {
        return Payment.builder()
                .user(user)
                .orderId("ORDER_123")
                .paymentKey("pk_123")
                .amount(3900)
                .status(PaymentStatus.COMPLETED)
                .type(PaymentType.INITIAL)
                .build();
    }

    private Subscription createMockSubscription(User user) {
        return Subscription.builder()
                .user(user)
                .plan(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(java.time.LocalDateTime.now())
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .billingKey("bk_123")
                .customerKey("ck_123")
                .build();
    }

    private DataPayload createDataPayload(String status, String orderId, String paymentKey, String method) {
        return new DataPayload(paymentKey, orderId, status, 3900, method, null, null, null, null, null);
    }

    private DataPayload createCanceledDataPayload(String paymentKey, String orderId, String cancelReason) {
        return new DataPayload(paymentKey, orderId, "CANCELED", 3900, "카드", null, "2024-01-01T11:00:00", cancelReason, null, null);
    }

    private DataPayload createBillingKeyDeletedPayload(String billingKey, String customerKey) {
        return new DataPayload(null, null, null, null, null, null, null, null, billingKey, customerKey);
    }
}
