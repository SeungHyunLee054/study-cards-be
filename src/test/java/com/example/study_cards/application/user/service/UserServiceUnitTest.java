package com.example.study_cards.application.user.service;

import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.user.dto.request.PasswordChangeRequest;
import com.example.study_cards.application.user.dto.request.UserUpdateRequest;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.exception.UserErrorCode;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class UserServiceUnitTest extends BaseUnitTest {

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private UserCacheService userCacheService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private User testUser;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String NICKNAME = "testUser";
    private static final String PASSWORD = "encodedPassword";
    private static final String NEW_NICKNAME = "newNickname";
    private static final String ACCESS_TOKEN = "access-token";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = User.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .nickname(NICKNAME)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        ReflectionTestUtils.setField(user, "id", USER_ID);

        return user;
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfoTest {

        @Test
        @DisplayName("내 정보 조회 성공 시 UserResponse를 반환한다")
        void getMyInfo_returnsUserResponse() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            UserResponse response = userService.getMyInfo(USER_ID);

            // then
            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.nickname()).isEqualTo(NICKNAME);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
        void getMyInfo_userNotFound_throwsException() {
            // given
            given(userDomainService.findById(USER_ID))
                    .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                    .isInstanceOf(UserException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateMyInfo")
    class UpdateMyInfoTest {

        @Test
        @DisplayName("내 정보 수정 성공 시 수정된 UserResponse를 반환한다")
        void updateMyInfo_returnsUpdatedUserResponse() {
            // given
            UserUpdateRequest request = new UserUpdateRequest(NEW_NICKNAME);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            UserResponse response = userService.updateMyInfo(USER_ID, request);

            // then
            assertThat(response.nickname()).isEqualTo(NEW_NICKNAME);
        }

        @Test
        @DisplayName("내 정보 수정 시 사용자 캐시를 삭제한다")
        void updateMyInfo_evictsUserCache() {
            // given
            UserUpdateRequest request = new UserUpdateRequest(NEW_NICKNAME);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            userService.updateMyInfo(USER_ID, request);

            // then
            verify(userCacheService).evictUser(USER_ID);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTest {

        private static final String CURRENT_PASSWORD = "currentPassword";
        private static final String NEW_PASSWORD = "newPassword123";

        @Test
        @DisplayName("비밀번호 변경에 성공한다")
        void changePassword_success() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(CURRENT_PASSWORD, NEW_PASSWORD);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            userService.changePassword(USER_ID, request);

            // then
            verify(userDomainService).validatePassword(CURRENT_PASSWORD, PASSWORD);
            verify(userDomainService).changePassword(testUser, NEW_PASSWORD);
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다")
        void changePassword_invalidCurrentPassword_throwsException() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest("wrongPassword", NEW_PASSWORD);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            doThrow(new UserException(UserErrorCode.INVALID_PASSWORD))
                    .when(userDomainService).validatePassword("wrongPassword", PASSWORD);

            // when & then
            assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                    .isInstanceOf(UserException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.INVALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("withdrawMyAccount")
    class WithdrawMyAccountTest {

        @Test
        @DisplayName("회원 탈퇴 시 토큰 정리 후 탈퇴 처리한다")
        void withdrawMyAccount_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(jwtTokenProvider.getRemainingExpiration(ACCESS_TOKEN)).willReturn(1000L);

            // when
            userService.withdrawMyAccount(USER_ID, ACCESS_TOKEN);

            // then
            verify(tokenBlacklistService).blacklistToken(ACCESS_TOKEN, 1000L);
            verify(refreshTokenService).deleteRefreshToken(USER_ID);
            verify(userCacheService).evictUser(USER_ID);
            verify(userDomainService).withdraw(testUser);
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자면 예외가 발생한다")
        void withdrawMyAccount_alreadyWithdrawn_throwsException() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(jwtTokenProvider.getRemainingExpiration(ACCESS_TOKEN)).willReturn(1000L);
            doThrow(new UserException(UserErrorCode.USER_ALREADY_WITHDRAWN))
                    .when(userDomainService).withdraw(testUser);

            // when & then
            assertThatThrownBy(() -> userService.withdrawMyAccount(USER_ID, ACCESS_TOKEN))
                    .isInstanceOf(UserException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }
    }
}
