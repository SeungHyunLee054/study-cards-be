package com.example.study_cards.domain.subscription.service;

import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class SubscriptionDomainServiceTest extends BaseUnitTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private SubscriptionDomainService subscriptionDomainService;

    private User testUser;
    private Subscription testSubscription;
    private Payment testPayment;

    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 1L;
    private static final String CUSTOMER_KEY = "CK_TEST123";

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
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        return subscription;
    }

    private Payment createTestPayment() {
        Payment payment = Payment.builder()
                .user(testUser)
                .subscription(testSubscription)
                .orderId("ORDER_TEST123")
                .amount(3900)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }

    @Nested
    @DisplayName("createSubscription")
    class CreateSubscriptionTest {

        @Test
        @DisplayName("새 구독을 생성한다")
        void createSubscription_success() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(false);
            given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
                Subscription saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", SUBSCRIPTION_ID);
                return saved;
            });

            // when
            Subscription result = subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.PREMIUM,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY
            );

            // then
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void createSubscription_alreadyExists_throwsException() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.PREMIUM,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("무료 플랜은 구매할 수 없다")
        void createSubscription_freePlan_throwsException() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.FREE,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }

        @Test
        @DisplayName("기본 플랜은 구매할 수 없다")
        void createSubscription_basicPlan_throwsException() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.BASIC,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.BASIC_PLAN_NOT_PURCHASABLE);
        }
    }

    @Nested
    @DisplayName("getSubscription")
    class GetSubscriptionTest {

        @Test
        @DisplayName("사용자 ID로 구독을 조회한다")
        void getSubscription_success() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when
            Subscription result = subscriptionDomainService.getSubscription(USER_ID);

            // then
            assertThat(result.getId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
        }

        @Test
        @DisplayName("구독이 없으면 예외를 던진다")
        void getSubscription_notFound_throwsException() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.getSubscription(USER_ID))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("cancelSubscription")
    class CancelSubscriptionTest {

        @Test
        @DisplayName("구독을 취소한다")
        void cancelSubscription_success() {
            // given
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

            // when
            subscriptionDomainService.cancelSubscription(testSubscription);

            // then
            assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
            verify(subscriptionRepository).save(testSubscription);
        }

        @Test
        @DisplayName("이미 취소된 구독을 취소하면 예외를 던진다")
        void cancelSubscription_alreadyCanceled_throwsException() {
            // given
            testSubscription.cancel();

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.cancelSubscription(testSubscription))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED);
        }
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePaymentTest {

        @Test
        @DisplayName("결제를 생성한다")
        void createPayment_success() {
            // given
            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
                Payment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            Payment result = subscriptionDomainService.createPayment(
                    testUser,
                    testSubscription,
                    3900,
                    PaymentType.INITIAL
            );

            // then
            assertThat(result.getAmount()).isEqualTo(3900);
            assertThat(result.getType()).isEqualTo(PaymentType.INITIAL);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getOrderId()).startsWith("ORDER_");
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("completePayment")
    class CompletePaymentTest {

        @Test
        @DisplayName("결제를 완료한다")
        void completePayment_success() {
            // given
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);
            String paymentKey = "pk_test123";
            String method = "카드";

            // when
            subscriptionDomainService.completePayment(testPayment, paymentKey, method);

            // then
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(testPayment.getPaymentKey()).isEqualTo(paymentKey);
            assertThat(testPayment.getMethod()).isEqualTo(method);
            verify(paymentRepository).save(testPayment);
        }

        @Test
        @DisplayName("이미 완료된 결제를 다시 완료하면 예외를 던진다")
        void completePayment_alreadyCompleted_throwsException() {
            // given
            testPayment.complete("pk_test", "카드", LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.completePayment(
                    testPayment, "pk_test2", "카드"
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("hasActiveSubscription")
    class HasActiveSubscriptionTest {

        @Test
        @DisplayName("활성 구독이 있으면 true를 반환한다")
        void hasActiveSubscription_true() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when
            boolean result = subscriptionDomainService.hasActiveSubscription(USER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("구독이 없으면 false를 반환한다")
        void hasActiveSubscription_noSubscription() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when
            boolean result = subscriptionDomainService.hasActiveSubscription(USER_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("createInitialPayment")
    class CreateInitialPaymentTest {

        @Test
        @DisplayName("초기 결제를 생성한다")
        void createInitialPayment_success() {
            // given
            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
                Payment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            Payment result = subscriptionDomainService.createInitialPayment(
                    testUser,
                    SubscriptionPlan.PREMIUM,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY,
                    3900
            );

            // then
            assertThat(result.getAmount()).isEqualTo(3900);
            assertThat(result.getType()).isEqualTo(PaymentType.INITIAL);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getCustomerKey()).isEqualTo(CUSTOMER_KEY);
            assertThat(result.getOrderId()).startsWith("ORDER_");
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("createSubscriptionFromPayment")
    class CreateSubscriptionFromPaymentTest {

        private Payment paymentWithPlan;

        @BeforeEach
        void setUp() {
            paymentWithPlan = Payment.builder()
                    .user(testUser)
                    .orderId("ORDER_TEST123")
                    .amount(3900)
                    .status(PaymentStatus.COMPLETED)
                    .type(PaymentType.INITIAL)
                    .plan(SubscriptionPlan.PREMIUM)
                    .billingCycle(BillingCycle.MONTHLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(paymentWithPlan, "id", 1L);
        }

        @Test
        @DisplayName("결제 정보로 구독을 생성한다")
        void createSubscriptionFromPayment_success() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(false);
            given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
                Subscription saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", SUBSCRIPTION_ID);
                return saved;
            });
            given(paymentRepository.save(any(Payment.class))).willReturn(paymentWithPlan);

            // when
            Subscription result = subscriptionDomainService.createSubscriptionFromPayment(
                    paymentWithPlan,
                    "billing_key_123"
            );

            // then
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCustomerKey()).isEqualTo(CUSTOMER_KEY);
            assertThat(result.getBillingKey()).isEqualTo("billing_key_123");
            verify(subscriptionRepository).save(any(Subscription.class));
            verify(paymentRepository).save(paymentWithPlan);
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void createSubscriptionFromPayment_alreadyExists_throwsException() {
            // given
            given(subscriptionRepository.existsByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscriptionFromPayment(
                    paymentWithPlan,
                    "billing_key_123"
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }
    }
}
