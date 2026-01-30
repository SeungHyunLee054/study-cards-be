package com.example.study_cards.application.auth.service;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.redis.vo.UserVo;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("nickname", "testUser")
                .sample();

        signInRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .sample();
    }

    @Nested
    @DisplayName("signUp")
    class SignUpTest {

        @Test
        @DisplayName("회원가입이 성공하면 사용자 정보를 반환한다")
        void signUp_success_returnsUserResponse() {
            // when
            UserResponse response = authService.signUp(signUpRequest);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.email()).isEqualTo(signUpRequest.email());
            assertThat(response.nickname()).isEqualTo(signUpRequest.nickname());
        }

        @Test
        @DisplayName("회원가입 시 비밀번호가 암호화되어 저장된다")
        void signUp_success_encodesPassword() {
            // when
            UserResponse response = authService.signUp(signUpRequest);

            // then
            User savedUser = userRepository.findById(response.id()).orElseThrow();
            assertThat(passwordEncoder.matches(signUpRequest.password(), savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
        void signUp_withDuplicateEmail_throwsException() {
            // given
            authService.signUp(signUpRequest);

            SignUpRequest duplicateRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                    .set("email", signUpRequest.email())
                    .set("password", "different123")
                    .set("nickname", "different")
                    .sample();

            // when & then
            assertThatThrownBy(() -> authService.signUp(duplicateRequest))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("signIn")
    class SignInTest {

        @Test
        @DisplayName("로그인 성공 시 토큰을 반환한다")
        void signIn_success_returnsTokenResult() {
            // given
            authService.signUp(signUpRequest);

            // when
            TokenResult result = authService.signIn(signInRequest);

            // then
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(result.accessTokenExpiresIn()).isPositive();
        }

        @Test
        @DisplayName("로그인 시 유효한 JWT 토큰이 생성된다")
        void signIn_generatesValidJwtTokens() {
            // given
            authService.signUp(signUpRequest);

            // when
            TokenResult result = authService.signIn(signInRequest);

            // then
            jwtTokenProvider.validateToken(result.accessToken());
            jwtTokenProvider.validateToken(result.refreshToken());

            assertThat(jwtTokenProvider.getEmail(result.accessToken())).isEqualTo(signUpRequest.email());
            assertThat(jwtTokenProvider.getRole(result.accessToken())).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("로그인 시 RefreshToken이 Redis에 저장된다")
        void signIn_savesRefreshTokenToRedis() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);

            // when
            TokenResult result = authService.signIn(signInRequest);

            // then
            boolean isValid = refreshTokenService.validateRefreshToken(userResponse.id(), result.refreshToken());
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("로그인 시 사용자 정보가 캐시된다")
        void signIn_cachesUser() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);

            // when
            authService.signIn(signInRequest);

            // then
            String key = "user:" + userResponse.id();
            Boolean hasKey = redisTemplate.hasKey(key);
            assertThat(hasKey).isTrue();
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생한다")
        void signIn_withWrongPassword_throwsException() {
            // given
            authService.signUp(signUpRequest);

            SignInRequest wrongPasswordRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                    .set("email", signUpRequest.email())
                    .set("password", "wrongPassword")
                    .sample();

            // when & then
            assertThatThrownBy(() -> authService.signIn(wrongPasswordRequest))
                    .isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
        void signIn_withNonExistentEmail_throwsException() {
            // given
            SignInRequest nonExistentRequest = fixtureMonkey.giveMeBuilder(SignInRequest.class)
                    .set("email", "nonexistent@example.com")
                    .set("password", "password123")
                    .sample();

            // when & then
            assertThatThrownBy(() -> authService.signIn(nonExistentRequest))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("signOut")
    class SignOutTest {

        @Test
        @DisplayName("로그아웃 시 AccessToken이 블랙리스트에 등록된다")
        void signOut_blacklistsAccessToken() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when
            authService.signOut(userResponse.id(), tokenResult.accessToken());

            // then
            boolean isBlacklisted = tokenBlacklistService.isBlacklisted(tokenResult.accessToken());
            assertThat(isBlacklisted).isTrue();
        }

        @Test
        @DisplayName("로그아웃 시 RefreshToken이 삭제된다")
        void signOut_deletesRefreshToken() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when
            authService.signOut(userResponse.id(), tokenResult.accessToken());

            // then
            Optional<String> refreshToken = refreshTokenService.getRefreshToken(userResponse.id());
            assertThat(refreshToken).isEmpty();
        }

        @Test
        @DisplayName("로그아웃 시 사용자 캐시가 삭제된다")
        void signOut_evictsUserCache() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            // when
            authService.signOut(userResponse.id(), tokenResult.accessToken());

            // then
            Optional<UserVo> cachedUser = userCacheService.getCachedUser(userResponse.id());
            assertThat(cachedUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTest {

        @Test
        @DisplayName("토큰 갱신 시 새로운 AccessToken을 발급한다")
        void refreshToken_issuesNewAccessToken() throws InterruptedException {
            // given
            authService.signUp(signUpRequest);
            TokenResult originalToken = authService.signIn(signInRequest);

            Thread.sleep(1000);

            // when
            TokenResult refreshedToken = authService.refreshToken(originalToken.refreshToken());

            // then
            assertThat(refreshedToken.accessToken()).isNotBlank();
            jwtTokenProvider.validateToken(refreshedToken.accessToken());
        }

        @Test
        @DisplayName("토큰 갱신 시 동일한 RefreshToken을 유지한다")
        void refreshToken_keepsSameRefreshToken() {
            // given
            authService.signUp(signUpRequest);
            TokenResult originalToken = authService.signIn(signInRequest);

            // when
            TokenResult refreshedToken = authService.refreshToken(originalToken.refreshToken());

            // then
            assertThat(refreshedToken.refreshToken()).isEqualTo(originalToken.refreshToken());
        }

        @Test
        @DisplayName("null RefreshToken으로 갱신 시 예외가 발생한다")
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
        @DisplayName("Redis에 없는 RefreshToken으로 갱신 시 예외가 발생한다")
        void refreshToken_notInRedis_throwsException() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            refreshTokenService.deleteRefreshToken(userResponse.id());

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(tokenResult.refreshToken()))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 시 사용자 캐시가 업데이트된다")
        void refreshToken_updatesUserCache() {
            // given
            UserResponse userResponse = authService.signUp(signUpRequest);
            TokenResult tokenResult = authService.signIn(signInRequest);

            userCacheService.evictUser(userResponse.id());

            // when
            authService.refreshToken(tokenResult.refreshToken());

            // then
            String key = "user:" + userResponse.id();
            Boolean hasKey = redisTemplate.hasKey(key);
            assertThat(hasKey).isTrue();
        }
    }
}
