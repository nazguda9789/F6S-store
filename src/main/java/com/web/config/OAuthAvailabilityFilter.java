package com.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class OAuthAvailabilityFilter extends OncePerRequestFilter {
    private final Environment environment;
    public OAuthAvailabilityFilter(Environment environment) { this.environment=environment; }
    public static boolean available(Environment environment,String provider) {
        String prefix="spring.security.oauth2.client.registration."+provider;
        return StringUtils.hasText(environment.getProperty(prefix+".client-id"))
                && StringUtils.hasText(environment.getProperty(prefix+".client-secret"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String path=request.getServletPath();
        if (path.equals("/oauth2/authorization/google") || path.equals("/oauth2/authorization/facebook")) {
            String provider=path.substring(path.lastIndexOf('/')+1);
            if (!available(environment,provider)) {
                request.getSession().setAttribute("loginError","Đăng nhập "+provider+" chưa được cấu hình. Vui lòng dùng email/mật khẩu hoặc liên hệ quản trị viên.");
                response.sendRedirect("/login"); return;
            }
        }
        chain.doFilter(request,response);
    }
}
