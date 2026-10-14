package com.web.config;


import com.web.doss.LoginAttemptService;
import com.web.dto.CustomUserDetails;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.exception.LoginException;
import com.web.exception.MessageException;
import com.web.exception.UserNotActivatedException;
import com.web.repository.UserRepository;
import com.web.service.CustomOAuth2UserService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig  extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Autowired
    private CustomLoginSuccessHandler customLoginSuccessHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(TypeSystem.ADMIN_URL+"/api/**").hasAnyAuthority(Role.ROLE_ADMIN.toString(), Role.ROLE_EMPLOYEE.toString())
                        .requestMatchers(TypeSystem.ADMIN_URL+"/**").hasAuthority(Role.ROLE_ADMIN.toString())
                        .requestMatchers(TypeSystem.ADMIN_URL+"/api/**").hasAnyAuthority(Role.ROLE_ADMIN.toString(), Role.ROLE_EMPLOYEE.toString())
                        .requestMatchers(TypeSystem.EMPLOYEE_URL+"/**").hasAuthority(Role.ROLE_EMPLOYEE.toString())
                        .requestMatchers("/tai-khoan").hasAuthority(Role.ROLE_USER.toString())
                        .requestMatchers( "/error","/ws/**").permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .failureHandler(customAuthenticationFailureHandler)
                        .successHandler(customLoginSuccessHandler)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("fhwuiefhiurhfrghiureghslkfjie123") // key mã hóa cookie
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                        .rememberMeParameter("remember-me") // tên param trong form login
                )
//                .rememberMe(remember -> remember
//                        .key("fhwuiefhiurhfrghiureghslkfjie123") // key mã hóa cookie
//                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
//                        .alwaysRemember(true)
//                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // dùng service ở trên
                        )
                        .successHandler(customLoginSuccessHandler)
                )
//                .rememberMe(remember -> remember
//                        .key("fhwuiefhiurhfrghiureghslkfjie123") // key mã hóa cookie
//                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
//                        .alwaysRemember(true)
//                )
                .logout(logout -> logout
                        .logoutUrl("/logout")  // URL xử lý logout
                        .logoutSuccessUrl("/login")  // Chuyển hướng sau khi logout thành công
                        .invalidateHttpSession(true)  // Xóa session
                        .deleteCookies("JSESSIONID")  // Xóa cookie nếu cần
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider())
//                .exceptionHandling(exceptionHandling ->
//                        exceptionHandling
//                                .authenticationEntryPoint((request, response, authException) -> {
//                                    if (authException instanceof UserNotActivatedException) {
//                                        String email = ((UserNotActivatedException) authException).getEmail();
//                                        System.out.println("active exception");
//                                        response.sendRedirect("/confirm?email="+email);
//                                    } else {
//                                        System.out.println("another exception");
//                                        response.sendRedirect("/login?error");
//                                    }
//                                })
//                )
                .build();
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // chỉ cho ADMIN, NHANVIEN subscribe topic
                .simpSubscribeDestMatchers("/topic/notifications","/topic/order")
                .hasAnyAuthority(Role.ROLE_ADMIN.toString(), Role.ROLE_EMPLOYEE.toString())

                .anyMessage().permitAll();
    }

    // tắt CSRF cho SockJS
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<com.web.entity.User> user = userRepository.findByEmail(username);
            if (user.isEmpty()) {
                throw new LoginException("Tài khoản hoặc mật khẩu không chính xác", username);
            }
            else if (user.get().getActivationStatus() != null && !user.get().getActivationStatus()) {
                throw new UserNotActivatedException("Tài khoản chưa được kích hoạt", user.get().getEmail());
            }
            else if (!user.get().getActived()) {
                throw new LoginException("Tài khoản đã bị khóa do phát hiện nghi ngờ", user.get().getEmail());
            }
            else if (user.get().getUserType().equals(UserType.GOOGLE)) {
                throw new LoginException("Hãy login bằng google", username);
            }
            else if (user.get().getUserType().equals(UserType.FACEBOOK)) {
                throw new LoginException("Hãy login bằng facebook", username);
            }

            // Kiểm tra lock
            if (!user.get().isAccountNonLocked()) {
                // Nếu tài khoản đang bị khóa → thử mở khóa nếu đã hết hạn
                if (loginAttemptService.unlockWhenTimeExpired(user.get())) {
                    // user đã được mở khóa, tiếp tục login bình thường
                }
                else {
                    throw new LockedException("Tài khoản bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau "+LoginAttemptService.LOCK_TIME_DURATION+" phút.");
                }
            }


            List<String> roles = new ArrayList<>();
            roles.add(user.get().getRole().toString());
            List<GrantedAuthority> grantList = new ArrayList<GrantedAuthority>();
            if (roles != null) {
                for (String role : roles) {
                    GrantedAuthority authority = new SimpleGrantedAuthority(role);
                    grantList.add(authority);
                }
            }

            String password = user.get().getPassword();
//            UserDetails userDetails = (UserDetails) new User(username,
//                    password, grantList);
            UserDetails userDetails = (UserDetails) new CustomUserDetails(username,
                    password, user.get().getFullName(), grantList);


            return userDetails;
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



}
