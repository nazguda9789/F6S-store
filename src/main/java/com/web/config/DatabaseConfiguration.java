package com.web.config;

import com.web.utils.FormatUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {"com.web.repository"})
@EnableTransactionManagement
@EnableJpaAuditing

public class DatabaseConfiguration {

    @Bean(name = "formatUtils")
    public FormatUtils formatUtils() {
        return new FormatUtils();
    }


}

