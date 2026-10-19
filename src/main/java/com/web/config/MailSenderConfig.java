package com.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailSenderConfig {
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:${MAIL_USERNAME:}}") private String username;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.password:${MAIL_PASSWORD:}}") private String password;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:smtp.gmail.com}") private String host;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.port:587}") private int port;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.properties.mail.smtp.auth:true}") private boolean auth;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") private boolean starttls;

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
