package com.example.study_cards.application.subscription.service;

import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
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
import static org.mockito.BDDMockito.given;

class SubscriptionServiceUnitTest extends BaseUnitTest {

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;
    private Subscription testSubscription;

    private static final Long USER_ID = 1L;
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
        ReflectionTestUtils.setField(subscription, "id", 1L);
        return subscription;
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
            assertThat(result).hasSize(2);
            assertThat(result).extracting(PlanResponse::plan)
                    .containsExactly(SubscriptionPlan.FREE, SubscriptionPlan.PRO);
        }

        @Test
        @DisplayName("Pro 플랜만 구매 가능하다")
        void getPlans_onlyProPurchasable() {
            // when
            List<PlanResponse> result = subscriptionService.getPlans();

            // then
            assertThat(result.stream().filter(PlanResponse::isPurchasable).count()).isEqualTo(1);
            assertThat(result.stream().filter(PlanResponse::isPurchasable).findFirst().get().plan())
                    .isEqualTo(SubscriptionPlan.PRO);
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
            assertThat(result.plan()).isEqualTo(SubscriptionPlan.PRO);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("구독이 없으면 null을 반환한다")
        void getMySubscriptionOrNull_noSubscription() {
            // given
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());

            // when
            SubscriptionResponse result = subscriptionService.getMySubscriptionOrNull(testUser);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("cancelSubscription")
    class CancelSubscriptionTest {

        @Test
        @DisplayName("비활성 구독 취소 시 예외를 던진다")
        void cancelSubscription_notActive_throwsException() {
            // given
            Subscription canceledSubscription = Subscription.builder()
                    .user(testUser)
                    .plan(SubscriptionPlan.PRO)
                    .status(SubscriptionStatus.CANCELED)
                    .billingCycle(BillingCycle.MONTHLY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .customerKey(CUSTOMER_KEY)
                    .build();
            ReflectionTestUtils.setField(canceledSubscription, "id", 1L);

            given(subscriptionDomainService.getSubscription(USER_ID)).willReturn(canceledSubscription);

            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(testUser, null))
                    .isInstanceOf(SubscriptionException.class)
                    .extracting(e -> ((SubscriptionException) e).getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
    }
}
