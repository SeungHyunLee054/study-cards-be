package com.example.study_cards.domain.subscription.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder()
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(20))
                .customerKey("customer_123")
                .billingKey("billing_123")
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("status 미지정 시 PENDING으로 설정된다")
        void builder_withoutStatus_defaultsToPending() {
            // when
            Subscription newSub = Subscription.builder()
                    .plan(SubscriptionPlan.FREE)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .build();

            // then
            assertThat(newSub.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        }

        @Test
        @DisplayName("모든 필드가 정상적으로 설정된다")
        void builder_allFields_setCorrectly() {
            // then
            assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(subscription.getCustomerKey()).isEqualTo("customer_123");
            assertThat(subscription.getBillingKey()).isEqualTo("billing_123");
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTest {

        @Test
        @DisplayName("ACTIVE 상태이고 만료 전이면 true를 반환한다")
        void isActive_activeAndNotExpired_returnsTrue() {
            // then
            assertThat(subscription.isActive()).isTrue();
        }

        @Test
        @DisplayName("ACTIVE 상태지만 만료되었으면 false를 반환한다")
        void isActive_activeButExpired_returnsFalse() {
            // given
            Subscription expired = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now().minusDays(40))
                    .endDate(LocalDateTime.now().minusDays(1))
                    .build();

            // then
            assertThat(expired.isActive()).isFalse();
        }

        @Test
        @DisplayName("CANCELED 상태이면 false를 반환한다")
        void isActive_canceled_returnsFalse() {
            // given
            subscription.cancel();

            // then
            assertThat(subscription.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpiredTest {

        @Test
        @DisplayName("만료일이 지나면 true를 반환한다")
        void isExpired_pastEndDate_returnsTrue() {
            // given
            Subscription expired = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now().minusDays(40))
                    .endDate(LocalDateTime.now().minusDays(1))
                    .build();

            // then
            assertThat(expired.isExpired()).isTrue();
        }

        @Test
        @DisplayName("만료일이 지나지 않으면 false를 반환한다")
        void isExpired_beforeEndDate_returnsFalse() {
            // then
            assertThat(subscription.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpiringSoon")
    class IsExpiringSoonTest {

        @Test
        @DisplayName("만료일이 지정 일수 내에 있으면 true를 반환한다")
        void isExpiringSoon_withinThreshold_returnsTrue() {
            // given
            Subscription soonExpiring = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now().minusDays(25))
                    .endDate(LocalDateTime.now().plusDays(2))
                    .build();

            // then
            assertThat(soonExpiring.isExpiringSoon(7)).isTrue();
        }

        @Test
        @DisplayName("만료일이 지정 일수를 초과하면 false를 반환한다")
        void isExpiringSoon_beyondThreshold_returnsFalse() {
            // then
            assertThat(subscription.isExpiringSoon(3)).isFalse();
        }

        @Test
        @DisplayName("이미 만료되었으면 false를 반환한다")
        void isExpiringSoon_alreadyExpired_returnsFalse() {
            // given
            Subscription expired = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now().minusDays(40))
                    .endDate(LocalDateTime.now().minusDays(1))
                    .build();

            // then
            assertThat(expired.isExpiringSoon(7)).isFalse();
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTest {

        @Test
        @DisplayName("상태를 ACTIVE로 변경한다")
        void activate_changesStatusToActive() {
            // given
            Subscription pending = Subscription.builder()
                    .plan(SubscriptionPlan.PRO)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .build();

            // when
            pending.activate();

            // then
            assertThat(pending.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("상태를 CANCELED로 변경한다")
        void cancel_changesStatusToCanceled() {
            // when
            subscription.cancel();

            // then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        }
    }

    @Nested
    @DisplayName("expire")
    class ExpireTest {

        @Test
        @DisplayName("상태를 EXPIRED로 변경한다")
        void expire_changesStatusToExpired() {
            // when
            subscription.expire();

            // then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("renew")
    class RenewTest {

        @Test
        @DisplayName("구독을 갱신하면 상태가 ACTIVE가 되고 종료일이 갱신된다")
        void renew_updatesStatusAndEndDate() {
            // given
            LocalDateTime newEndDate = LocalDateTime.now().plusMonths(1);

            // when
            subscription.renew(newEndDate);

            // then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getEndDate()).isEqualTo(newEndDate);
            assertThat(subscription.getStartDate()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("updateBillingKey")
    class UpdateBillingKeyTest {

        @Test
        @DisplayName("빌링 키를 업데이트한다")
        void updateBillingKey_changesKey() {
            // when
            subscription.updateBillingKey("new_billing_key");

            // then
            assertThat(subscription.getBillingKey()).isEqualTo("new_billing_key");
        }
    }

    @Nested
    @DisplayName("changePlan")
    class ChangePlanTest {

        @Test
        @DisplayName("플랜과 결제 주기를 변경한다")
        void changePlan_changesPlanAndCycle() {
            // when
            subscription.changePlan(SubscriptionPlan.FREE, BillingCycle.YEARLY);

            // then
            assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.FREE);
            assertThat(subscription.getBillingCycle()).isEqualTo(BillingCycle.YEARLY);
        }
    }
}
