package com.example.study_cards.infra.redis.service;

import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TokenBlacklistServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_TOKEN = "test.access.token";
    private static final long REMAINING_MS = 5000L;

    @BeforeEach
    void setUp() {
        redisTemplate.delete("bl:" + ACCESS_TOKEN);
    }

    @Nested
    @DisplayName("blacklistToken")
    class BlacklistTokenTest {

        @Test
        @DisplayName("토큰을 블랙리스트에 등록한다")
        void blacklistToken_addsToBlacklist() {
            // when
            tokenBlacklistService.blacklistToken(ACCESS_TOKEN, REMAINING_MS);

            // then
            boolean isBlacklisted = tokenBlacklistService.isBlacklisted(ACCESS_TOKEN);
            assertThat(isBlacklisted).isTrue();
        }

        @Test
        @DisplayName("TTL과 함께 블랙리스트에 등록한다")
        void blacklistToken_withTTL() {
            // when
            tokenBlacklistService.blacklistToken(ACCESS_TOKEN, REMAINING_MS);

            // then
            String key = "bl:" + ACCESS_TOKEN;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isPositive();
            assertThat(ttl).isLessThanOrEqualTo(REMAINING_MS);
        }

        @Test
        @DisplayName("남은 시간이 0 이하면 블랙리스트에 등록하지 않는다")
        void blacklistToken_withZeroOrNegativeRemaining_doesNotAdd() {
            // when
            tokenBlacklistService.blacklistToken(ACCESS_TOKEN, 0);
            tokenBlacklistService.blacklistToken("another.token", -100);

            // then
            assertThat(tokenBlacklistService.isBlacklisted(ACCESS_TOKEN)).isFalse();
            assertThat(tokenBlacklistService.isBlacklisted("another.token")).isFalse();
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklistedTest {

        @Test
        @DisplayName("블랙리스트에 있는 토큰은 true를 반환한다")
        void isBlacklisted_withBlacklistedToken_returnsTrue() {
            // given
            tokenBlacklistService.blacklistToken(ACCESS_TOKEN, REMAINING_MS);

            // when
            boolean result = tokenBlacklistService.isBlacklisted(ACCESS_TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("블랙리스트에 없는 토큰은 false를 반환한다")
        void isBlacklisted_withNonBlacklistedToken_returnsFalse() {
            // when
            boolean result = tokenBlacklistService.isBlacklisted("non.blacklisted.token");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 만료")
    class TTLExpirationTest {

        @Test
        @DisplayName("TTL이 만료되면 블랙리스트에서 자동으로 삭제된다")
        void blacklistedToken_expiresAfterTTL() {
            // given
            long shortTTL = 1000L;
            tokenBlacklistService.blacklistToken(ACCESS_TOKEN, shortTTL);

            // when & then
            await().atMost(3, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(ACCESS_TOKEN);
                        assertThat(isBlacklisted).isFalse();
                    });
        }
    }
}
