package com.example.study_cards.infra.security.jwt;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.support.BaseUnitTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

class JwtTokenProviderTest extends BaseUnitTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-provider-test-must-be-256-bits";
    private static final String TEST_ISSUER = "test-issuer";
    private static final int ACCESS_TOKEN_EXPIRE_MINUTES = 15;
    private static final int REFRESH_TOKEN_EXPIRE_DAYS = 14;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtProperties.getIssuer()).thenReturn(TEST_ISSUER);
        lenient().when(jwtProperties.getAccessTokenExpireMinutes()).thenReturn(ACCESS_TOKEN_EXPIRE_MINUTES);
        lenient().when(jwtProperties.getRefreshTokenExpireDays()).thenReturn(REFRESH_TOKEN_EXPIRE_DAYS);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessTokenTest {

        @Test
        @DisplayName("올바른 클레임으로 액세스 토큰을 생성한다")
        void createAccessToken_withValidClaims() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            Role role = Role.ROLE_USER;

            // when
            String token = jwtTokenProvider.createAccessToken(userId, email, role);

            // then
            assertThat(token).isNotBlank();
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
            assertThat(jwtTokenProvider.getEmail(token)).isEqualTo(email);
            assertThat(jwtTokenProvider.getRole(token)).isEqualTo(role);
        }

        @ParameterizedTest
        @EnumSource(Role.class)
        @DisplayName("모든 역할에 대해 토큰을 생성한다")
        void createAccessToken_withAllRoles(Role role) {
            // given
            Long userId = 1L;
            String email = "test@example.com";

            // when
            String token = jwtTokenProvider.createAccessToken(userId, email, role);

            // then
            assertThat(jwtTokenProvider.getRole(token)).isEqualTo(role);
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("userId만 포함한 리프레시 토큰을 생성한다")
        void createRefreshToken_containsOnlyUserId() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);

            // then
            assertThat(token).isNotBlank();
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰은 검증을 통과한다")
        void validateToken_withValidToken() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", Role.ROLE_USER);

            // when & then
            assertThatCode(() -> jwtTokenProvider.validateToken(token))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("만료된 토큰은 EXPIRED_TOKEN 예외를 발생시킨다")
        void validateToken_withExpiredToken() {
            // given
            SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                    .issuer(TEST_ISSUER)
                    .subject("1")
                    .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(secretKey)
                    .compact();

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
                    });
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 MALFORMED_TOKEN 예외를 발생시킨다")
        void validateToken_withMalformedToken() {
            // given
            String malformedToken = "invalid.token.format";

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(malformedToken))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
                    });
        }

        @Test
        @DisplayName("잘못된 서명의 토큰은 INVALID_SIGNATURE 예외를 발생시킨다")
        void validateToken_withInvalidSignature() {
            // given
            String differentSecret = "different-secret-key-for-jwt-token-provider-test-256-bits!";
            SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));
            String tokenWithDifferentSignature = Jwts.builder()
                    .issuer(TEST_ISSUER)
                    .subject("1")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(differentKey)
                    .compact();

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(tokenWithDifferentSignature))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_SIGNATURE);
                    });
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserIdTest {

        @Test
        @DisplayName("토큰에서 userId를 추출한다")
        void getUserId_extractsUserId() {
            // given
            Long userId = 123L;
            String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", Role.ROLE_USER);

            // when
            Long extractedUserId = jwtTokenProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getEmail")
    class GetEmailTest {

        @Test
        @DisplayName("토큰에서 email을 추출한다")
        void getEmail_extractsEmail() {
            // given
            String email = "test@example.com";
            String token = jwtTokenProvider.createAccessToken(1L, email, Role.ROLE_USER);

            // when
            String extractedEmail = jwtTokenProvider.getEmail(token);

            // then
            assertThat(extractedEmail).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("getRole")
    class GetRoleTest {

        @Test
        @DisplayName("토큰에서 role을 추출한다")
        void getRole_extractsRole() {
            // given
            Role role = Role.ROLE_ADMIN;
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", role);

            // when
            Role extractedRole = jwtTokenProvider.getRole(token);

            // then
            assertThat(extractedRole).isEqualTo(role);
        }
    }

    @Nested
    @DisplayName("getRemainingExpiration")
    class GetRemainingExpirationTest {

        @Test
        @DisplayName("토큰의 남은 만료 시간을 반환한다")
        void getRemainingExpiration_returnsRemainingTime() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", Role.ROLE_USER);

            // when
            long remainingExpiration = jwtTokenProvider.getRemainingExpiration(token);

            // then
            long expectedMaxExpiration = ACCESS_TOKEN_EXPIRE_MINUTES * 60 * 1000L;
            assertThat(remainingExpiration).isPositive();
            assertThat(remainingExpiration).isLessThanOrEqualTo(expectedMaxExpiration);
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpirationMs")
    class GetAccessTokenExpirationMsTest {

        @Test
        @DisplayName("액세스 토큰 만료 시간을 밀리초로 반환한다")
        void getAccessTokenExpirationMs_returnsMilliseconds() {
            // when
            long expirationMs = jwtTokenProvider.getAccessTokenExpirationMs();

            // then
            long expectedMs = ACCESS_TOKEN_EXPIRE_MINUTES * 60 * 1000L;
            assertThat(expirationMs).isEqualTo(expectedMs);
        }
    }

    @Nested
    @DisplayName("getRefreshTokenExpirationMs")
    class GetRefreshTokenExpirationMsTest {

        @Test
        @DisplayName("리프레시 토큰 만료 시간을 밀리초로 반환한다")
        void getRefreshTokenExpirationMs_returnsMilliseconds() {
            // when
            long expirationMs = jwtTokenProvider.getRefreshTokenExpirationMs();

            // then
            long expectedMs = REFRESH_TOKEN_EXPIRE_DAYS * 24 * 60 * 60 * 1000L;
            assertThat(expirationMs).isEqualTo(expectedMs);
        }
    }
}
