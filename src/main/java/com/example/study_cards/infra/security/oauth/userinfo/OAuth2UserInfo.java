package com.example.study_cards.infra.security.oauth.userinfo;

import com.example.study_cards.domain.user.entity.OAuthProvider;

public interface OAuth2UserInfo {

    OAuthProvider getProvider();

    String getProviderId();

    String getEmail();

    String getNickname();
}
