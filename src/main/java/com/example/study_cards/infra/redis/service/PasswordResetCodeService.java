package com.example.study_cards.infra.redis.service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PasswordResetCodeService {

    private static final String CODE_PREFIX = "password-reset:code:";
    private static final String ATTEMPTS_PREFIX = "password-reset:attempts:";
    private static final long CODE_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndSaveCode(String email) {
        String code = generateCode();
        String codeKey = CODE_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(attemptsKey);

        return code;
    }

    public boolean verifyCode(String email, String code) {
        String codeKey = CODE_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            return false;
        }

        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode != null && storedCode.toString().equals(code)) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            return true;
        }

        return false;
    }

    public boolean hasExceededAttempts(String email) {
        String attemptsKey = ATTEMPTS_PREFIX + email;
        Object attempts = redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null && Long.parseLong(attempts.toString()) >= MAX_ATTEMPTS;
    }

    public void deleteCode(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
