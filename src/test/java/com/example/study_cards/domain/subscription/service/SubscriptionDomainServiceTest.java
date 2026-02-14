package com.example.study_cards.domain.subscription.service;

import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.user.entity.Role;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class SubscriptionDomainServiceTest extends BaseUnitTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionDomainService subscriptionDomainService;

    private User testUser;
    private Subscription testSubscription;

    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 1L;
    private static final String CUSTOMER_KEY = "CK_TEST123";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testSubscription = createTestSubscription();
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
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        return subscription;
    }

    @Nested
    @DisplayName("createSubscription")
    class CreateSubscriptionTest {

        @Test
        @DisplayName("새 구독을 생성한다")
        void createSubscription_success() {
            // given
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(false);
            given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
                Subscription saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", SUBSCRIPTION_ID);
                return saved;
            });

            // when
            Subscription result = subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.PRO,
                    BillingCycle.MONTHLY,
                    CUSTOMER_KEY
            );

            // then
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void createSubscription_alreadyExists_throwsException() {
            // given
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscription(
                    testUser,
                    SubscriptionPlan.PRO,
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
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(false);

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
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(false);

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
    }

    @Nested
    @DisplayName("getSubscription")
    class GetSubscriptionTest {

        @Test
        @DisplayName("사용자 ID로 구독을 조회한다")
        void getSubscription_success() {
            // given
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when
            Subscription result = subscriptionDomainService.getSubscription(USER_ID);

            // then
            assertThat(result.getId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PRO);
        }

        @Test
        @DisplayName("구독이 없으면 예외를 던진다")
        void getSubscription_notFound_throwsException() {
            // given
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());

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
            subscriptionDomainService.cancelSubscription(testSubscription, "테스트 취소");

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
            assertThatThrownBy(() -> subscriptionDomainService.cancelSubscription(testSubscription, "취소 사유"))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED);
        }
    }

    @Nested
    @DisplayName("hasActiveSubscription")
    class HasActiveSubscriptionTest {

        @Test
        @DisplayName("활성 구독이 있으면 true를 반환한다")
        void hasActiveSubscription_true() {
            // given
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(true);

            // when
            boolean result = subscriptionDomainService.hasActiveSubscription(USER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("구독이 없으면 false를 반환한다")
        void hasActiveSubscription_noSubscription() {
            // given
            given(subscriptionRepository.existsActiveByUserId(USER_ID)).willReturn(false);

            // when
            boolean result = subscriptionDomainService.hasActiveSubscription(USER_ID);

            // then
            assertThat(result).isFalse();
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
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(paymentWithPlan, "id", 1L);
        }

        @Test
        @DisplayName("결제 정보로 구독을 생성한다")
        void createSubscriptionFromPayment_success() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
                Subscription saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", SUBSCRIPTION_ID);
                return saved;
            });

            // when
            Subscription result = subscriptionDomainService.createSubscriptionFromPayment(
                    paymentWithPlan,
                    "billing_key_123"
            );

            // then
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCustomerKey()).isEqualTo(CUSTOMER_KEY);
            assertThat(result.getBillingKey()).isEqualTo("billing_key_123");
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("이미 구독이 있으면 예외를 던진다")
        void createSubscriptionFromPayment_alreadyExists_throwsException() {
            // given
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.of(testSubscription));

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.createSubscriptionFromPayment(
                    paymentWithPlan,
                    "billing_key_123"
            ))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("비활성 구독이 있으면 재활성화한다")
        void createSubscriptionFromPayment_reactivatesInactiveSubscription() {
            // given
            testSubscription.cancel("기존 구독 취소");
            given(subscriptionRepository.findByUserId(USER_ID)).willReturn(Optional.of(testSubscription));
            given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Subscription result = subscriptionDomainService.createSubscriptionFromPayment(
                    paymentWithPlan,
                    "billing_key_123"
            );

            // then
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getBillingKey()).isEqualTo("billing_key_123");
            assertThat(paymentWithPlan.getSubscription()).isEqualTo(result);
            verify(subscriptionRepository).save(testSubscription);
        }
    }

    @Nested
    @DisplayName("findSubscriptionByBillingKey")
    class FindSubscriptionByBillingKeyTest {

        @Test
        @DisplayName("빌링키로 구독을 찾는다")
        void findSubscriptionByBillingKey_success() {
            // given
            given(subscriptionRepository.findByBillingKey("billing_key_123"))
                    .willReturn(Optional.of(testSubscription));

            // when
            Optional<Subscription> result = subscriptionDomainService.findSubscriptionByBillingKey("billing_key_123");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("빌링키로 구독이 없으면 빈 Optional을 반환한다")
        void findSubscriptionByBillingKey_notFound() {
            // given
            given(subscriptionRepository.findByBillingKey("nonexistent")).willReturn(Optional.empty());

            // when
            Optional<Subscription> result = subscriptionDomainService.findSubscriptionByBillingKey("nonexistent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("disableAutoRenewal")
    class DisableAutoRenewalTest {

        @Test
        @DisplayName("자동 갱신을 비활성화한다")
        void disableAutoRenewal_success() {
            // given
            testSubscription.updateBillingKey("billing_key_123");
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

            // when
            subscriptionDomainService.disableAutoRenewal(testSubscription);

            // then
            assertThat(testSubscription.getBillingKey()).isNull();
            verify(subscriptionRepository).save(testSubscription);
        }
    }

    @Nested
    @DisplayName("getEffectivePlan")
    class GetEffectivePlanTest {

        @Test
        @DisplayName("관리자는 PRO 플랜을 반환한다")
        void getEffectivePlan_admin_returnsPro() {
            // given
            testUser.addRole(Role.ROLE_ADMIN);

            // when
            SubscriptionPlan result = subscriptionDomainService.getEffectivePlan(testUser);

            // then
            assertThat(result).isEqualTo(SubscriptionPlan.PRO);
        }

        @Test
        @DisplayName("활성 구독이 있으면 해당 플랜을 반환한다")
        void getEffectivePlan_activeSubscription_returnsPlan() {
            // given
            given(subscriptionRepository.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(testSubscription));

            // when
            SubscriptionPlan result = subscriptionDomainService.getEffectivePlan(testUser);

            // then
            assertThat(result).isEqualTo(SubscriptionPlan.PRO);
        }

        @Test
        @DisplayName("구독이 없으면 FREE 플랜을 반환한다")
        void getEffectivePlan_noSubscription_returnsFree() {
            // given
            given(subscriptionRepository.findActiveByUserId(USER_ID))
                    .willReturn(Optional.empty());

            // when
            SubscriptionPlan result = subscriptionDomainService.getEffectivePlan(testUser);

            // then
            assertThat(result).isEqualTo(SubscriptionPlan.FREE);
        }
    }

    @Nested
    @DisplayName("getSubscriptionByCustomerKey")
    class GetSubscriptionByCustomerKeyTest {

        @Test
        @DisplayName("customerKey로 구독을 조회한다")
        void getSubscriptionByCustomerKey_success() {
            // given
            given(subscriptionRepository.findByCustomerKey(CUSTOMER_KEY))
                    .willReturn(Optional.of(testSubscription));

            // when
            Subscription result = subscriptionDomainService.getSubscriptionByCustomerKey(CUSTOMER_KEY);

            // then
            assertThat(result.getId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(result.getCustomerKey()).isEqualTo(CUSTOMER_KEY);
        }

        @Test
        @DisplayName("존재하지 않는 customerKey로 조회 시 예외를 던진다")
        void getSubscriptionByCustomerKey_notFound_throwsException() {
            // given
            given(subscriptionRepository.findByCustomerKey("INVALID_KEY"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionDomainService.getSubscriptionByCustomerKey("INVALID_KEY"))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("activateSubscription")
    class ActivateSubscriptionTest {

        @Test
        @DisplayName("구독을 활성화한다")
        void activateSubscription_success() {
            // given
            Subscription pendingSub = Subscription.builder()
                    .user(testUser)
                    .plan(SubscriptionPlan.PRO)
                    .status(SubscriptionStatus.PENDING)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(pendingSub, "id", SUBSCRIPTION_ID);
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(pendingSub);

            // when
            subscriptionDomainService.activateSubscription(pendingSub, "billing_key_123");

            // then
            assertThat(pendingSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(pendingSub.getBillingKey()).isEqualTo("billing_key_123");
            verify(subscriptionRepository).save(pendingSub);
        }
    }

    @Nested
    @DisplayName("renewSubscription")
    class RenewSubscriptionTest {

        @Test
        @DisplayName("구독을 갱신한다")
        void renewSubscription_success() {
            // given
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

            // when
            subscriptionDomainService.renewSubscription(testSubscription);

            // then
            assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(testSubscription.getEndDate()).isAfter(LocalDateTime.now());
            verify(subscriptionRepository).save(testSubscription);
        }
    }

    @Nested
    @DisplayName("expireSubscription")
    class ExpireSubscriptionTest {

        @Test
        @DisplayName("구독을 만료시킨다")
        void expireSubscription_success() {
            // given
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

            // when
            subscriptionDomainService.expireSubscription(testSubscription);

            // then
            assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            verify(subscriptionRepository).save(testSubscription);
        }
    }

    @Nested
    @DisplayName("findRenewableSubscriptions")
    class FindRenewableSubscriptionsTest {

        @Test
        @DisplayName("갱신 가능한 구독 목록을 반환한다")
        void findRenewableSubscriptions_returnsList() {
            // given
            given(subscriptionRepository.findRenewableSubscriptions(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(List.of(testSubscription));

            // when
            List<Subscription> result = subscriptionDomainService.findRenewableSubscriptions(3);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(SUBSCRIPTION_ID);
        }
    }

    @Nested
    @DisplayName("findExpiredSubscriptions")
    class FindExpiredSubscriptionsTest {

        @Test
        @DisplayName("만료된 구독 목록을 반환한다")
        void findExpiredSubscriptions_returnsList() {
            // given
            given(subscriptionRepository.findExpired(any(SubscriptionStatus.class), any(LocalDateTime.class)))
                    .willReturn(List.of(testSubscription));

            // when
            List<Subscription> result = subscriptionDomainService.findExpiredSubscriptions();

            // then
            assertThat(result).hasSize(1);
        }
    }
}
