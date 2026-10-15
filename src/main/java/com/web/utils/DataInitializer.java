package com.web.utils;

import com.web.entity.User;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        String password = environment.getProperty("F6S_ADMIN_PASSWORD", "");
        String email = environment.getProperty("F6S_ADMIN_EMAIL", "").trim();

        if (email.isBlank() || password.isBlank()) {
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if (!environment.getProperty("F6S_ADMIN_RESET_PASSWORD", Boolean.class, false)) {
                return;
            }
            if (user.getRole() != Role.ROLE_ADMIN) {
                throw new IllegalStateException("Password reset requires an existing admin email.");
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setFullName("ADMIN");
            user.setRole(Role.ROLE_ADMIN);
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setActived(true);
        user.setDeleted(false);
        user.setActivationStatus(true);
        user.setUserType(UserType.NORMAL);
        user.setIsGoogleUser(false);
        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockTime(null);
        user.setRememberKey(null);
        userRepository.save(user);
    }
}
