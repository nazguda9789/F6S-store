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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  UserDetailsService users(
      @Value("${app.admin-username}") String username,
      @Value("${app.admin-password}") String password) {
    if (password.length() < 12)
      throw new IllegalArgumentException("ADMIN_PASSWORD cần ít nhất 12 ký tự");
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password))
            .roles("ADMIN")
            .build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET, "/api/products/**", "/api/categories", "/api/facets")
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
