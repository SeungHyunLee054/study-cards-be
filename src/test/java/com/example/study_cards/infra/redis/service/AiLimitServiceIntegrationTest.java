package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AiLimitServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AiLimitService aiLimitService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AiGenerationLogRepository aiGenerationLogRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String AI_DAILY_KEY = "ai_generation:" + 888L;

    private User testUser;

    @BeforeEach
    void setUp() {
        aiGenerationLogRepository.deleteAll();
        redisTemplate.delete(AI_DAILY_KEY);

        testUser = userRepository.save(User.builder()
                .email("ailimit-test@example.com")
                .password("password123")
                .nickname("aiLimitTestUser")
                .build());
    }

    private void insertAiGenerationLogs(int count) {
        for (int i = 0; i < count; i++) {
            aiGenerationLogRepository.save(AiGenerationLog.builder()
                    .user(testUser)
                    .type(AiGenerationType.USER_CARD)
                    .success(true)
                    .build());
        }
    }

    @Nested
    @DisplayName("canGenerate")
    class CanGenerateTest {

        @Test
        @DisplayName("FREE 플랜 - 초기 상태에서 생성 가능")
        void canGenerate_freePlan_initial_returnsTrue() {
            // when
            boolean result = aiLimitService.canGenerate(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("PRO 플랜 - 초기 상태에서 생성 가능")
        void canGenerate_proPlan_initial_returnsTrue() {
            // when
            boolean result = aiLimitService.canGenerate(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("FREE 플랜 - 평생 5회 초과 시 생성 불가")
        void canGenerate_freePlan_exceedLifetimeLimit_returnsFalse() {
            // given - DB에 5개의 성공 로그 삽입
            insertAiGenerationLogs(5);

            // when
            boolean result = aiLimitService.canGenerate(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("PRO 플랜 - 일일 30회 초과 시 생성 불가")
        void canGenerate_proPlan_exceedDailyLimit_returnsFalse() {
            // given - tryAcquireSlot으로 30회 선점
            for (int i = 0; i < 30; i++) {
                aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);
            }

            // when
            boolean result = aiLimitService.canGenerate(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("PRO 플랜 - 한도 내 생성 가능")
        void canGenerate_proPlan_withinLimit_returnsTrue() {
            // given - tryAcquireSlot으로 10회 선점
            for (int i = 0; i < 10; i++) {
                aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);
            }

            // when
            boolean result = aiLimitService.canGenerate(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getUsedCount")
    class GetUsedCountTest {

        @Test
        @DisplayName("FREE 플랜 - 초기 상태에서 0 반환")
        void getUsedCount_freePlan_initial_returnsZero() {
            // when
            int count = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("PRO 플랜 - 슬롯 선점 후 카운트 반환")
        void getUsedCount_proPlan_afterAcquireSlot() {
            // given
            aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);
            aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);

            // when
            int count = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("FREE 플랜 - DB 로그 기반으로 사용량 조회")
        void getUsedCount_freePlan_fromDatabase() {
            // given
            insertAiGenerationLogs(3);

            // when
            int count = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getRemainingCount")
    class GetRemainingCountTest {

        @Test
        @DisplayName("FREE 플랜 - 초기 상태에서 5회 남음")
        void getRemainingCount_freePlan_initial() {
            // when
            int remaining = aiLimitService.getRemainingCount(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(remaining).isEqualTo(5);
        }

        @Test
        @DisplayName("PRO 플랜 - 초기 상태에서 30회 남음")
        void getRemainingCount_proPlan_initial() {
            // when
            int remaining = aiLimitService.getRemainingCount(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(remaining).isEqualTo(30);
        }

        @Test
        @DisplayName("FREE 플랜 - 3회 사용 후 2회 남음")
        void getRemainingCount_freePlan_afterUsage() {
            // given - DB에 3개의 성공 로그 삽입
            insertAiGenerationLogs(3);

            // when
            int remaining = aiLimitService.getRemainingCount(testUser.getId(), SubscriptionPlan.FREE);

            // then
            assertThat(remaining).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("tryAcquireSlot / releaseSlot")
    class TryAcquireSlotTest {

        @Test
        @DisplayName("PRO 플랜 - 슬롯 선점 시 Redis에 저장 (TTL 있음)")
        void tryAcquireSlot_proPlan_hasTtl() {
            // when
            boolean result = aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(result).isTrue();
            Long ttl = redisTemplate.getExpire("ai_generation:" + testUser.getId());
            assertThat(ttl).isGreaterThan(0L);
        }

        @Test
        @DisplayName("PRO 플랜 - 한도 초과 시 슬롯 선점 실패 및 롤백")
        void tryAcquireSlot_proPlan_exceedLimit_returnsFalse() {
            // given - 30회 선점
            for (int i = 0; i < 30; i++) {
                aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);
            }

            // when - 31번째 시도
            boolean result = aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(result).isFalse();
            // 롤백되어 카운트는 30 유지
            int count = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.PRO);
            assertThat(count).isEqualTo(30);
        }

        @Test
        @DisplayName("PRO 플랜 - releaseSlot으로 슬롯 해제")
        void releaseSlot_proPlan_decrementsCount() {
            // given
            aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);
            aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);

            // when
            aiLimitService.releaseSlot(testUser.getId(), SubscriptionPlan.PRO);

            // then
            int count = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.PRO);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("FREE는 DB, PRO는 Redis로 별도 관리")
        void tryAcquireSlot_separateSources() {
            // given - FREE: DB에 로그 1건 삽입
            insertAiGenerationLogs(1);
            // PRO: Redis에 슬롯 1 선점
            aiLimitService.tryAcquireSlot(testUser.getId(), SubscriptionPlan.PRO);

            // when
            int freeCount = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.FREE);
            int proCount = aiLimitService.getUsedCount(testUser.getId(), SubscriptionPlan.PRO);

            // then
            assertThat(freeCount).isEqualTo(1);
            assertThat(proCount).isEqualTo(1);
        }
    }
}
