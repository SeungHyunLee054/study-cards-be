package com.example.study_cards.application.payment.service;

import com.example.study_cards.application.payment.dto.request.CheckoutRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmBillingRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.payment.dto.response.CheckoutResponse;
import com.example.study_cards.application.payment.dto.response.PaymentHistoryResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.domain.payment.repository.PaymentRepository;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.dto.response.TossBillingAuthResponse;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentServiceUnitTest extends BaseUnitTest {

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private PaymentDomainService paymentDomainService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TossPaymentService tossPaymentService;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Subscription testSubscription;
    private Payment testPayment;

    private static final Long USER_ID = 1L;
    private static final String CUSTOMER_KEY = "CK_TEST123";
    private static final String ORDER_ID = "ORDER_TEST123";
    private static final String PAYMENT_KEY = "pk_test123";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testSubscription = createTestSubscription();
        testPayment = createTestPayment();
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Subscription createTestSubscription() {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .customerKey(CUSTOMER_KEY)
                .build();
        ReflectionTestUtils.setField(subscription, "id", 1L);
        return subscription;
    }

    private Payment createTestPayment() {
        Payment payment = Payment.builder()
                .user(testUser)
                .orderId(ORDER_ID)
                .amount(9900)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .plan(SubscriptionPlan.PRO)
                .billingCycle(BillingCycle.MONTHLY)
                .customerKey(CUSTOMER_KEY)
                .build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }

    @Nested
    @DisplayName("checkout")
    class CheckoutTest {

        @Test
        @DisplayName("결제 세션을 생성한다")
        void checkout_success() {
            // given
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();
            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(false);
            given(paymentDomainService.createInitialPayment(eq(testUser), eq(SubscriptionPlan.PRO),
                    eq(BillingCycle.MONTHLY), anyString(), eq(9900))).willReturn(testPayment);

            // when
            CheckoutResponse result = paymentService.checkout(testUser, request);

            // then
            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.amount()).isEqualTo(9900);
            assertThat(result.orderName()).contains("프로", "월간");
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void checkout_alreadySubscribed_throwsException() {
            // given
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();
            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> paymentService.checkout(testUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("무료 플랜은 구매할 수 없다")
        void checkout_freePlan_throwsException() {
            // given
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.FREE)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();

            // when & then
            assertThatThrownBy(() -> paymentService.checkout(testUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }

        @Test
        @DisplayName("연간 구독은 99000원이다")
        void checkout_yearlyPlan_correctAmount() {
            // given
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.YEARLY)
                    .sample();
            Payment yearlyPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(99000)
                    .status(PaymentStatus.PENDING)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(yearlyPayment, "id", 1L);

            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(false);
            given(paymentDomainService.createInitialPayment(eq(testUser), eq(SubscriptionPlan.PRO),
                    eq(BillingCycle.YEARLY), anyString(), eq(99000))).willReturn(yearlyPayment);

            // when
            CheckoutResponse result = paymentService.checkout(testUser, request);

            // then
            assertThat(result.amount()).isEqualTo(99000);
            assertThat(result.orderName()).contains("연간");
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPaymentTest {

        @Test
        @DisplayName("결제를 확정한다")
        void confirmPayment_success() {
            // given
            Payment yearlyPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(9900)
                    .status(PaymentStatus.PENDING)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(yearlyPayment, "id", 1L);

            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", PAYMENT_KEY)
                    .set("orderId", ORDER_ID)
                    .set("amount", 9900)
                    .sample();
            TossConfirmResponse tossResponse = new TossConfirmResponse(
                    PAYMENT_KEY, ORDER_ID, "프로 월간 구독", "DONE",
                    9900, "카드", null, null, null, null
            );

            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(yearlyPayment);
            given(tossPaymentService.confirmPayment(PAYMENT_KEY, ORDER_ID, 9900)).willReturn(tossResponse);
            given(subscriptionDomainService.createSubscriptionFromPayment(eq(yearlyPayment), isNull()))
                    .willReturn(testSubscription);

            // when
            SubscriptionResponse result = paymentService.confirmPayment(testUser, request);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PRO);
            verify(paymentDomainService).completePayment(yearlyPayment, PAYMENT_KEY, "카드");
            verify(subscriptionDomainService).createSubscriptionFromPayment(eq(yearlyPayment), isNull());
        }

        @Test
        @DisplayName("다른 사용자의 결제는 확정할 수 없다")
        void confirmPayment_wrongUser_throwsException() {
            // given
            User anotherUser = User.builder()
                    .email("another@example.com")
                    .password("password123")
                    .nickname("다른유저")
                    .build();
            ReflectionTestUtils.setField(anotherUser, "id", 2L);

            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", PAYMENT_KEY)
                    .set("orderId", ORDER_ID)
                    .set("amount", 3900)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(testPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(anotherUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("금액이 일치하지 않으면 예외를 던진다")
        void confirmPayment_amountMismatch_throwsException() {
            // given
            Payment yearlyPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(9900)
                    .status(PaymentStatus.PENDING)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(yearlyPayment, "id", 1L);

            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", PAYMENT_KEY)
                    .set("orderId", ORDER_ID)
                    .set("amount", 5000)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(yearlyPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(testUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("이미 완료된 결제면 기존 구독을 반환한다")
        void confirmPayment_alreadyCompleted_returnsExistingSubscription() {
            // given
            Payment completedPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(9900)
                    .status(PaymentStatus.COMPLETED)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(completedPayment, "id", 1L);

            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", PAYMENT_KEY)
                    .set("orderId", ORDER_ID)
                    .set("amount", 9900)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(completedPayment);
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when
            SubscriptionResponse result = paymentService.confirmPayment(testUser, request);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("월간 결제는 일반 결제 확인을 지원하지 않는다")
        void confirmPayment_monthlyPayment_throwsException() {
            // given
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(testPayment);
            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", PAYMENT_KEY)
                    .set("orderId", ORDER_ID)
                    .set("amount", 9900)
                    .sample();

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(testUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_SUPPORTED_FOR_CYCLE);
        }
    }

    @Nested
    @DisplayName("confirmBilling")
    class ConfirmBillingTest {

        private static final String AUTH_KEY = "auth_test123";
        private static final String BILLING_KEY = "billing_test123";

        @Test
        @DisplayName("빌링 결제를 확정한다")
        void confirmBilling_success() {
            // given
            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", CUSTOMER_KEY)
                    .set("orderId", ORDER_ID)
                    .sample();
            TossBillingAuthResponse billingAuthResponse = new TossBillingAuthResponse(
                    BILLING_KEY, CUSTOMER_KEY, "2024-01-01T10:00:00", "카드", null);
            TossConfirmResponse billingResponse = new TossConfirmResponse(
                    PAYMENT_KEY, ORDER_ID, "프로 월간 구독", "DONE",
                    9900, "카드", null, null, null, null);

            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(testPayment);
            given(tossPaymentService.issueBillingKey(AUTH_KEY, CUSTOMER_KEY)).willReturn(billingAuthResponse);
            given(tossPaymentService.billingPayment(eq(BILLING_KEY), eq(CUSTOMER_KEY),
                    eq(ORDER_ID), eq(9900), anyString())).willReturn(billingResponse);
            given(subscriptionDomainService.createSubscriptionFromPayment(testPayment, BILLING_KEY))
                    .willReturn(testSubscription);

            // when
            SubscriptionResponse result = paymentService.confirmBilling(testUser, request);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PRO);
            verify(paymentDomainService).completePayment(testPayment, PAYMENT_KEY, "카드");
            verify(subscriptionDomainService).createSubscriptionFromPayment(testPayment, BILLING_KEY);
        }

        @Test
        @DisplayName("다른 사용자의 결제는 확정할 수 없다")
        void confirmBilling_wrongUser_throwsException() {
            // given
            User anotherUser = User.builder()
                    .email("another@example.com")
                    .password("password123")
                    .nickname("다른유저")
                    .build();
            ReflectionTestUtils.setField(anotherUser, "id", 2L);

            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", CUSTOMER_KEY)
                    .set("orderId", ORDER_ID)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(testPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmBilling(anotherUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("customerKey가 일치하지 않으면 예외를 던진다")
        void confirmBilling_customerKeyMismatch_throwsException() {
            // given
            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", "WRONG_CUSTOMER_KEY")
                    .set("orderId", ORDER_ID)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(testPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmBilling(testUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_CUSTOMER_KEY_MISMATCH);

            verify(tossPaymentService, never()).issueBillingKey(anyString(), anyString());
        }

        @Test
        @DisplayName("이미 완료된 결제면 기존 구독을 반환한다")
        void confirmBilling_alreadyCompleted_returnsExistingSubscription() {
            // given
            Payment completedPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(3900)
                    .status(PaymentStatus.COMPLETED)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(completedPayment, "id", 1L);

            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", CUSTOMER_KEY)
                    .set("orderId", ORDER_ID)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(completedPayment);
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when
            SubscriptionResponse result = paymentService.confirmBilling(testUser, request);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("PENDING이 아닌 상태면 예외를 던진다")
        void confirmBilling_notPending_throwsException() {
            // given
            Payment canceledPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(3900)
                    .status(PaymentStatus.CANCELED)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(canceledPayment, "id", 1L);

            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", CUSTOMER_KEY)
                    .set("orderId", ORDER_ID)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(canceledPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmBilling(testUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("연간 결제는 빌링 결제를 지원하지 않는다")
        void confirmBilling_yearlyPayment_throwsException() {
            // given
            Payment yearlyPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(99000)
                    .status(PaymentStatus.PENDING)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(yearlyPayment, "id", 1L);

            ConfirmBillingRequest request = fixtureMonkey.giveMeBuilder(ConfirmBillingRequest.class)
                    .set("authKey", AUTH_KEY)
                    .set("customerKey", CUSTOMER_KEY)
                    .set("orderId", ORDER_ID)
                    .sample();
            given(paymentDomainService.getPaymentByOrderIdForUpdate(ORDER_ID)).willReturn(yearlyPayment);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmBilling(testUser, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.BILLING_NOT_SUPPORTED_FOR_CYCLE);
        }
    }

    @Nested
    @DisplayName("getPaymentHistory")
    class GetPaymentHistoryTest {

        @Test
        @DisplayName("결제 내역을 조회한다")
        void getPaymentHistory_success() {
            // given
            Payment completedPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(3900)
                    .status(PaymentStatus.COMPLETED)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(completedPayment, "id", 1L);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> paymentPage = new PageImpl<>(List.of(completedPayment), pageable, 1);

            given(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, PaymentStatus.COMPLETED, pageable)).willReturn(paymentPage);

            // when
            Page<PaymentHistoryResponse> result = paymentService.getPaymentHistory(testUser, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(ORDER_ID);
            assertThat(result.getContent().get(0).amount()).isEqualTo(3900);
        }
    }
}
