package com.example.study_cards.domain.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("사용자를 생성한다")
        void builder_createsUser() {
            // then
            assertThat(testUser.getEmail()).isEqualTo("test@example.com");
            assertThat(testUser.getPassword()).isEqualTo("encodedPassword");
            assertThat(testUser.getNickname()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("roles 미지정 시 ROLE_USER가 기본값으로 설정된다")
        void builder_withoutRoles_defaultsToRoleUser() {
            // then
            assertThat(testUser.getRoles()).contains(Role.ROLE_USER);
        }

        @Test
        @DisplayName("roles를 지정하면 해당 값이 설정된다")
        void builder_withRoles_usesProvidedValue() {
            // when
            User user = User.builder()
                    .email("admin@example.com")
                    .password("encodedPassword")
                    .nickname("admin")
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();

            // then
            assertThat(user.getRoles()).contains(Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("streak 기본값은 0이다")
        void builder_streakDefaultsToZero() {
            // then
            assertThat(testUser.getStreak()).isEqualTo(0);
        }

        @Test
        @DisplayName("provider 미지정 시 LOCAL이 기본값으로 설정된다")
        void builder_providerDefaultsToLocal() {
            // then
            assertThat(testUser.getProvider()).isEqualTo(OAuthProvider.LOCAL);
        }

        @Test
        @DisplayName("pushEnabled 기본값은 true이다")
        void builder_pushEnabledDefaultsToTrue() {
            // then
            assertThat(testUser.getPushEnabled()).isTrue();
        }

        @Test
        @DisplayName("emailVerified 기본값은 false이다")
        void builder_emailVerifiedDefaultsToFalse() {
            // then
            assertThat(testUser.getEmailVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("addRole / removeRole / hasRole")
    class RoleManagementTest {

        @Test
        @DisplayName("역할을 추가한다")
        void addRole_success() {
            // when
            testUser.addRole(Role.ROLE_ADMIN);

            // then
            assertThat(testUser.getRoles()).contains(Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("역할을 제거한다")
        void removeRole_success() {
            // given
            testUser.addRole(Role.ROLE_ADMIN);

            // when
            testUser.removeRole(Role.ROLE_ADMIN);

            // then
            assertThat(testUser.getRoles()).doesNotContain(Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("보유한 역할이면 true를 반환한다")
        void hasRole_containsRole_true() {
            // then
            assertThat(testUser.hasRole(Role.ROLE_USER)).isTrue();
        }

        @Test
        @DisplayName("보유하지 않은 역할이면 false를 반환한다")
        void hasRole_notContainsRole_false() {
            // then
            assertThat(testUser.hasRole(Role.ROLE_ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOAuthUser")
    class IsOAuthUserTest {

        @Test
        @DisplayName("LOCAL 사용자이면 false를 반환한다")
        void isOAuthUser_local_false() {
            // then
            assertThat(testUser.isOAuthUser()).isFalse();
        }

        @Test
        @DisplayName("OAuth 사용자이면 true를 반환한다")
        void isOAuthUser_oauth_true() {
            // given
            User oauthUser = User.builder()
                    .email("oauth@example.com")
                    .nickname("oauthUser")
                    .provider(OAuthProvider.GOOGLE)
                    .providerId("google123")
                    .build();

            // then
            assertThat(oauthUser.isOAuthUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateStreak")
    class UpdateStreakTest {

        @Test
        @DisplayName("첫 학습 시 streak이 1이 된다")
        void updateStreak_firstStudy_streakBecomesOne() {
            // when
            testUser.updateStreak(LocalDate.now());

            // then
            assertThat(testUser.getStreak()).isEqualTo(1);
            assertThat(testUser.getLastStudyDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("같은 날 학습하면 streak이 변하지 않는다")
        void updateStreak_sameDay_streakUnchanged() {
            // given
            LocalDate today = LocalDate.now();
            testUser.updateStreak(today);
            assertThat(testUser.getStreak()).isEqualTo(1);

            // when
            testUser.updateStreak(today);

            // then
            assertThat(testUser.getStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("연속일 학습하면 streak이 증가한다")
        void updateStreak_consecutiveDay_streakIncreases() {
            // given
            LocalDate yesterday = LocalDate.now().minusDays(1);
            testUser.updateStreak(yesterday);
            assertThat(testUser.getStreak()).isEqualTo(1);

            // when
            testUser.updateStreak(LocalDate.now());

            // then
            assertThat(testUser.getStreak()).isEqualTo(2);
        }

        @Test
        @DisplayName("비연속일 학습하면 streak이 1로 초기화된다")
        void updateStreak_nonConsecutiveDay_streakResetsToOne() {
            // given
            LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
            testUser.updateStreak(threeDaysAgo);
            testUser.updateStreak(threeDaysAgo.plusDays(1));
            assertThat(testUser.getStreak()).isEqualTo(2);

            // when
            testUser.updateStreak(LocalDate.now());

            // then
            assertThat(testUser.getStreak()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateNickname")
    class UpdateNicknameTest {

        @Test
        @DisplayName("닉네임을 변경한다")
        void updateNickname_success() {
            // when
            testUser.updateNickname("newNickname");

            // then
            assertThat(testUser.getNickname()).isEqualTo("newNickname");
        }
    }

    @Nested
    @DisplayName("updatePassword")
    class UpdatePasswordTest {

        @Test
        @DisplayName("비밀번호를 변경한다")
        void updatePassword_success() {
            // when
            testUser.updatePassword("newEncodedPassword");

            // then
            assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }
    }

    @Nested
    @DisplayName("updateFcmToken / removeFcmToken")
    class FcmTokenTest {

        @Test
        @DisplayName("FCM 토큰을 설정한다")
        void updateFcmToken_success() {
            // when
            testUser.updateFcmToken("fcm-token-123");

            // then
            assertThat(testUser.getFcmToken()).isEqualTo("fcm-token-123");
        }

        @Test
        @DisplayName("FCM 토큰을 제거한다")
        void removeFcmToken_success() {
            // given
            testUser.updateFcmToken("fcm-token-123");

            // when
            testUser.removeFcmToken();

            // then
            assertThat(testUser.getFcmToken()).isNull();
        }
    }

    @Nested
    @DisplayName("updatePushEnabled")
    class UpdatePushEnabledTest {

        @Test
        @DisplayName("푸시 알림을 비활성화한다")
        void updatePushEnabled_false() {
            // when
            testUser.updatePushEnabled(false);

            // then
            assertThat(testUser.getPushEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmailTest {

        @Test
        @DisplayName("이메일을 인증한다")
        void verifyEmail_success() {
            // when
            testUser.verifyEmail();

            // then
            assertThat(testUser.getEmailVerified()).isTrue();
        }
    }
}
