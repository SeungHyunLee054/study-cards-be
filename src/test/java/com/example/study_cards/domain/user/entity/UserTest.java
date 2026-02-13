package com.example.study_cards.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("사용자를 생성한다")
        void builder_createsUser() {
            // when
            User user = User.builder()
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("testUser")
                    .build();

            // then
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
            assertThat(user.getNickname()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("roles 미지정 시 ROLE_USER가 기본값으로 설정된다")
        void builder_withoutRoles_defaultsToRoleUser() {
            // when
            User user = User.builder()
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("testUser")
                    .build();

            // then
            assertThat(user.getRoles()).contains(Role.ROLE_USER);
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
            // when
            User user = User.builder()
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("testUser")
                    .build();

            // then
            assertThat(user.getStreak()).isEqualTo(0);
        }

    }
}
