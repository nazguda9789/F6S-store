package com.example.sneaker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class EmailDelivery {
  private final JavaMailSenderImpl sender = new JavaMailSenderImpl();
  private final String from;

  public EmailDelivery(
      @Value("${app.smtp.host:}") String host,
      @Value("${app.smtp.port:587}") int port,
      @Value("${app.smtp.username:}") String username,
      @Value("${app.smtp.password:}") String password,
      @Value("${app.smtp.from:}") String from,
      @Value("${app.smtp.starttls:true}") boolean starttls) {
    if (!starttls && !java.util.Set.of("localhost", "127.0.0.1", "::1").contains(host))
      throw new IllegalArgumentException("Chỉ cho phép SMTP không TLS với máy chủ demo localhost");
    this.from = from;
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    var p = sender.getJavaMailProperties();
    p.setProperty("mail.smtp.auth", Boolean.toString(!username.isBlank()));
    p.setProperty("mail.smtp.starttls.enable", Boolean.toString(starttls));
    p.setProperty("mail.smtp.starttls.required", Boolean.toString(starttls));
    p.setProperty("mail.smtp.connectiontimeout", "5000");
    p.setProperty("mail.smtp.timeout", "5000");
    p.setProperty("mail.smtp.writetimeout", "5000");
  }

  public void otp(String email, String code) {
    if (sender.getHost() == null || sender.getHost().isBlank() || from.isBlank())
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "SMTP chưa được cấu hình");
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("F6S — Mã đăng nhập tài khoản");
    message.setText(
        "Mã đăng ký/đăng nhập F6S của bạn: "
            + code
            + "\n"
            + "Hết hạn sau 5 phút. Không chia sẻ mã với người khác. Nếu bạn không yêu cầu, hãy bỏ"
            + " qua email này.");
    try {
      sender.send(message);
    } catch (org.springframework.mail.MailException e) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Không gửi được email. Vui lòng thử lại sau.");
    }
  }
}
