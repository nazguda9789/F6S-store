package com.web.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuthAccountService accounts;
    public CustomOAuth2UserService(OAuthAccountService accounts) { this.accounts=accounts; }
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User profile=super.loadUser(request);
        return accounts.completeLogin(request.getClientRegistration().getRegistrationId(),profile);
    }
}
