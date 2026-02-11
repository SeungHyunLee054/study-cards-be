package com.example.study_cards.infra.security.oauth;

import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.redis.vo.UserVo;
import com.example.study_cards.infra.security.jwt.CookieProvider;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.exception.UserErrorCode;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserCacheService userCacheService;
    private final CookieProvider cookieProvider;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.createAccessToken(
                oAuth2User.userId(),
                oAuth2User.email(),
                oAuth2User.roles()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(oAuth2User.userId());

        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationMs();
        long refreshTokenExpiresIn = jwtTokenProvider.getRefreshTokenExpirationMs();

        refreshTokenService.saveRefreshToken(oAuth2User.userId(), refreshToken, refreshTokenExpiresIn);

        User user = userRepository.findById(oAuth2User.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        userCacheService.cacheUser(UserVo.from(user), accessTokenExpiresIn);

        cookieProvider.addRefreshTokenCookie(response, refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
