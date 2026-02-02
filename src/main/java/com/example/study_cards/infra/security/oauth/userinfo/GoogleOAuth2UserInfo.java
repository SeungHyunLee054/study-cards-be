package com.example.study_cards.infra.security.oauth.userinfo;

import com.example.study_cards.domain.user.entity.OAuthProvider;

import java.util.Map;

public record GoogleOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }
}
