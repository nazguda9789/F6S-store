package com.web.config;


import com.web.doss.LoginAttemptService;
import com.web.entity.User;
import com.web.exception.LoginException;
import com.web.exception.UserNotActivatedException;
import com.web.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        HttpSession session = request.getSession();

        // lấy email từ form login
        String emailCheck = request.getParameter("username");

        if (exception instanceof InternalAuthenticationServiceException
                && exception.getCause() instanceof UserNotActivatedException) {
            String email = ((UserNotActivatedException) exception.getCause()).getEmail();
            response.sendRedirect("/verify-otp?email=" + email+"&duaration="+ UUID.randomUUID().toString());
        }
        else if(exception instanceof InternalAuthenticationServiceException
                && exception.getCause() instanceof LoginException){
            if (emailCheck != null) {
                Optional<User> userOpt = userRepository.findByEmail(emailCheck);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.isAccountNonLocked()) {
                        loginAttemptService.increaseFailedAttempts(user);
                    }
                }
            }
            String email = ((LoginException) exception.getCause()).getEmail();
            String message = exception.getCause().getMessage();
            session.setAttribute("loginError", message); // lưu message vào session
            response.sendRedirect("/login");
        }
        else if(exception instanceof InternalAuthenticationServiceException
                && exception.getCause() instanceof LockedException){
            String message = exception.getCause().getMessage();
            session.setAttribute("loginError", message); // lưu message vào session
            response.sendRedirect("/login");
        }
        else {
            if (emailCheck != null) {
                Optional<User> userOpt = userRepository.findByEmail(emailCheck);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.isAccountNonLocked()) {
                        loginAttemptService.increaseFailedAttempts(user);
                    }
                }
            }
            session.setAttribute("loginError", "Tài khoản hoặc mật khẩu không chính xác");
            response.sendRedirect("/login");
        }
    }
}