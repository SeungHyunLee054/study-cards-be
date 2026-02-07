package com.example.study_cards.infra.security.oauth.userinfo;

import com.example.study_cards.domain.user.entity.OAuthProvider;

import java.util.Map;

public record KakaoOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        if (kakaoAccount == null) {
            return null;
        }
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getNickname() {
        Map<String, Object> properties = getProperties();
        if (properties == null) {
            return null;
        }
        return (String) properties.get("nickname");
    }

    private Map<String, Object> getKakaoAccount() {
        return (Map<String, Object>) attributes.get("kakao_account");
    }

    private Map<String, Object> getProperties() {
        return (Map<String, Object>) attributes.get("properties");
    }
}
