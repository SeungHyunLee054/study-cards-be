package com.example.study_cards.application.auth.service;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.redis.vo.UserVo;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;

import com.example.study_cards.application.auth.exception.AuthErrorCode;
import com.example.study_cards.application.auth.exception.AuthException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class AuthServiceUnitTest extends BaseUnitTest {

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String NICKNAME = "testUser";
    private static final String ACCESS_TOKEN = "access.token.here";
    private static final String REFRESH_TOKEN = "refresh.token.here";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1209600000L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();

        signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", EMAIL)
                .set("password", PASSWORD)
                .set("passwordConfirm", PASSWORD)
                .set("nickname", NICKNAME)
                .sample();

        signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", EMAIL)
                .set("password", PASSWORD)
                .sample();
    }

    private User createTestUser() {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .nickname(NICKNAME)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, USER_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return user;
    }

    @Nested
    @DisplayName("signUp")
    class SignUpTest {

        @Test
        @DisplayName("회원가입 시 UserResponse를 반환한다")
        void signUp_returnsUserResponse() {
            // given
            given(userDomainService.registerUser(EMAIL, PASSWORD, NICKNAME)).willReturn(testUser);

            // when
            UserResponse response = authService.signUp(signUpRequest);

            // then
            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.nickname()).isEqualTo(NICKNAME);
            verify(userDomainService).registerUser(EMAIL, PASSWORD, NICKNAME);
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 일치하지 않으면 예외가 발생한다")
        void signUp_passwordMismatch_throwsException() {
            // given
            SignUpRequest mismatchRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                    .set("email", EMAIL)
                    .set("password", PASSWORD)
                    .set("passwordConfirm", "differentPassword")
                    .set("nickname", NICKNAME)
                    .sample();

            // when & then
            assertThatThrownBy(() -> authService.signUp(mismatchRequest))
                    .isInstanceOf(AuthException.class)
                    .satisfies(exception -> {
                        AuthException authException = (AuthException) exception;
                        assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_MISMATCH);
                    });
        }
    }

    @Nested
    @DisplayName("signIn")
    class SignInTest {

        @Test
        @DisplayName("로그인 성공 시 TokenResult를 반환한다")
        void signIn_returnsTokenResult() {
            // given
            given(userDomainService.findByEmail(EMAIL)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);

            // when
            TokenResult result = authService.signIn(signInRequest);

            // then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(result.accessTokenExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS);
        }

        @Test
        @DisplayName("로그인 시 비밀번호를 검증한다")
        void signIn_validatesPassword() {
            // given
            given(userDomainService.findByEmail(EMAIL)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);

            // when
            authService.signIn(signInRequest);

            // then
            verify(userDomainService).validatePassword(PASSWORD, ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("로그인 시 RefreshToken을 Redis에 저장한다")
        void signIn_savesRefreshTokenToRedis() {
            // given
            given(userDomainService.findByEmail(EMAIL)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);

            // when
            authService.signIn(signInRequest);

            // then
            verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MS);
        }

        @Test
        @DisplayName("로그인 시 사용자 정보를 캐시한다")
        void signIn_cachesUser() {
            // given
            given(userDomainService.findByEmail(EMAIL)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);

            // when
            authService.signIn(signInRequest);

            // then
            ArgumentCaptor<UserVo> userVoCaptor = ArgumentCaptor.forClass(UserVo.class);
            verify(userCacheService).cacheUser(userVoCaptor.capture(), eq(ACCESS_TOKEN_EXPIRATION_MS));

            UserVo cachedUserVo = userVoCaptor.getValue();
            assertThat(cachedUserVo.id()).isEqualTo(USER_ID);
            assertThat(cachedUserVo.email()).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("signOut")
    class SignOutTest {

        @Test
        @DisplayName("로그아웃 시 AccessToken을 블랙리스트에 등록한다")
        void signOut_blacklistsAccessToken() {
            // given
            long remainingMs = 500000L;
            given(jwtTokenProvider.getRemainingExpiration(ACCESS_TOKEN)).willReturn(remainingMs);

            // when
            authService.signOut(USER_ID, ACCESS_TOKEN);

            // then
            verify(tokenBlacklistService).blacklistToken(ACCESS_TOKEN, remainingMs);
        }

        @Test
        @DisplayName("로그아웃 시 RefreshToken을 삭제한다")
        void signOut_deletesRefreshToken() {
            // given
            given(jwtTokenProvider.getRemainingExpiration(ACCESS_TOKEN)).willReturn(500000L);

            // when
            authService.signOut(USER_ID, ACCESS_TOKEN);

            // then
            verify(refreshTokenService).deleteRefreshToken(USER_ID);
        }

        @Test
        @DisplayName("로그아웃 시 사용자 캐시를 삭제한다")
        void signOut_evictsUserCache() {
            // given
            given(jwtTokenProvider.getRemainingExpiration(ACCESS_TOKEN)).willReturn(500000L);

            // when
            authService.signOut(USER_ID, ACCESS_TOKEN);

            // then
            verify(userCacheService).evictUser(USER_ID);
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTest {

        @Test
        @DisplayName("토큰 갱신 시 새 AccessToken을 발급한다")
        void refreshToken_issuesNewAccessToken() {
            // given
            String newAccessToken = "new.access.token";
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(refreshTokenService.validateRefreshToken(USER_ID, REFRESH_TOKEN)).willReturn(true);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(newAccessToken);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);

            // when
            TokenResult result = authService.refreshToken(REFRESH_TOKEN);

            // then
            assertThat(result.accessToken()).isEqualTo(newAccessToken);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("null 토큰은 REFRESH_TOKEN_NOT_FOUND 예외를 발생시킨다")
        void refreshToken_withNull_throwsException() {
            // when & then
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.REFRESH_TOKEN_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("빈 토큰은 REFRESH_TOKEN_NOT_FOUND 예외를 발생시킨다")
        void refreshToken_withBlank_throwsException() {
            // when & then
            assertThatThrownBy(() -> authService.refreshToken("  "))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.REFRESH_TOKEN_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Redis에 없는 토큰은 INVALID_REFRESH_TOKEN 예외를 발생시킨다")
        void refreshToken_notInRedis_throwsException() {
            // given
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(refreshTokenService.validateRefreshToken(USER_ID, REFRESH_TOKEN)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 시 사용자 캐시를 업데이트한다")
        void refreshToken_updatesUserCache() {
            // given
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(refreshTokenService.validateRefreshToken(USER_ID, REFRESH_TOKEN)).willReturn(true);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(jwtTokenProvider.createAccessToken(USER_ID, EMAIL, Set.of(Role.ROLE_USER))).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);

            // when
            authService.refreshToken(REFRESH_TOKEN);

            // then
            verify(userCacheService).cacheUser(any(UserVo.class), eq(ACCESS_TOKEN_EXPIRATION_MS));
        }
    }
}
