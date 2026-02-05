package com.example.study_cards.infra.redis.service;

import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_IP = "192.168.1.100";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:cards:";
    private static final int MAX_CARDS_PER_DAY = 15;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys(RATE_LIMIT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("getRemainingCards")
    class GetRemainingCardsTest {

        @Test
        @DisplayName("처음 요청 시 최대 카드 수를 반환한다")
        void getRemainingCards_firstRequest_returnsMaxCards() {
            // when
            int remaining = rateLimitService.getRemainingCards(TEST_IP);

            // then
            assertThat(remaining).isEqualTo(MAX_CARDS_PER_DAY);
        }

        @Test
        @DisplayName("카드 사용 후 남은 카드 수를 반환한다")
        void getRemainingCards_afterUsage_returnsRemainingCount() {
            // given
            rateLimitService.incrementCardCount(TEST_IP, 5);

            // when
            int remaining = rateLimitService.getRemainingCards(TEST_IP);

            // then
            assertThat(remaining).isEqualTo(MAX_CARDS_PER_DAY - 5);
        }

        @Test
        @DisplayName("최대 카드 수 초과 시 0을 반환한다")
        void getRemainingCards_exceedsLimit_returnsZero() {
            // given
            rateLimitService.incrementCardCount(TEST_IP, MAX_CARDS_PER_DAY + 5);

            // when
            int remaining = rateLimitService.getRemainingCards(TEST_IP);

            // then
            assertThat(remaining).isZero();
        }
    }

    @Nested
    @DisplayName("incrementCardCount")
    class IncrementCardCountTest {

        @Test
        @DisplayName("카드 카운트를 증가시킨다")
        void incrementCardCount_increasesCount() {
            // given
            int initialRemaining = rateLimitService.getRemainingCards(TEST_IP);

            // when
            rateLimitService.incrementCardCount(TEST_IP, 3);

            // then
            int remaining = rateLimitService.getRemainingCards(TEST_IP);
            assertThat(remaining).isEqualTo(initialRemaining - 3);
        }

        @Test
        @DisplayName("0 이하의 카운트는 무시한다")
        void incrementCardCount_zeroOrNegative_doesNothing() {
            // given
            int initialRemaining = rateLimitService.getRemainingCards(TEST_IP);

            // when
            rateLimitService.incrementCardCount(TEST_IP, 0);
            rateLimitService.incrementCardCount(TEST_IP, -5);

            // then
            int remaining = rateLimitService.getRemainingCards(TEST_IP);
            assertThat(remaining).isEqualTo(initialRemaining);
        }

        @Test
        @DisplayName("여러 번 증가시키면 누적된다")
        void incrementCardCount_multipleTimes_accumulates() {
            // when
            rateLimitService.incrementCardCount(TEST_IP, 3);
            rateLimitService.incrementCardCount(TEST_IP, 4);
            rateLimitService.incrementCardCount(TEST_IP, 2);

            // then
            int remaining = rateLimitService.getRemainingCards(TEST_IP);
            assertThat(remaining).isEqualTo(MAX_CARDS_PER_DAY - 9);
        }

        @Test
        @DisplayName("IP별로 독립적으로 카운트한다")
        void incrementCardCount_independentByIp() {
            // given
            String ip1 = "192.168.1.1";
            String ip2 = "192.168.1.2";

            // when
            rateLimitService.incrementCardCount(ip1, 5);
            rateLimitService.incrementCardCount(ip2, 10);

            // then
            assertThat(rateLimitService.getRemainingCards(ip1)).isEqualTo(MAX_CARDS_PER_DAY - 5);
            assertThat(rateLimitService.getRemainingCards(ip2)).isEqualTo(MAX_CARDS_PER_DAY - 10);
        }

        @Test
        @DisplayName("Redis에 TTL이 설정된다")
        void incrementCardCount_setsTtl() {
            // when
            rateLimitService.incrementCardCount(TEST_IP, 1);

            // then
            String key = RATE_LIMIT_PREFIX + TEST_IP;
            Long ttl = redisTemplate.getExpire(key);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
        }
    }
}
