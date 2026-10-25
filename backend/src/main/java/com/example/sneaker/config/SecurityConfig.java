package com.example.sneaker.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  UserDetailsService users(
      @Value("${app.admin-username}") String username,
      @Value("${app.admin-password}") String password,
      com.example.sneaker.repository.StaffRepository staff) {
    if (password.length() < 12)
      throw new IllegalArgumentException("ADMIN_PASSWORD cần ít nhất 12 ký tự");
    var admin =
        User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password))
            .roles("ADMIN")
            .build();
    return requested -> {
      if (requested.equalsIgnoreCase(username)) return User.withUserDetails(admin).build();
      var account =
          staff
              .findByUsername(requested.toLowerCase(java.util.Locale.ROOT))
              .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ"));
      return User.withUsername(account.getUsername())
          .password(account.getPasswordHash())
          .roles("STAFF")
          .disabled(!account.isActive())
          .build();
    };
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http, com.example.sneaker.service.ShopAuthService customerAuth)
      throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            new CustomerBearerFilter(customerAuth),
            org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/admin/session", "/api/admin/pos/**", "/api/admin/customers/**")
                    .hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers("/api/me/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers(HttpMethod.POST, "/api/auth/otp", "/api/auth/verify")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/integrations",
                        "/api/payments/vnpay/ipn",
                        "/api/payments/vnpay/return")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/products/**",
                        "/api/categories",
                        "/api/facets",
                        "/api/media/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/orders")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(
            basic ->
                basic.authenticationEntryPoint(
                    (request, response, error) -> {
                      response.setStatus(401);
                      response.setContentType("application/json;charset=UTF-8");
                      response
                          .getWriter()
                          .write(
                              "{\"message\":\"Thông tin quản trị không hợp lệ hoặc phiên đã kết"
                                  + " thúc\"}");
                    }))
        .build();
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.cors-origin}") String origin) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(origin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    config.setAllowCredentials(false);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
