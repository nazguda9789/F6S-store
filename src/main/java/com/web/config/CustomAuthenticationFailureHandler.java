package com.web.config;

import com.web.doss.LoginAttemptService;
import com.web.entity.User;
import com.web.exception.*;
import com.web.repository.UserRepository;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Autowired private UserRepository userRepository;
    @Autowired private LoginAttemptService loginAttemptService;

    @Override public void onAuthenticationFailure(HttpServletRequest request,HttpServletResponse response,
                                                   AuthenticationException exception) throws IOException {
        Throwable cause=exception instanceof InternalAuthenticationServiceException && exception.getCause()!=null
                ? exception.getCause() : exception;
        if (cause instanceof UserNotActivatedException pending) {
            request.getSession().setAttribute("verificationNotice","Đăng ký chưa hoàn tất. Vui lòng nhập mã hoặc gửi lại mã để xác thực email.");
            response.sendRedirect("/verify-otp?email="+URLEncoder.encode(pending.getEmail(),StandardCharsets.UTF_8)); return;
        }
        String message="Tài khoản hoặc mật khẩu không chính xác";
        if (cause instanceof LoginException) message=cause.getMessage();
        else if (cause instanceof LockedException)
            message="Tài khoản tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau "+LoginAttemptService.LOCK_TIME_DURATION+" phút.";
        // Provider-only accounts and activation/configuration errors are not bad passwords.
        if (exception instanceof BadCredentialsException) {
            String email=request.getParameter("username");
            if (email!=null) {
                User user=userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
                if (user!=null && Boolean.TRUE.equals(user.getActived()) && !Boolean.TRUE.equals(user.getDeleted())
                        && Boolean.TRUE.equals(user.getActivationStatus()) && user.isAccountNonLocked())
                    loginAttemptService.increaseFailedAttempts(user);
            }
        }
        request.getSession().setAttribute("loginError",message);
        response.sendRedirect("/login");
    }
}
