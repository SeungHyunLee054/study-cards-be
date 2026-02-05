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

class PasswordResetCodeServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PasswordResetCodeService passwordResetCodeService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String CODE_PREFIX = "password-reset:code:";
    private static final String ATTEMPTS_PREFIX = "password-reset:attempts:";

    @BeforeEach
    void setUp() {
        Set<String> codeKeys = redisTemplate.keys(CODE_PREFIX + "*");
        Set<String> attemptsKeys = redisTemplate.keys(ATTEMPTS_PREFIX + "*");
        if (codeKeys != null && !codeKeys.isEmpty()) {
            redisTemplate.delete(codeKeys);
        }
        if (attemptsKeys != null && !attemptsKeys.isEmpty()) {
            redisTemplate.delete(attemptsKeys);
        }
    }

    @Nested
    @DisplayName("generateAndSaveCode")
    class GenerateAndSaveCodeTest {

        @Test
        @DisplayName("6자리 숫자 코드를 생성한다")
        void generateAndSaveCode_returns6DigitCode() {
            // when
            String code = passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // then
            assertThat(code).hasSize(6);
            assertThat(code).matches("\\d{6}");
        }

        @Test
        @DisplayName("코드를 Redis에 저장한다")
        void generateAndSaveCode_savesToRedis() {
            // when
            String code = passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // then
            String key = CODE_PREFIX + TEST_EMAIL;
            Object storedCode = redisTemplate.opsForValue().get(key);
            assertThat(storedCode).isNotNull();
            assertThat(storedCode.toString()).isEqualTo(code);
        }

        @Test
        @DisplayName("코드에 TTL이 설정된다")
        void generateAndSaveCode_setsTtl() {
            // when
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // then
            String key = CODE_PREFIX + TEST_EMAIL;
            Long ttl = redisTemplate.getExpire(key);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
            assertThat(ttl).isLessThanOrEqualTo(300);
        }

        @Test
        @DisplayName("새 코드 생성 시 시도 횟수가 초기화된다")
        void generateAndSaveCode_resetsAttempts() {
            // given
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);
            passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");
            passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");

            // when
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // then
            String attemptsKey = ATTEMPTS_PREFIX + TEST_EMAIL;
            Object attempts = redisTemplate.opsForValue().get(attemptsKey);
            assertThat(attempts).isNull();
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCodeTest {

        @Test
        @DisplayName("올바른 코드로 검증 시 true를 반환한다")
        void verifyCode_correctCode_returnsTrue() {
            // given
            String code = passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // when
            boolean result = passwordResetCodeService.verifyCode(TEST_EMAIL, code);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잘못된 코드로 검증 시 false를 반환한다")
        void verifyCode_wrongCode_returnsFalse() {
            // given
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // when
            boolean result = passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("검증 성공 시 코드가 삭제된다")
        void verifyCode_success_deletesCode() {
            // given
            String code = passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);

            // when
            passwordResetCodeService.verifyCode(TEST_EMAIL, code);

            // then
            String key = CODE_PREFIX + TEST_EMAIL;
            Object storedCode = redisTemplate.opsForValue().get(key);
            assertThat(storedCode).isNull();
        }

        @Test
        @DisplayName("5회 초과 시도 시 코드가 삭제되고 false를 반환한다")
        void verifyCode_exceedsMaxAttempts_deletesCodeAndReturnsFalse() {
            // given
            String code = passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);
            for (int i = 0; i < 5; i++) {
                passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");
            }

            // when
            boolean result = passwordResetCodeService.verifyCode(TEST_EMAIL, code);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 코드로 검증 시 false를 반환한다")
        void verifyCode_nonExistentCode_returnsFalse() {
            // when
            boolean result = passwordResetCodeService.verifyCode(TEST_EMAIL, "123456");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasExceededAttempts")
    class HasExceededAttemptsTest {

        @Test
        @DisplayName("시도 횟수가 없으면 false를 반환한다")
        void hasExceededAttempts_noAttempts_returnsFalse() {
            // when
            boolean result = passwordResetCodeService.hasExceededAttempts(TEST_EMAIL);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("시도 횟수가 5회 미만이면 false를 반환한다")
        void hasExceededAttempts_lessThanMax_returnsFalse() {
            // given
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);
            for (int i = 0; i < 4; i++) {
                passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");
            }

            // when
            boolean result = passwordResetCodeService.hasExceededAttempts(TEST_EMAIL);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("시도 횟수가 5회 이상이면 true를 반환한다")
        void hasExceededAttempts_maxOrMore_returnsTrue() {
            // given
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);
            for (int i = 0; i < 5; i++) {
                passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");
            }

            // when
            boolean result = passwordResetCodeService.hasExceededAttempts(TEST_EMAIL);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteCode")
    class DeleteCodeTest {

        @Test
        @DisplayName("코드와 시도 횟수를 삭제한다")
        void deleteCode_deletesCodeAndAttempts() {
            // given
            passwordResetCodeService.generateAndSaveCode(TEST_EMAIL);
            passwordResetCodeService.verifyCode(TEST_EMAIL, "000000");

            // when
            passwordResetCodeService.deleteCode(TEST_EMAIL);

            // then
            String codeKey = CODE_PREFIX + TEST_EMAIL;
            String attemptsKey = ATTEMPTS_PREFIX + TEST_EMAIL;
            assertThat(redisTemplate.opsForValue().get(codeKey)).isNull();
            assertThat(redisTemplate.opsForValue().get(attemptsKey)).isNull();
        }
    }
}
