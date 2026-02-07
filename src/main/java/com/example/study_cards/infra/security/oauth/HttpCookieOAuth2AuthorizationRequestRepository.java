package com.example.study_cards.infra.security.oauth;

import com.example.study_cards.common.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(value -> CookieUtils.deserialize(value, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        CookieUtils.addCookie(
                response,
                OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                OAuth2CookieConstants.COOKIE_EXPIRE_SECONDS,
                cookieDomain
        );

        String redirectUriAfterLogin = request.getParameter(OAuth2CookieConstants.REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtils.addCookie(
                    response,
                    OAuth2CookieConstants.REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin,
                    OAuth2CookieConstants.COOKIE_EXPIRE_SECONDS,
                    cookieDomain
            );
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(response, OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, cookieDomain);
        CookieUtils.deleteCookie(response, OAuth2CookieConstants.REDIRECT_URI_PARAM_COOKIE_NAME, cookieDomain);
    }
}
