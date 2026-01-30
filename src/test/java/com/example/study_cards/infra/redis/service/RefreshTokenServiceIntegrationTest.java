package com.example.study_cards.infra.redis.service;

import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RefreshTokenServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "test.refresh.token";
    private static final long EXPIRATION_MS = 5000L;

    @BeforeEach
    void setUp() {
        redisTemplate.delete("refresh:" + USER_ID);
    }

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshTokenTest {

        @Test
        @DisplayName("리프레시 토큰을 Redis에 저장한다")
        void saveRefreshToken_savesToRedis() {
            // when
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // then
            String key = "refresh:" + USER_ID;
            Object storedValue = redisTemplate.opsForValue().get(key);
            assertThat(storedValue).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("리프레시 토큰을 TTL과 함께 저장한다")
        void saveRefreshToken_withTTL() {
            // when
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // then
            String key = "refresh:" + USER_ID;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isPositive();
            assertThat(ttl).isLessThanOrEqualTo(EXPIRATION_MS);
        }
    }

    @Nested
    @DisplayName("getRefreshToken")
    class GetRefreshTokenTest {

        @Test
        @DisplayName("저장된 리프레시 토큰을 조회한다")
        void getRefreshToken_returnsStoredToken() {
            // given
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // when
            Optional<String> result = refreshTokenService.getRefreshToken(USER_ID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("존재하지 않는 토큰은 empty를 반환한다")
        void getRefreshToken_withNonExistent_returnsEmpty() {
            // when
            Optional<String> result = refreshTokenService.getRefreshToken(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteRefreshToken")
    class DeleteRefreshTokenTest {

        @Test
        @DisplayName("리프레시 토큰을 삭제한다")
        void deleteRefreshToken_deletesFromRedis() {
            // given
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // when
            refreshTokenService.deleteRefreshToken(USER_ID);

            // then
            Optional<String> result = refreshTokenService.getRefreshToken(USER_ID);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshTokenTest {

        @Test
        @DisplayName("저장된 토큰과 일치하면 true를 반환한다")
        void validateRefreshToken_withMatchingToken_returnsTrue() {
            // given
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // when
            boolean result = refreshTokenService.validateRefreshToken(USER_ID, REFRESH_TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("저장된 토큰과 일치하지 않으면 false를 반환한다")
        void validateRefreshToken_withDifferentToken_returnsFalse() {
            // given
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, EXPIRATION_MS);

            // when
            boolean result = refreshTokenService.validateRefreshToken(USER_ID, "different.token");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("토큰이 존재하지 않으면 false를 반환한다")
        void validateRefreshToken_withNonExistent_returnsFalse() {
            // when
            boolean result = refreshTokenService.validateRefreshToken(999L, REFRESH_TOKEN);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 만료")
    class TTLExpirationTest {

        @Test
        @DisplayName("TTL이 만료되면 토큰이 자동으로 삭제된다")
        void refreshToken_expiresAfterTTL() {
            // given
            long shortTTL = 1000L;
            refreshTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, shortTTL);

            // when & then
            await().atMost(3, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        Optional<String> result = refreshTokenService.getRefreshToken(USER_ID);
                        assertThat(result).isEmpty();
                    });
        }
    }
}
