package com.example.study_cards.infra.security.oauth;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.security.jwt.CookieProvider;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import com.example.study_cards.support.BaseUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.study_cards.domain.user.entity.OAuthProvider;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class OAuth2SuccessHandlerTest extends BaseUnitTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserCacheService userCacheService;

    @Mock
    private CookieProvider cookieProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    private CustomOAuth2User oAuth2User;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "redirectUri", "http://localhost:3000/oauth/callback");
        successHandler.setRedirectStrategy(redirectStrategy);

        oAuth2User = new CustomOAuth2User(
                1L,
                "test@example.com",
                Set.of(Role.ROLE_USER),
                Map.of("sub", "oauth_user_123")
        );

        user = User.builder()
                .email("test@example.com")
                .nickname("Test User")
                .provider(OAuthProvider.GOOGLE)
                .providerId("oauth_user_123")
                .build();
        setId(user, 1L);
    }

    @Nested
    @DisplayName("onAuthenticationSuccess")
    class OnAuthenticationSuccessTest {

        @Test
        @DisplayName("OAuth 인증 성공 시 토큰을 생성하고 리다이렉트한다")
        void onAuthenticationSuccess_createsTokensAndRedirects() throws Exception {
            // given
            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anySet()))
                    .willReturn("access_token_123");
            given(jwtTokenProvider.createRefreshToken(anyLong()))
                    .willReturn("refresh_token_456");
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(900000L);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1209600000L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            verify(jwtTokenProvider).createAccessToken(1L, "test@example.com", Set.of(Role.ROLE_USER));
            verify(jwtTokenProvider).createRefreshToken(1L);
            verify(refreshTokenService).saveRefreshToken(eq(1L), eq("refresh_token_456"), anyLong());
            verify(userCacheService).cacheUser(any(), anyLong());
            verify(cookieProvider).addRefreshTokenCookie(response, "refresh_token_456");
        }

        @Test
        @DisplayName("리다이렉트 URL에 액세스 토큰이 포함된다")
        void onAuthenticationSuccess_redirectsWithToken() throws Exception {
            // given
            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anySet()))
                    .willReturn("access_token_abc");
            given(jwtTokenProvider.createRefreshToken(anyLong()))
                    .willReturn("refresh_token_xyz");
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(900000L);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1209600000L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("http://localhost:3000/oauth/callback");
            assertThat(redirectUrl).contains("token=access_token_abc");
        }

        @Test
        @DisplayName("Refresh Token이 Redis에 저장된다")
        void onAuthenticationSuccess_savesRefreshToken() throws Exception {
            // given
            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anySet()))
                    .willReturn("access_token");
            given(jwtTokenProvider.createRefreshToken(anyLong()))
                    .willReturn("refresh_token");
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(900000L);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1209600000L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            verify(refreshTokenService).saveRefreshToken(1L, "refresh_token", 1209600000L);
        }

        @Test
        @DisplayName("사용자 정보가 캐시에 저장된다")
        void onAuthenticationSuccess_cachesUser() throws Exception {
            // given
            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anySet()))
                    .willReturn("access_token");
            given(jwtTokenProvider.createRefreshToken(anyLong()))
                    .willReturn("refresh_token");
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(900000L);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1209600000L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            verify(userCacheService).cacheUser(any(), eq(900000L));
        }
    }

    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
