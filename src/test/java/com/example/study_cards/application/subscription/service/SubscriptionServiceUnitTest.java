package com.example.study_cards.application.subscription.service;

import com.example.study_cards.application.subscription.dto.request.CheckoutRequest;
import com.example.study_cards.application.subscription.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.subscription.dto.response.CheckoutResponse;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.dto.TossConfirmResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class SubscriptionServiceUnitTest extends BaseUnitTest {

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentService tossPaymentService;

    @InjectMocks
    private SubscriptionService subscriptionService;

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
                .plan(SubscriptionPlan.PREMIUM)
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
                .amount(3900)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .plan(SubscriptionPlan.PREMIUM)
                .billingCycle(BillingCycle.MONTHLY)
                .customerKey(CUSTOMER_KEY)
                .build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }

    @Nested
    @DisplayName("getPlans")
    class GetPlansTest {

        @Test
        @DisplayName("모든 요금제를 조회한다")
        void getPlans_success() {
            // when
            List<PlanResponse> result = subscriptionService.getPlans();

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(PlanResponse::plan)
                    .containsExactly(SubscriptionPlan.FREE, SubscriptionPlan.BASIC, SubscriptionPlan.PREMIUM);
        }

        @Test
        @DisplayName("Premium 플랜만 구매 가능하다")
        void getPlans_onlyPremiumPurchasable() {
            // when
            List<PlanResponse> result = subscriptionService.getPlans();

            // then
            assertThat(result.stream().filter(PlanResponse::isPurchasable).count()).isEqualTo(1);
            assertThat(result.stream().filter(PlanResponse::isPurchasable).findFirst().get().plan())
                    .isEqualTo(SubscriptionPlan.PREMIUM);
        }
    }

    @Nested
    @DisplayName("checkout")
    class CheckoutTest {

        @Test
        @DisplayName("결제 세션을 생성한다")
        void checkout_success() {
            // given
            CheckoutRequest request = new CheckoutRequest(SubscriptionPlan.PREMIUM, BillingCycle.MONTHLY);
            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(false);
            given(subscriptionDomainService.createInitialPayment(eq(testUser), eq(SubscriptionPlan.PREMIUM),
                    eq(BillingCycle.MONTHLY), anyString(), eq(3900))).willReturn(testPayment);

            // when
            CheckoutResponse result = subscriptionService.checkout(testUser, request);

            // then
            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.amount()).isEqualTo(3900);
            assertThat(result.orderName()).contains("프리미엄", "월간");
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void checkout_alreadySubscribed_throwsException() {
            // given
            CheckoutRequest request = new CheckoutRequest(SubscriptionPlan.PREMIUM, BillingCycle.MONTHLY);
            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> subscriptionService.checkout(testUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("무료 플랜은 구매할 수 없다")
        void checkout_freePlan_throwsException() {
            // given
            CheckoutRequest request = new CheckoutRequest(SubscriptionPlan.FREE, BillingCycle.MONTHLY);

            // when & then
            assertThatThrownBy(() -> subscriptionService.checkout(testUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }

        @Test
        @DisplayName("연간 구독은 39000원이다")
        void checkout_yearlyPlan_correctAmount() {
            // given
            CheckoutRequest request = new CheckoutRequest(SubscriptionPlan.PREMIUM, BillingCycle.YEARLY);
            Payment yearlyPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .amount(39000)
                    .status(PaymentStatus.PENDING)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PREMIUM)
                    .billingCycle(BillingCycle.YEARLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(yearlyPayment, "id", 1L);

            given(subscriptionDomainService.hasActiveSubscription(USER_ID)).willReturn(false);
            given(subscriptionDomainService.createInitialPayment(eq(testUser), eq(SubscriptionPlan.PREMIUM),
                    eq(BillingCycle.YEARLY), anyString(), eq(39000))).willReturn(yearlyPayment);

            // when
            CheckoutResponse result = subscriptionService.checkout(testUser, request);

            // then
            assertThat(result.amount()).isEqualTo(39000);
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
            ConfirmPaymentRequest request = new ConfirmPaymentRequest(PAYMENT_KEY, ORDER_ID, 3900);
            TossConfirmResponse tossResponse = new TossConfirmResponse(
                    PAYMENT_KEY, ORDER_ID, "프리미엄 월간 구독", "DONE",
                    3900, "카드", null, null, null, null
            );

            given(subscriptionDomainService.getPaymentByOrderId(ORDER_ID)).willReturn(testPayment);
            given(tossPaymentService.confirmPayment(PAYMENT_KEY, ORDER_ID, 3900)).willReturn(tossResponse);
            given(subscriptionDomainService.createSubscriptionFromPayment(eq(testPayment), isNull()))
                    .willReturn(testSubscription);

            // when
            SubscriptionResponse result = subscriptionService.confirmPayment(testUser, request);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PREMIUM);
            verify(subscriptionDomainService).completePayment(testPayment, PAYMENT_KEY, "카드");
            verify(subscriptionDomainService).createSubscriptionFromPayment(eq(testPayment), isNull());
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

            ConfirmPaymentRequest request = new ConfirmPaymentRequest(PAYMENT_KEY, ORDER_ID, 3900);
            given(subscriptionDomainService.getPaymentByOrderId(ORDER_ID)).willReturn(testPayment);

            // when & then
            assertThatThrownBy(() -> subscriptionService.confirmPayment(anotherUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("금액이 일치하지 않으면 예외를 던진다")
        void confirmPayment_amountMismatch_throwsException() {
            // given
            ConfirmPaymentRequest request = new ConfirmPaymentRequest(PAYMENT_KEY, ORDER_ID, 5000);
            given(subscriptionDomainService.getPaymentByOrderId(ORDER_ID)).willReturn(testPayment);

            // when & then
            assertThatThrownBy(() -> subscriptionService.confirmPayment(testUser, request))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    @Nested
    @DisplayName("getMySubscription")
    class GetMySubscriptionTest {

        @Test
        @DisplayName("내 구독 정보를 조회한다")
        void getMySubscription_success() {
            // given
            given(subscriptionDomainService.getSubscription(USER_ID)).willReturn(testSubscription);

            // when
            SubscriptionResponse result = subscriptionService.getMySubscription(testUser);

            // then
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PREMIUM);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("구독이 없으면 null을 반환한다")
        void getMySubscriptionOrNull_noSubscription() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when
            SubscriptionResponse result = subscriptionService.getMySubscriptionOrNull(testUser);

            // then
            assertThat(result).isNull();
        }
    }
}
