package com.web.config;

import com.web.doss.LoginAttemptService;
import com.web.entity.User;
import com.web.enums.Role;
import com.web.repository.UserRepository;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired private UserRepository userRepository;
    @Autowired private LoginAttemptService loginAttemptService;
    private final HttpSessionRequestCache requestCache=new HttpSessionRequestCache();

    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,
                                                   Authentication authentication) throws IOException {
        SavedRequest saved=requestCache.getRequest(request,response);
        requestCache.removeRequest(request,response);
        String email=authentication instanceof OAuth2AuthenticationToken oauth
                ? oauth.getPrincipal().getAttribute("email") : authentication.getName();
        User user=email==null ? null : userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user==null || Boolean.TRUE.equals(user.getDeleted()) || !Boolean.TRUE.equals(user.getActived())
                || !user.isAccountNonLocked() || user.getRole()==null) {
            new SecurityContextLogoutHandler().logout(request,response,authentication);
            response.sendRedirect("/login?error"); return;
        }
        // Never allow a saved URL to bypass activation.
        if (user.getRole()==Role.ROLE_USER && !Boolean.TRUE.equals(user.getActivationStatus())) {
            new SecurityContextLogoutHandler().logout(request,response,authentication);
            response.sendRedirect("/verify-otp?email="+URLEncoder.encode(user.getEmail(),StandardCharsets.UTF_8)); return;
        }
        if (user.getFailedAttempts()>0) loginAttemptService.resetFailedAttempts(user);
        String fallback=user.getRole()==Role.ROLE_ADMIN ? TypeSystem.ADMIN_URL+"/invoice/list"
                : user.getRole()==Role.ROLE_EMPLOYEE ? TypeSystem.EMPLOYEE_URL+"/invoice/list" : "/";
        response.sendRedirect(safeSavedTarget(saved,request,user.getRole()).orElse(fallback));
    }

    private Optional<String> safeSavedTarget(SavedRequest saved,HttpServletRequest request,Role role) {
        if (saved==null || !"GET".equals(saved.getMethod())) return Optional.empty();
        try {
            URI uri=URI.create(saved.getRedirectUrl());
            if (uri.getRawUserInfo()!=null || uri.getRawFragment()!=null) return Optional.empty();
            if (uri.isAbsolute()) {
                int port=uri.getPort()<0 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
                if (!request.getScheme().equalsIgnoreCase(uri.getScheme())
                        || !request.getServerName().equalsIgnoreCase(uri.getHost()) || port!=request.getServerPort())
                    return Optional.empty();
            } else if (uri.getRawAuthority()!=null) return Optional.empty();
            Set<String> allowed=role==Role.ROLE_USER ? Set.of("/","/cart","/checkout","/tai-khoan")
                    : Set.of((role==Role.ROLE_ADMIN ? TypeSystem.ADMIN_URL : TypeSystem.EMPLOYEE_URL)+"/invoice/list");
            if (!allowed.contains(uri.getPath())) return Optional.empty();
            return Optional.of(uri.getRawPath()+(uri.getRawQuery()==null ? "" : "?"+uri.getRawQuery()));
        } catch (IllegalArgumentException ex) { return Optional.empty(); }
    }
}
