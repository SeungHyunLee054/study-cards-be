package com.example.study_cards.domain.payment.service;

import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.domain.payment.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;
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

class PaymentDomainServiceTest extends BaseUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentDomainService paymentDomainService;

    private User testUser;
    private Subscription testSubscription;
    private Payment testPayment;

    private static final Long USER_ID = 1L;
    private static final String ORDER_ID = "ORDER_TEST123";
    private static final String PAYMENT_KEY = "pk_test123";
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
                .amount(3900)
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
            Payment result = paymentDomainService.createPayment(testUser, testSubscription, 3900, PaymentType.RENEWAL);

            // then
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getSubscription()).isEqualTo(testSubscription);
            assertThat(result.getAmount()).isEqualTo(3900);
            assertThat(result.getType()).isEqualTo(PaymentType.RENEWAL);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getOrderId()).startsWith("ORDER_");
            verify(paymentRepository).save(any(Payment.class));
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
            Payment result = paymentDomainService.createInitialPayment(
                    testUser, SubscriptionPlan.PRO, BillingCycle.MONTHLY, CUSTOMER_KEY, 3900);

            // then
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getCustomerKey()).isEqualTo(CUSTOMER_KEY);
            assertThat(result.getAmount()).isEqualTo(3900);
            assertThat(result.getType()).isEqualTo(PaymentType.INITIAL);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getOrderId()).startsWith("ORDER_");
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetPaymentByOrderIdTest {

        @Test
        @DisplayName("주문 ID로 결제를 조회한다")
        void getPaymentByOrderId_success() {
            // given
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

            // when
            Payment result = paymentDomainService.getPaymentByOrderId(ORDER_ID);

            // then
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("결제가 없으면 예외를 던진다")
        void getPaymentByOrderId_notFound_throwsException() {
            // given
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentDomainService.getPaymentByOrderId(ORDER_ID))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getPaymentByPaymentKey")
    class GetPaymentByPaymentKeyTest {

        @Test
        @DisplayName("결제 키로 결제를 조회한다")
        void getPaymentByPaymentKey_success() {
            // given
            Payment completedPayment = Payment.builder()
                    .user(testUser)
                    .orderId(ORDER_ID)
                    .paymentKey(PAYMENT_KEY)
                    .amount(3900)
                    .status(PaymentStatus.COMPLETED)
                    .type(PaymentType.INITIAL)
                    .build();
            given(paymentRepository.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.of(completedPayment));

            // when
            Payment result = paymentDomainService.getPaymentByPaymentKey(PAYMENT_KEY);

            // then
            assertThat(result.getPaymentKey()).isEqualTo(PAYMENT_KEY);
        }

        @Test
        @DisplayName("결제가 없으면 예외를 던진다")
        void getPaymentByPaymentKey_notFound_throwsException() {
            // given
            given(paymentRepository.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentDomainService.getPaymentByPaymentKey(PAYMENT_KEY))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
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

            // when
            paymentDomainService.completePayment(testPayment, PAYMENT_KEY, "카드");

            // then
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(testPayment.getPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(testPayment.getMethod()).isEqualTo("카드");
            assertThat(testPayment.getPaidAt()).isNotNull();
            verify(paymentRepository).save(testPayment);
        }

        @Test
        @DisplayName("이미 완료된 결제면 예외를 던진다")
        void completePayment_alreadyCompleted_throwsException() {
            // given
            testPayment.complete(PAYMENT_KEY, "카드", LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> paymentDomainService.completePayment(testPayment, PAYMENT_KEY, "카드"))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("tryCompletePayment")
    class TryCompletePaymentTest {

        @Test
        @DisplayName("PENDING 상태면 완료하고 true를 반환한다")
        void tryCompletePayment_pending_returnsTrue() {
            // given
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);

            // when
            boolean result = paymentDomainService.tryCompletePayment(testPayment, PAYMENT_KEY, "카드");

            // then
            assertThat(result).isTrue();
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(paymentRepository).save(testPayment);
        }

        @Test
        @DisplayName("PENDING이 아니면 false를 반환한다")
        void tryCompletePayment_notPending_returnsFalse() {
            // given
            testPayment.complete(PAYMENT_KEY, "카드", LocalDateTime.now());

            // when
            boolean result = paymentDomainService.tryCompletePayment(testPayment, "new_key", "카드");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPaymentTest {

        @Test
        @DisplayName("결제를 취소한다")
        void cancelPayment_success() {
            // given
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);

            // when
            paymentDomainService.cancelPayment(testPayment, "고객 요청");

            // then
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(testPayment.getCancelReason()).isEqualTo("고객 요청");
            verify(paymentRepository).save(testPayment);
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPaymentTest {

        @Test
        @DisplayName("결제를 실패 처리한다")
        void failPayment_success() {
            // given
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);

            // when
            paymentDomainService.failPayment(testPayment, "결제 오류");

            // then
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testPayment.getFailReason()).isEqualTo("결제 오류");
            verify(paymentRepository).save(testPayment);
        }
    }
}
