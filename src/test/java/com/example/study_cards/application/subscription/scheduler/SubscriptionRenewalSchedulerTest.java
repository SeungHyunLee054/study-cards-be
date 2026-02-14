package com.example.study_cards.application.subscription.scheduler;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionRenewalSchedulerTest extends BaseUnitTest {

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private PaymentDomainService paymentDomainService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TossPaymentService tossPaymentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionRenewalScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "gracePeriodDays", 3);
    }

    @Nested
    @DisplayName("checkAndRenewSubscriptions")
    class CheckAndRenewSubscriptionsTest {

        @Test
        @DisplayName("갱신 가능한 구독이 없으면 처리하지 않는다")
        void checkAndRenew_noSubscriptions_doesNothing() {
            // given
            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(Collections.emptyList());
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(Collections.emptyList());

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(tossPaymentService, never()).billingPayment(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("빌링키가 없는 구독은 갱신하지 않는다")
        void checkAndRenew_noBillingKey_skipsRenewal() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, null);

            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(List.of(subscription));
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(Collections.emptyList());

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(tossPaymentService, never()).billingPayment(any(), any(), any(), anyInt(), any());
            verify(paymentDomainService, never()).createPayment(any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("연간 구독은 자동 갱신 대상에서 제외한다")
        void checkAndRenew_yearlySubscription_skipsRenewal() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, "billing_key_123", BillingCycle.YEARLY);

            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(List.of(subscription));
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(Collections.emptyList());

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(tossPaymentService, never()).billingPayment(any(), any(), any(), anyInt(), any());
            verify(paymentDomainService, never()).createPayment(any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("구독 갱신 성공")
        void checkAndRenew_success() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, "billing_key_123");
            Payment payment = createMockPayment();

            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(List.of(subscription));
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(Collections.emptyList());
            given(paymentDomainService.createPayment(any(), any(), anyInt(), any()))
                    .willReturn(payment);
            given(tossPaymentService.billingPayment(anyString(), anyString(), anyString(), anyInt(), anyString()))
                    .willReturn(createMockTossResponse());

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(tossPaymentService).billingPayment(
                    eq("billing_key_123"),
                    eq("customer_key_123"),
                    any(),
                    eq(9900),
                    contains("프로")
            );
            verify(paymentDomainService).completePayment(eq(payment), any(), any());
            verify(subscriptionDomainService).renewSubscription(subscription);
        }

        @Test
        @DisplayName("구독 갱신 실패 시 알림을 전송한다")
        void checkAndRenew_failure_sendsNotification() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, "billing_key_123");
            Payment payment = createMockPayment();

            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(List.of(subscription));
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(Collections.emptyList());
            given(paymentDomainService.createPayment(any(), any(), anyInt(), any()))
                    .willReturn(payment);
            given(tossPaymentService.billingPayment(anyString(), anyString(), anyString(), anyInt(), anyString()))
                    .willThrow(new RuntimeException("Payment failed"));

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(paymentDomainService).failPayment(eq(payment), anyString());
            verify(notificationService).sendNotification(
                    eq(mockUser),
                    eq(NotificationType.PAYMENT_FAILED),
                    eq("결제 실패"),
                    contains("결제가 실패했습니다")
            );
        }

        @Test
        @DisplayName("만료된 구독을 만료 처리한다")
        void checkAndRenew_expiresOldSubscriptions() {
            // given
            User mockUser = createMockUser();
            Subscription expiredSubscription = createMockSubscription(mockUser, null);

            given(subscriptionDomainService.findRenewableSubscriptions(3))
                    .willReturn(Collections.emptyList());
            given(subscriptionDomainService.findExpiredSubscriptions())
                    .willReturn(List.of(expiredSubscription));

            // when
            scheduler.checkAndRenewSubscriptions();

            // then
            verify(subscriptionDomainService).expireSubscription(expiredSubscription);
        }
    }

    @Nested
    @DisplayName("checkExpiringSubscriptions")
    class CheckExpiringSubscriptionsTest {

        @Test
        @DisplayName("7일, 3일, 1일 후 만료되는 구독에 알림을 전송한다")
        void checkExpiring_sendsNotifications() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, "billing_key");

            given(subscriptionRepository.findExpiringOn(any(), any()))
                    .willReturn(List.of(subscription));
            given(notificationService.existsNotification(any(), any(), anyLong()))
                    .willReturn(false);

            // when
            scheduler.checkExpiringSubscriptions();

            // then
            verify(notificationService, times(3)).sendNotification(
                    eq(mockUser),
                    any(NotificationType.class),
                    eq("구독 만료 임박"),
                    anyString(),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("이미 알림을 받은 경우 중복 알림을 보내지 않는다")
        void checkExpiring_alreadyNotified_skips() {
            // given
            User mockUser = createMockUser();
            Subscription subscription = createMockSubscription(mockUser, "billing_key");

            given(subscriptionRepository.findExpiringOn(any(), any()))
                    .willReturn(List.of(subscription));
            given(notificationService.existsNotification(any(), any(), anyLong()))
                    .willReturn(true);

            // when
            scheduler.checkExpiringSubscriptions();

            // then
            verify(notificationService, never()).sendNotification(
                    any(User.class), any(NotificationType.class), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("만료 예정 구독이 없으면 알림을 보내지 않는다")
        void checkExpiring_noExpiring_noNotifications() {
            // given
            given(subscriptionRepository.findExpiringOn(any(), any()))
                    .willReturn(Collections.emptyList());

            // when
            scheduler.checkExpiringSubscriptions();

            // then
            verify(notificationService, never()).sendNotification(
                    any(User.class), any(NotificationType.class), anyString(), anyString(), anyLong());
        }
    }

    private User createMockUser() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getEmail()).willReturn("test@example.com");
        return user;
    }

    private Subscription createMockSubscription(User user, String billingKey) {
        return createMockSubscription(user, billingKey, BillingCycle.MONTHLY);
    }

    private Subscription createMockSubscription(User user, String billingKey, BillingCycle billingCycle) {
        Subscription subscription = mock(Subscription.class);
        given(subscription.getId()).willReturn(1L);
        given(subscription.getUser()).willReturn(user);
        given(subscription.getBillingKey()).willReturn(billingKey);
        given(subscription.getCustomerKey()).willReturn("customer_key_123");
        given(subscription.getPlan()).willReturn(SubscriptionPlan.PRO);
        given(subscription.getBillingCycle()).willReturn(billingCycle);
        given(subscription.getEndDate()).willReturn(LocalDateTime.now().plusDays(3));
        return subscription;
    }

    private Payment createMockPayment() {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(1L);
        given(payment.getOrderId()).willReturn("ORDER_123");
        return payment;
    }

    private TossConfirmResponse createMockTossResponse() {
        return new TossConfirmResponse(
                "payment_key_123",
                "ORDER_123",
                "프로 월간 구독 갱신",
                "DONE",
                9900,
                "카드",
                "2024-01-01T10:00:00",
                "2024-01-01T10:00:00",
                null,
                null
        );
    }
}
