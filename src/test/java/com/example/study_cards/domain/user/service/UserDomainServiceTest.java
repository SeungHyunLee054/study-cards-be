package com.example.study_cards.domain.user.service;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;
import com.example.study_cards.domain.user.exception.UserErrorCode;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserDomainServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserDomainService userDomainService;

    private User testUser;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String NICKNAME = "testUser";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .nickname(NICKNAME)
                .roles(Set.of(Role.ROLE_USER))
                .build();
        setId(user, USER_ID);
        return user;
    }

    private void setId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUserTest {

        @Test
        @DisplayName("새 사용자를 등록한다")
        void registerUser_createsAndReturnsUser() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // when
            User result = userDomainService.registerUser(EMAIL, PASSWORD, NICKNAME);

            // then
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getNickname()).isEqualTo(NICKNAME);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("비밀번호를 암호화하여 저장한다")
        void registerUser_encodesPassword() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // when
            userDomainService.registerUser(EMAIL, PASSWORD, NICKNAME);

            // then
            verify(passwordEncoder).encode(PASSWORD);
        }

        @Test
        @DisplayName("중복 이메일로 등록 시 예외를 발생시킨다")
        void registerUser_withDuplicateEmail_throwsException() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userDomainService.registerUser(EMAIL, PASSWORD, NICKNAME))
                    .isInstanceOf(UserException.class)
                    .satisfies(exception -> {
                        UserException userException = (UserException) exception;
                        assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
                    });
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTest {

        @Test
        @DisplayName("이메일로 사용자를 조회한다")
        void findByEmail_returnsUser() {
            // given
            given(userRepository.findByEmailAndStatus(EMAIL, UserStatus.ACTIVE)).willReturn(Optional.of(testUser));

            // when
            User result = userDomainService.findByEmail(EMAIL);

            // then
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 시 예외를 발생시킨다")
        void findByEmail_withNonExistentEmail_throwsException() {
            // given
            given(userRepository.findByEmailAndStatus(EMAIL, UserStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userDomainService.findByEmail(EMAIL))
                    .isInstanceOf(UserException.class)
                    .satisfies(exception -> {
                        UserException userException = (UserException) exception;
                        assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("ID로 사용자를 조회한다")
        void findById_returnsUser() {
            // given
            given(userRepository.findByIdAndStatus(USER_ID, UserStatus.ACTIVE)).willReturn(Optional.of(testUser));

            // when
            User result = userDomainService.findById(USER_ID);

            // then
            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
        void findById_withNonExistentId_throwsException() {
            // given
            given(userRepository.findByIdAndStatus(USER_ID, UserStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userDomainService.findById(USER_ID))
                    .isInstanceOf(UserException.class)
                    .satisfies(exception -> {
                        UserException userException = (UserException) exception;
                        assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePasswordTest {

        @Test
        @DisplayName("비밀번호가 일치하면 예외를 발생시키지 않는다")
        void validatePassword_withCorrectPassword_doesNotThrow() {
            // given
            given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);

            // when & then
            userDomainService.validatePassword(PASSWORD, ENCODED_PASSWORD);
            verify(passwordEncoder).matches(PASSWORD, ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외를 발생시킨다")
        void validatePassword_withIncorrectPassword_throwsException() {
            // given
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userDomainService.validatePassword("wrongPassword", ENCODED_PASSWORD))
                    .isInstanceOf(UserException.class)
                    .satisfies(exception -> {
                        UserException userException = (UserException) exception;
                        assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.INVALID_PASSWORD);
                    });
        }
    }

    @Nested
    @DisplayName("ban")
    class BanTest {

        @Test
        @DisplayName("활성 사용자를 제재한다")
        void ban_activeUser_success() {
            // when
            userDomainService.ban(testUser);

            // then
            assertThat(testUser.getStatus()).isEqualTo(UserStatus.BANNED);
        }

        @Test
        @DisplayName("이미 제재된 사용자면 예외를 발생시킨다")
        void ban_alreadyBanned_throwsException() {
            // given
            testUser.ban();

            // when & then
            assertThatThrownBy(() -> userDomainService.ban(testUser))
                    .isInstanceOf(UserException.class)
                    .satisfies(exception -> {
                        UserException userException = (UserException) exception;
                        assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_ALREADY_BANNED);
                    });
        }
    }
}
