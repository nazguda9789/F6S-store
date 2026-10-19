package com.web.config;

import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                   AuthenticationException exception) throws IOException {
        String code=exception instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode() : "";
        String message=switch(code) {
            case "email_missing", "email_unverified" -> "Google chưa cung cấp email đã xác minh. Vui lòng chọn tài khoản có email được xác minh.";
            case "account_unavailable" -> "Tài khoản đã bị khóa, xóa hoặc chưa được phép đăng nhập. Vui lòng liên hệ quản trị viên.";
            case "account_link_required" -> "Email này đã có tài khoản. Vui lòng dùng mật khẩu hoặc Google để đăng nhập.";
            case "access_denied" -> "Bạn đã hủy đăng nhập Google/Facebook. Có thể thử lại hoặc dùng email và mật khẩu.";
            default -> "Không hoàn tất đăng nhập Google/Facebook. Vui lòng thử lại; quản trị viên cần kiểm tra Client ID, Client Secret và URL chuyển hướng.";
        };
        new HttpSessionRequestCache().removeRequest(request,response);
        request.getSession().setAttribute("loginError",message);
        response.sendRedirect("/login");
    }
}
