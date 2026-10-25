package com.example.sneaker.config;

import com.example.sneaker.service.ShopAuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CustomerBearerFilter extends OncePerRequestFilter {
  private final ShopAuthService auth;

  public CustomerBearerFilter(ShopAuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String id = auth.authenticate(header.substring(7));
      if (id == null) {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Phiên khách hàng hết hạn\"}");
        return;
      }
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  id, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
    chain.doFilter(request, response);
  }
}
