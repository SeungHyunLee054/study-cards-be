package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class StudyLimitServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StudyLimitService studyLimitService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long USER_ID = 999L;
    private static final String STUDY_COUNT_KEY = "study_count:" + USER_ID;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(STUDY_COUNT_KEY);
    }

    @Nested
    @DisplayName("getRemainingStudies")
    class GetRemainingStudiesTest {

        @Test
        @DisplayName("Free 플랜 - 초기 상태에서 15개 남음")
        void getRemainingStudies_freePlan_initial() {
            // when
            int remaining = studyLimitService.getRemainingStudies(USER_ID, SubscriptionPlan.FREE);

            // then
            assertThat(remaining).isEqualTo(15);
        }

        @Test
        @DisplayName("Basic 플랜 - 초기 상태에서 100개 남음")
        void getRemainingStudies_basicPlan_initial() {
            // when
            int remaining = studyLimitService.getRemainingStudies(USER_ID, SubscriptionPlan.BASIC);

            // then
            assertThat(remaining).isEqualTo(100);
        }

        @Test
        @DisplayName("Premium 플랜 - 무제한")
        void getRemainingStudies_premiumPlan_unlimited() {
            // when
            int remaining = studyLimitService.getRemainingStudies(USER_ID, SubscriptionPlan.PREMIUM);

            // then
            assertThat(remaining).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("학습 후 남은 횟수 감소")
        void getRemainingStudies_afterIncrement() {
            // given
            studyLimitService.incrementStudyCount(USER_ID);
            studyLimitService.incrementStudyCount(USER_ID);
            studyLimitService.incrementStudyCount(USER_ID);

            // when
            int remaining = studyLimitService.getRemainingStudies(USER_ID, SubscriptionPlan.FREE);

            // then
            assertThat(remaining).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("incrementStudyCount")
    class IncrementStudyCountTest {

        @Test
        @DisplayName("학습 카운트를 증가시킨다")
        void incrementStudyCount_success() {
            // when
            studyLimitService.incrementStudyCount(USER_ID);

            // then
            assertThat(studyLimitService.getTodayStudyCount(USER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("여러 번 증가시키면 카운트가 누적된다")
        void incrementStudyCount_multiple() {
            // when
            studyLimitService.incrementStudyCount(USER_ID);
            studyLimitService.incrementStudyCount(USER_ID);
            studyLimitService.incrementStudyCount(USER_ID);

            // then
            assertThat(studyLimitService.getTodayStudyCount(USER_ID)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("canStudy")
    class CanStudyTest {

        @Test
        @DisplayName("Premium 플랜은 항상 학습 가능")
        void canStudy_premiumPlan_always() {
            // given
            for (int i = 0; i < 100; i++) {
                studyLimitService.incrementStudyCount(USER_ID);
            }

            // when
            boolean canStudy = studyLimitService.canStudy(USER_ID, SubscriptionPlan.PREMIUM);

            // then
            assertThat(canStudy).isTrue();
        }

        @Test
        @DisplayName("Free 플랜 - 한도 내에서 학습 가능")
        void canStudy_freePlan_withinLimit() {
            // given
            for (int i = 0; i < 10; i++) {
                studyLimitService.incrementStudyCount(USER_ID);
            }

            // when
            boolean canStudy = studyLimitService.canStudy(USER_ID, SubscriptionPlan.FREE);

            // then
            assertThat(canStudy).isTrue();
        }

        @Test
        @DisplayName("Free 플랜 - 한도 초과 시 학습 불가")
        void canStudy_freePlan_exceedLimit() {
            // given
            for (int i = 0; i < 15; i++) {
                studyLimitService.incrementStudyCount(USER_ID);
            }

            // when
            boolean canStudy = studyLimitService.canStudy(USER_ID, SubscriptionPlan.FREE);

            // then
            assertThat(canStudy).isFalse();
        }

        @Test
        @DisplayName("Basic 플랜 - 한도 초과 시 학습 불가")
        void canStudy_basicPlan_exceedLimit() {
            // given
            for (int i = 0; i < 100; i++) {
                studyLimitService.incrementStudyCount(USER_ID);
            }

            // when
            boolean canStudy = studyLimitService.canStudy(USER_ID, SubscriptionPlan.BASIC);

            // then
            assertThat(canStudy).isFalse();
        }
    }

    @Nested
    @DisplayName("getTodayStudyCount")
    class GetTodayStudyCountTest {

        @Test
        @DisplayName("초기 상태에서 0을 반환한다")
        void getTodayStudyCount_initial() {
            // when
            int count = studyLimitService.getTodayStudyCount(USER_ID);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("학습 후 현재 카운트를 반환한다")
        void getTodayStudyCount_afterStudy() {
            // given
            studyLimitService.incrementStudyCount(USER_ID);
            studyLimitService.incrementStudyCount(USER_ID);

            // when
            int count = studyLimitService.getTodayStudyCount(USER_ID);

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
