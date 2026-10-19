package com.web.utils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@EnableAsync
public class MailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String username;

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public MailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }


    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        try {
            sendEmailRequired(to, subject, content, isMultipart, isHtml);
        } catch (com.web.exception.MessageException ex) {
            log.warn("Optional email was not sent; check SMTP configuration.");
        }
    }

    public void requireConfigured() {
        if (!org.springframework.util.StringUtils.hasText(username)
                || (javaMailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl sender
                && Boolean.parseBoolean(sender.getJavaMailProperties().getProperty("mail.smtp.auth", "true"))
                && !org.springframework.util.StringUtils.hasText(sender.getPassword()))) {
            throw new com.web.exception.MessageException("Chưa cấu hình email gửi mã. Quản trị viên cần đặt MAIL_USERNAME và MAIL_PASSWORD (App Password), rồi khởi động lại web.");
        }
    }

    // Synchronous: only report success after SMTP accepts the message.
    public void sendEmailRequired(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        requireConfigured();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(username);
            message.setSubject(subject);
            message.setText(content, isHtml);

            javaMailSender.send(mimeMessage);
        } catch (MailException | MessagingException e) {
            // Never log OTP, credentials, message body or provider exception text.
            log.warn("SMTP delivery failed ({})", e.getClass().getSimpleName());
            throw new com.web.exception.MessageException("Không gửi được email xác thực. Vui lòng thử lại sau; quản trị viên cần kiểm tra SMTP/App Password và kết nối mạng.");
        }
    }
}
