package com.example.study_cards.application.auth.service;

import com.example.study_cards.application.auth.dto.request.EmailVerificationRequest;
import com.example.study_cards.application.auth.dto.request.EmailVerificationVerifyRequest;
import com.example.study_cards.application.auth.dto.request.PasswordResetRequest;
import com.example.study_cards.application.auth.dto.request.PasswordResetVerifyRequest;
import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.auth.exception.AuthErrorCode;
import com.example.study_cards.application.auth.exception.AuthException;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.mail.service.EmailService;
import com.example.study_cards.infra.redis.service.EmailVerificationCodeService;
import com.example.study_cards.infra.redis.service.PasswordResetCodeService;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.redis.vo.UserVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserDomainService userDomainService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserCacheService userCacheService;
    private final PasswordResetCodeService passwordResetCodeService;
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final EmailService emailService;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new AuthException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        User user = userDomainService.registerUser(
                request.email(),
                request.password(),
                request.nickname()
        );

        String code = emailVerificationCodeService.generateAndSaveCode(user.getEmail());
        emailService.sendVerificationCode(user.getEmail(), code);

        return UserResponse.from(user);
    }

    public TokenResult signIn(SignInRequest request) {
        User user = userDomainService.findByEmail(request.email());
        userDomainService.validatePassword(request.password(), user.getPassword());

        if (!user.getEmailVerified()) {
            throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationMs();
        long refreshTokenExpiresIn = jwtTokenProvider.getRefreshTokenExpirationMs();

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken, refreshTokenExpiresIn);
        userCacheService.cacheUser(UserVo.from(user), accessTokenExpiresIn);

        return new TokenResult(accessToken, refreshToken, accessTokenExpiresIn);
    }

    public void signOut(Long userId, String accessToken) {
        long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken);
        tokenBlacklistService.blacklistToken(accessToken, remainingMs);
        refreshTokenService.deleteRefreshToken(userId);
        userCacheService.evictUser(userId);
    }

    public TokenResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JwtException(JwtErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        jwtTokenProvider.validateToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new JwtException(JwtErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userDomainService.findById(userId);

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoles());

        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

        userCacheService.cacheUser(UserVo.from(user), accessTokenExpiresIn);

        return new TokenResult(newAccessToken, refreshToken, accessTokenExpiresIn);
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        if (userDomainService.existsByEmail(request.email())) {
            String code = passwordResetCodeService.generateAndSaveCode(request.email());
            emailService.sendPasswordResetCode(request.email(), code);
        }
    }

    @Transactional
    public void verifyAndResetPassword(PasswordResetVerifyRequest request) {
        if (passwordResetCodeService.hasExceededAttempts(request.email())) {
            throw new AuthException(AuthErrorCode.TOO_MANY_ATTEMPTS);
        }

        if (!passwordResetCodeService.verifyCode(request.email(), request.code())) {
            throw new AuthException(AuthErrorCode.INVALID_RESET_CODE);
        }

        User user = userDomainService.findByEmail(request.email());
        userDomainService.changePassword(user, request.newPassword());
    }

    public void requestEmailVerification(EmailVerificationRequest request) {
        if (userDomainService.existsByEmail(request.email())) {
            User user = userDomainService.findByEmail(request.email());

            if (user.getEmailVerified()) {
                return;
            }

            String code = emailVerificationCodeService.generateAndSaveCode(request.email());
            emailService.sendVerificationCode(request.email(), code);
        }
    }

    @Transactional
    public void verifyEmail(EmailVerificationVerifyRequest request) {
        if (emailVerificationCodeService.hasExceededAttempts(request.email())) {
            throw new AuthException(AuthErrorCode.TOO_MANY_ATTEMPTS);
        }

        if (!emailVerificationCodeService.verifyCode(request.email(), request.code())) {
            throw new AuthException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        User user = userDomainService.findByEmail(request.email());
        user.verifyEmail();

        emailVerificationCodeService.deleteCode(request.email());
    }
}
