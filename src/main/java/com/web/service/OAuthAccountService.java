package com.web.service;

import com.web.config.CustomOAuth2User;
import com.web.entity.User;
import com.web.enums.*;
import com.web.repository.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OAuthAccountService {
    private final UserRepository users;
    private final PendingRegistrationRepository pending;
    private final OtpCodeRepository codes;
    public OAuthAccountService(UserRepository users, PendingRegistrationRepository pending, OtpCodeRepository codes) {
        this.users=users; this.pending=pending; this.codes=codes;
    }
    @Transactional
    public OAuth2User completeLogin(String provider, OAuth2User profile) {
        boolean google="google".equals(provider);
        if (!google && !"facebook".equals(provider)) throw failure("unsupported_provider");
        String email=profile.getAttribute("email");
        if (!StringUtils.hasText(email)) throw failure("email_missing");
        if (google && !Boolean.TRUE.equals(profile.getAttribute("email_verified"))) throw failure("email_unverified");
        email=email.trim().toLowerCase(Locale.ROOT);
        User user=users.findByEmail(email).orElse(null);
        boolean isNew=user==null;
        if (!isNew) {
            if (Boolean.TRUE.equals(user.getDeleted()) || !Boolean.TRUE.equals(user.getActived())
                    || user.getRole()==null) throw failure("account_unavailable");
            if (!user.isAccountNonLocked()) {
                if (user.getLockTime()==null || user.getLockTime().plusMinutes(com.web.doss.LoginAttemptService.LOCK_TIME_DURATION).isAfter(LocalDateTime.now()))
                    throw failure("account_unavailable");
                user.setAccountNonLocked(true); user.setFailedAttempts(0); user.setLockTime(null);
            }
            if (!google && user.getUserType()!=UserType.FACEBOOK) throw failure("account_link_required");
            if (user.getRole()!=Role.ROLE_USER && !Boolean.TRUE.equals(user.getActivationStatus()))
                throw failure("account_unavailable");
        } else {
            user=new User(); user.setEmail(email); user.setRole(Role.ROLE_USER);
            user.setActived(true); user.setDeleted(false); user.setAccountNonLocked(true);
            user.setCreatedDate(LocalDateTime.now());
        }
        boolean unverified=isNew || !Boolean.TRUE.equals(user.getActivationStatus());
        if (unverified) {
            // Discard credentials/profile supplied before email ownership was proved.
            user.setPassword(null); user.setRememberKey(null); user.setPhone(null); user.setAddress(null);
            user.setUserType(google ? UserType.GOOGLE : UserType.FACEBOOK);
            user.setIsGoogleUser(google); user.setFullName(profile.getAttribute("name"));
        } else if (!StringUtils.hasText(user.getFullName())) user.setFullName(profile.getAttribute("name"));
        if (google) user.setAvatar(profile.getAttribute("picture"));
        user.setActivationStatus(true);
        user=users.save(user);
        if (google) {
            pending.deleteById(email);
            codes.invalidatePreviousRegistrationCodes(email,-1L);
        }
        return new CustomOAuth2User(profile,List.of(new SimpleGrantedAuthority(user.getRole().name())));
    }
    private OAuth2AuthenticationException failure(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
