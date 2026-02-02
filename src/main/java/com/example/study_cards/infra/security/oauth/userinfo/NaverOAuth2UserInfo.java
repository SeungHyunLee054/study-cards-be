package com.example.study_cards.infra.security.oauth.userinfo;

import com.example.study_cards.domain.user.entity.OAuthProvider;

import java.util.Map;

public record NaverOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = getResponse(attributes);
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        String nickname = (String) attributes.get("nickname");
        if (nickname == null) {
            nickname = (String) attributes.get("name");
        }
        return nickname;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse(Map<String, Object> attributes) {
        return (Map<String, Object>) attributes.get("response");
    }
}
