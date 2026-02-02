package com.example.study_cards.infra.security.jwt;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import com.example.study_cards.support.BaseUnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest extends BaseUnitTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final Set<Role> ROLES = Set.of(Role.ROLE_USER);

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTest {

        @Test
        @DisplayName("유효한 토큰이 있으면 SecurityContext에 인증 정보를 설정한다")
        void doFilterInternal_withValidToken_setsAuthentication() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).willReturn(false);
            given(jwtTokenProvider.getUserId(VALID_TOKEN)).willReturn(USER_ID);
            given(jwtTokenProvider.getEmail(VALID_TOKEN)).willReturn(EMAIL);
            given(jwtTokenProvider.getRoles(VALID_TOKEN)).willReturn(ROLES);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(jwtTokenProvider).validateToken(VALID_TOKEN);
            verify(filterChain).doFilter(request, response);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            assertThat(userDetails.userId()).isEqualTo(USER_ID);
            assertThat(userDetails.email()).isEqualTo(EMAIL);
            assertThat(userDetails.roles()).isEqualTo(ROLES);
        }

        @Test
        @DisplayName("토큰이 없으면 인증 없이 필터 체인을 계속한다")
        void doFilterInternal_withoutToken_continuesFilterChain() throws ServletException, IOException {
            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Authorization 헤더가 Bearer로 시작하지 않으면 토큰을 추출하지 않는다")
        void doFilterInternal_withNonBearerHeader_doesNotExtractToken() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Basic sometoken");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("블랙리스트에 있는 토큰은 BLACKLISTED_TOKEN 예외를 발생시킨다")
        void doFilterInternal_withBlacklistedToken_throwsException() {
            // given
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(JwtException.class)
                    .satisfies(exception -> {
                        JwtException jwtException = (JwtException) exception;
                        assertThat(jwtException.getErrorCode()).isEqualTo(JwtErrorCode.BLACKLISTED_TOKEN);
                    });
        }

        @Test
        @DisplayName("토큰 검증 실패 시 예외를 전파한다")
        void doFilterInternal_withInvalidToken_propagatesException() {
            // given
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).willReturn(false);
            org.mockito.Mockito.doThrow(new JwtException(JwtErrorCode.INVALID_TOKEN))
                    .when(jwtTokenProvider).validateToken(VALID_TOKEN);

            // when & then
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("extractToken")
    class ExtractTokenTest {

        @Test
        @DisplayName("Bearer 토큰을 올바르게 추출한다")
        void extractToken_withBearerToken_extractsCorrectly() throws ServletException, IOException {
            // given
            String expectedToken = "my.jwt.token";
            request.addHeader("Authorization", "Bearer " + expectedToken);
            given(tokenBlacklistService.isBlacklisted(expectedToken)).willReturn(false);
            given(jwtTokenProvider.getUserId(expectedToken)).willReturn(USER_ID);
            given(jwtTokenProvider.getEmail(expectedToken)).willReturn(EMAIL);
            given(jwtTokenProvider.getRoles(expectedToken)).willReturn(ROLES);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(jwtTokenProvider).validateToken(expectedToken);
        }

        @Test
        @DisplayName("Authorization 헤더가 비어있으면 null을 반환한다")
        void extractToken_withEmptyHeader_returnsNull() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
