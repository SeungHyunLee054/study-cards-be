package com.example.study_cards.application.auth.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.SignInResponse;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.auth.service.AuthService;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieProvider cookieProvider;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response) {
        TokenResult result = authService.signIn(request);
        cookieProvider.addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new SignInResponse(result.accessToken(), result.accessTokenExpiresIn()));
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            HttpServletResponse response) {
        String accessToken = authorization.substring("Bearer ".length());
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
}
