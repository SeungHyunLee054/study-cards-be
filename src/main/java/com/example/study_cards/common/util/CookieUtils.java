package com.example.study_cards.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtils {

    private CookieUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * 요청에서 특정 이름의 쿠키 값을 조회합니다.
     *
     * @param request HTTP 요청
     * @param name 쿠키 이름
     * @return 쿠키 값 (존재하지 않으면 empty)
     */
    public static Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * ResponseCookie를 생성하여 응답에 추가합니다.
     * HttpOnly, Secure, SameSite=None 속성이 자동으로 설정됩니다.
     *
     * @param response HTTP 응답
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAgeSeconds 쿠키 만료 시간 (초)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .httpOnly(true)
                .secure(true)
                .sameSite("None")  // 크로스 도메인 지원
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 쿠키를 삭제합니다 (Max-Age=0으로 설정).
     *
     * @param response HTTP 응답
     * @param name 삭제할 쿠키 이름
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .secure(true)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 객체를 Base64 인코딩된 문자열로 직렬화합니다.
     *
     * @param object 직렬화할 객체
     * @return Base64 인코딩된 문자열
     */
    public static String serialize(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return Base64.getUrlEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize object", e);
        }
    }

    /**
     * Base64 인코딩된 문자열을 객체로 역직렬화합니다.
     *
     * @param cookieValue Base64 인코딩된 쿠키 값
     * @param cls 반환할 클래스 타입
     * @param <T> 반환 타입
     * @return 역직렬화된 객체
     */
    public static <T> T deserialize(String cookieValue, Class<T> cls) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(cookieValue);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return cls.cast(ois.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to deserialize object", e);
        }
    }
}
