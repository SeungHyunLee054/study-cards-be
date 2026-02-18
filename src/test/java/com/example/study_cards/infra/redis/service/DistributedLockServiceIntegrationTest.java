package com.example.study_cards.infra.redis.service;

import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DistributedLockServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_LOCK = "test:lock";

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("lock:test:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("tryLock")
    class TryLockTest {

        @Test
        @DisplayName("락 획득에 성공한다")
        void tryLock_success() {
            // when
            boolean acquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(acquired).isTrue();

            distributedLockService.unlock(TEST_LOCK);
        }

        @Test
        @DisplayName("이미 락이 존재하면 획득에 실패한다")
        void tryLock_alreadyLocked_fails() {
            // given
            boolean acquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // when
            boolean secondAcquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(acquired).isTrue();
            assertThat(secondAcquired).isFalse();

            distributedLockService.unlock(TEST_LOCK);
        }
    }

    @Nested
    @DisplayName("unlock")
    class UnlockTest {

        @Test
        @DisplayName("락 해제 후 다시 획득할 수 있다")
        void unlock_thenReacquire_success() {
            // given
            boolean acquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
            assertThat(acquired).isTrue();
            distributedLockService.unlock(TEST_LOCK);

            // when
            boolean reAcquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(reAcquired).isTrue();

            distributedLockService.unlock(TEST_LOCK);
        }

        @Test
        @DisplayName("락 미보유 상태에서 unlock 호출 시 예외가 발생하지 않는다")
        void unlock_withoutOwnership_noException() {
            // when
            distributedLockService.unlock(TEST_LOCK);

            // then
            boolean acquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
            assertThat(acquired).isTrue();
            distributedLockService.unlock(TEST_LOCK);
        }
    }

    @Nested
    @DisplayName("TTL 만료")
    class TtlExpirationTest {

        @Test
        @DisplayName("TTL이 만료되면 락이 자동으로 해제된다")
        void ttlExpired_lockReleased() {
            // given
            distributedLockService.tryLock(TEST_LOCK, Duration.ofSeconds(1));

            // when & then
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        boolean acquired = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
                        assertThat(acquired).isTrue();
                        distributedLockService.unlock(TEST_LOCK);
                    });
        }
    }
}
