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
            String lockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(lockValue).isNotNull();

            distributedLockService.unlock(TEST_LOCK, lockValue);
        }

        @Test
        @DisplayName("이미 락이 존재하면 획득에 실패한다")
        void tryLock_alreadyLocked_fails() {
            // given
            String lockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // when
            String secondLockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(secondLockValue).isNull();

            distributedLockService.unlock(TEST_LOCK, lockValue);
        }
    }

    @Nested
    @DisplayName("unlock")
    class UnlockTest {

        @Test
        @DisplayName("락 해제 후 다시 획득할 수 있다")
        void unlock_thenReacquire_success() {
            // given
            String lockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
            distributedLockService.unlock(TEST_LOCK, lockValue);

            // when
            String newLockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // then
            assertThat(newLockValue).isNotNull();

            distributedLockService.unlock(TEST_LOCK, newLockValue);
        }

        @Test
        @DisplayName("다른 소유자의 락은 해제할 수 없다")
        void unlock_differentOwner_fails() {
            // given
            String lockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));

            // when - 잘못된 lockValue로 해제 시도
            distributedLockService.unlock(TEST_LOCK, "wrong-value");

            // then - 락이 여전히 존재하므로 재획득 불가
            String secondLockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
            assertThat(secondLockValue).isNull();

            distributedLockService.unlock(TEST_LOCK, lockValue);
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
                        String lockValue = distributedLockService.tryLock(TEST_LOCK, Duration.ofMinutes(1));
                        assertThat(lockValue).isNotNull();
                    });
        }
    }
}
