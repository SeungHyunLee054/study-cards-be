package com.example.study_cards.infra.security.oauth;

import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.security.oauth.userinfo.OAuth2UserInfo;
import com.example.study_cards.infra.security.oauth.userinfo.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElseGet(() -> findOrCreateUser(userInfo));

        return new CustomOAuth2User(
                user.getId(),
                user.getEmail(),
                user.getRoles(),
                oAuth2User.getAttributes()
        );
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        if (userRepository.existsByEmail(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_exists", "이미 해당 이메일로 가입된 계정이 있습니다.", null)
            );
        }
        return registerNewUser(userInfo);
    }

    private User registerNewUser(OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .build();

        return userRepository.save(user);
    }
}
