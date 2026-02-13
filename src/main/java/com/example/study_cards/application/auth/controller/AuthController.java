package com.example.study_cards.application.auth.controller;

import com.example.study_cards.application.auth.dto.request.EmailVerificationRequest;
import com.example.study_cards.application.auth.dto.request.EmailVerificationVerifyRequest;
import com.example.study_cards.application.auth.dto.request.PasswordResetRequest;
import com.example.study_cards.application.auth.dto.request.PasswordResetVerifyRequest;
import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.SignInResponse;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.auth.exception.AuthErrorCode;
import com.example.study_cards.application.auth.exception.AuthException;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.infra.redis.service.RateLimitService;
import com.example.study_cards.infra.security.jwt.CookieProvider;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MAX_AUTH_ATTEMPTS = 5;
    private static final Duration AUTH_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);

    private final AuthService authService;
    private final CookieProvider cookieProvider;
    private final RateLimitService rateLimitService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response) {
        checkRateLimit("signin", request.email());
        TokenResult result = authService.signIn(request);
        cookieProvider.addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new SignInResponse(result.accessToken(), result.accessTokenExpiresIn()));
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            HttpServletResponse response) {
        if (!authorization.startsWith("Bearer ")) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        String accessToken = authorization.substring(7);
        authService.signOut(userDetails.userId(), accessToken);
        cookieProvider.deleteRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<SignInResponse> refresh(HttpServletRequest request) {
        String refreshToken = cookieProvider.extractRefreshToken(request).orElse(null);
        TokenResult result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new SignInResponse(result.accessToken(), result.accessTokenExpiresIn()));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        checkRateLimit("password-reset", request.email());
        authService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<Void> verifyPasswordReset(@Valid @RequestBody PasswordResetVerifyRequest request) {
        checkRateLimit("password-reset-verify", request.email());
        authService.verifyAndResetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<Void> requestEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        checkRateLimit("email-verification", request.email());
        authService.requestEmailVerification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verification/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationVerifyRequest request) {
        checkRateLimit("email-verification-verify", request.email());
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    private void checkRateLimit(String action, String identifier) {
        if (rateLimitService.isRateLimited(action, identifier, MAX_AUTH_ATTEMPTS, AUTH_RATE_LIMIT_WINDOW)) {
            throw new AuthException(AuthErrorCode.TOO_MANY_ATTEMPTS);
        }
    }
}
