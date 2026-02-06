package com.example.study_cards.infra.security.oauth;

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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class OAuth2FailureHandlerTest extends BaseUnitTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(failureHandler, "redirectUri", "http://localhost:3000/oauth/callback");
        failureHandler.setRedirectStrategy(redirectStrategy);
    }

    @Nested
    @DisplayName("onAuthenticationFailure")
    class OnAuthenticationFailureTest {

        @Test
        @DisplayName("OAuth 인증 실패 시 에러 메시지와 함께 리다이렉트한다")
        void onAuthenticationFailure_redirectsWithError() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("email_exists", "이미 해당 이메일로 가입된 계정이 있습니다.", null);
            AuthenticationException exception = new OAuth2AuthenticationException(error, error.getDescription());

            // when
            failureHandler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("http://localhost:3000/oauth/callback");
            assertThat(redirectUrl).contains("error=");
        }

        @Test
        @DisplayName("에러 메시지가 URL 인코딩된다")
        void onAuthenticationFailure_encodesErrorMessage() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("test_error", "테스트 에러 메시지", null);
            AuthenticationException exception = new OAuth2AuthenticationException(error, error.getDescription());

            // when
            failureHandler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("error=");
            assertThat(redirectUrl).doesNotContain("테스트 에러 메시지");
        }

        @Test
        @DisplayName("예외 메시지가 null이면 기본 메시지를 사용한다")
        void onAuthenticationFailure_nullMessage_usesDefaultMessage() throws Exception {
            // given
            AuthenticationException exception = new AuthenticationException(null) {};

            // when
            failureHandler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("error=");
        }

        @Test
        @DisplayName("리다이렉트 URL이 올바르게 구성된다")
        void onAuthenticationFailure_correctRedirectUrl() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid access token", null);
            AuthenticationException exception = new OAuth2AuthenticationException(error, error.getDescription());

            // when
            failureHandler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith("http://localhost:3000/oauth/callback?error=");
        }

        @Test
        @DisplayName("다양한 에러 코드를 처리할 수 있다")
        void onAuthenticationFailure_handlesVariousErrors() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("server_error", "Internal server error occurred", null);
            AuthenticationException exception = new OAuth2AuthenticationException(error, error.getDescription());

            // when
            failureHandler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("error=");
        }
    }
}
