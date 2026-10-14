package com.web.config;

import com.web.doss.LoginAttemptService;
import com.web.entity.User;
import com.web.enums.Role;
import com.web.repository.UserRepository;
import com.web.utils.TypeSystem;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private LoginAttemptService loginAttemptService;

    private HttpSessionRequestCache requestCache = new HttpSessionRequestCache();


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Lấy URL trước đó nếu có
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String redirectUrl = (savedRequest != null) ? savedRequest.getRedirectUrl() : null;

        String email;

        // OAuth2 login
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();
            email = oauthUser.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // ✅ Reset số lần sai khi login đúng
            if (user.getFailedAttempts() > 0) {
                loginAttemptService.resetFailedAttempts(user);
            }

            Role role = user.getRole();

            // Ưu tiên redirect về URL trước đó nếu có
            if (redirectUrl != null) {
                response.sendRedirect(redirectUrl);
                return;
            }

            // Nếu không có URL trước đó, redirect theo role
            if (role.equals(Role.ROLE_ADMIN)) {
                response.sendRedirect(TypeSystem.ADMIN_URL + "/invoice/list");
            }
            else if (role.equals(Role.ROLE_EMPLOYEE)) {
                response.sendRedirect(TypeSystem.EMPLOYEE_URL + "/invoice/list");
            }
            else if (role.equals(Role.ROLE_USER)) {
                if (user.getActivationStatus() != null && !user.getActivationStatus()) {
                    response.sendRedirect("/verify-otp?email=" + email);
                }
                else {
                    response.sendRedirect("/");
                }
            }
            else {
                response.sendRedirect("/");
            }
        }
        else {
            response.sendRedirect("/login?error");
        }
    }
}
