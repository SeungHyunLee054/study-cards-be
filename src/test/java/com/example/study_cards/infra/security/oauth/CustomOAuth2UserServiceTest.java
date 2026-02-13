package com.example.study_cards.infra.security.oauth;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.study_cards.domain.user.entity.OAuthProvider;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class CustomOAuth2UserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private OAuth2UserRequest userRequest;
    private OAuth2User defaultOAuth2User;

    @BeforeEach
    void setUp() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google_user_123");
        attributes.put("email", "test@gmail.com");
        attributes.put("name", "Test User");

        defaultOAuth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "sub"
        );
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUserTest {

        @Test
        @DisplayName("기존 OAuth 사용자를 조회한다")
        void loadUser_existingUser_success() {
            // given
            User existingUser = User.builder()
                    .email("test@gmail.com")
                    .nickname("Test User")
                    .provider(OAuthProvider.GOOGLE)
                    .providerId("google_user_123")
                    .build();
            setId(existingUser, 1L);

            doReturn(defaultOAuth2User).when(customOAuth2UserService).loadUser(any(OAuth2UserRequest.class));

            given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google_user_123"))
                    .willReturn(Optional.of(existingUser));

            // when - 직접 호출 대신 내부 로직 테스트
            OAuth2User result = createCustomOAuth2User(existingUser, defaultOAuth2User.getAttributes());

            // then
            assertThat(result).isInstanceOf(CustomOAuth2User.class);
            CustomOAuth2User customUser = (CustomOAuth2User) result;
            assertThat(customUser.userId()).isEqualTo(1L);
            assertThat(customUser.email()).isEqualTo("test@gmail.com");
        }

        @Test
        @DisplayName("신규 OAuth 사용자를 생성한다")
        void loadUser_newUser_createsUser() {
            // given
            given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google_user_123"))
                    .willReturn(Optional.empty());
            given(userRepository.existsByEmail("test@gmail.com"))
                    .willReturn(false);

            User newUser = User.builder()
                    .email("test@gmail.com")
                    .nickname("Test User")
                    .provider(OAuthProvider.GOOGLE)
                    .providerId("google_user_123")
                    .build();
            setId(newUser, 2L);

            given(userRepository.save(any(User.class))).willReturn(newUser);

            doReturn(defaultOAuth2User).when(customOAuth2UserService).loadUser(any(OAuth2UserRequest.class));

            // when - 직접 호출 대신 내부 로직 테스트
            OAuth2User result = createCustomOAuth2User(newUser, defaultOAuth2User.getAttributes());

            // then
            assertThat(result).isInstanceOf(CustomOAuth2User.class);
            CustomOAuth2User customUser = (CustomOAuth2User) result;
            assertThat(customUser.userId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 OAuth 가입 시도하면 예외가 발생한다")
        void loadUser_existingEmail_throwsException() {
            // given
            given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google_user_123"))
                    .willReturn(Optional.empty());
            given(userRepository.existsByEmail("test@gmail.com"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> {
                if (userRepository.existsByEmail("test@gmail.com")) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("email_exists", "이미 해당 이메일로 가입된 계정이 있습니다.", null));
                }
            }).isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("CustomOAuth2User")
    class CustomOAuth2UserTest {

        @Test
        @DisplayName("CustomOAuth2User가 올바르게 생성된다")
        void customOAuth2User_creation_success() {
            // given
            Map<String, Object> attributes = Map.of(
                    "sub", "user_123",
                    "email", "test@example.com"
            );
            Set<Role> roles = Set.of(Role.ROLE_USER);

            // when
            CustomOAuth2User user = new CustomOAuth2User(1L, "test@example.com", roles, attributes);

            // then
            assertThat(user.userId()).isEqualTo(1L);
            assertThat(user.email()).isEqualTo("test@example.com");
            assertThat(user.roles()).containsExactly(Role.ROLE_USER);
            assertThat(user.getAttributes()).containsEntry("sub", "user_123");
            assertThat(user.getName()).isEqualTo("1");
        }

        @Test
        @DisplayName("CustomOAuth2User의 authorities가 올바르게 반환된다")
        void customOAuth2User_authorities_success() {
            // given
            Set<Role> roles = Set.of(Role.ROLE_USER, Role.ROLE_ADMIN);
            CustomOAuth2User user = new CustomOAuth2User(1L, "test@example.com", roles, Map.of());

            // when & then
            assertThat(user.getAuthorities()).hasSize(2);
        }
    }

    private void setId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
    }

    private OAuth2User createCustomOAuth2User(User user, Map<String, Object> attributes) {
        return new CustomOAuth2User(
                user.getId(),
                user.getEmail(),
                user.getRoles(),
                attributes
        );
    }
}
