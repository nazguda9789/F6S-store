package com.web.service;
import com.web.config.CustomOAuth2User;
import com.web.entity.User;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google hoặc facebook
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        String avatar = null;
        if ("google".equals(registrationId)) {
            avatar = oauth2User.getAttribute("picture");
        }
        else if ("facebook".equals(registrationId)) {
            Map<String, Object> pictureObj = oauth2User.getAttribute("picture");
            if (pictureObj != null) {
                Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                if (dataObj != null) {
                    avatar = (String) dataObj.get("url");
                }
            }
        }
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setDeleted(false);
            user.setActived(true);
            user.setRole(Role.ROLE_USER);
            user.setCreatedDate(LocalDateTime.now());
            user.setFullName(name);
            user.setAvatar(avatar);
            if("google".equals(registrationId)){
                user.setUserType(UserType.GOOGLE);
            }
            if("facebook".equals(registrationId)){
                user.setUserType(UserType.FACEBOOK);
            }
            userRepository.save(user);
        }

        return new CustomOAuth2User(oauth2User,
                Collections.singletonList(new SimpleGrantedAuthority(Role.ROLE_USER.toString())));
    }

}
