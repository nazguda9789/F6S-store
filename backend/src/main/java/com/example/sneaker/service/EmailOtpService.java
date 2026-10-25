package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.model.EmailChallenge;
import com.example.sneaker.repository.EmailChallengeRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailOtpService {
  private final EmailChallengeRepository challenges;
  private final EmailDelivery delivery;
  private final boolean enabled;
  private final String secret;
  private final SecureRandom random = new SecureRandom();

  public EmailOtpService(
      EmailChallengeRepository challenges,
      EmailDelivery delivery,
      @Value("${app.otp.enabled:false}") boolean enabled,
      @Value("${app.otp.secret:}") String secret) {
    this.challenges = challenges;
    this.delivery = delivery;
    this.enabled = enabled;
    this.secret = secret;
    if (enabled && secret.length() < 32)
      throw new IllegalArgumentException("OTP_SECRET cần tối thiểu 32 ký tự ngẫu nhiên");
  }

  public boolean enabled() {
    return enabled;
  }

  @Transactional(readOnly = true)
  public String emailForGrant(String grant) {
    return challenges
        .findById(grant.split("\\.")[0])
        .orElseThrow(() -> bad("Phiên không hợp lệ"))
        .getEmail();
  }

  private String email(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public record Sent(String challengeId, Instant expiresAt) {}

  public record Verified(String token, Instant expiresAt) {}

  @Transactional
  public synchronized Sent request(String email) {
    if (!enabled)
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OTP email chưa được bật");
    email = email(email);
    Instant now = Instant.now();
    if (challenges.countByEmailAndCreatedAtAfter(email, now.minusSeconds(60)) > 0
        || challenges.countByEmailAndCreatedAtAfter(email, now.minusSeconds(3600)) >= 5
        || challenges.countByCreatedAtAfter(now.minusSeconds(3600)) >= 200)
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ trước khi yêu cầu mã mới");
    var c = new EmailChallenge();
    c.setId(UUID.randomUUID().toString());
    c.setEmail(email);
    String code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
    c.setCodeHash(VnpayService.hmac(secret, c.getId() + ":" + email + ":" + code).substring(0, 64));
    c.setExpiresAt(now.plusSeconds(300));
    challenges.saveAndFlush(c);
    delivery.otp(email, code);
    return new Sent(c.getId(), c.getExpiresAt());
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public Verified verify(String id, String code) {
    if (!enabled) throw bad("OTP chưa được bật");
    var c = challenges.lockId(id).orElseThrow(() -> bad("Mã xác minh không hợp lệ"));
    if (!c.getExpiresAt().isAfter(Instant.now())
        || c.getAttempts() >= 5
        || c.getVerifiedAt() != null
        || c.getConsumedAt() != null) throw bad("Mã đã hết hạn, hết lượt hoặc đã sử dụng");
    c.setAttempts(c.getAttempts() + 1);
    String hash =
        VnpayService.hmac(secret, c.getId() + ":" + c.getEmail() + ":" + code).substring(0, 64);
    if (!MessageDigest.isEqual(
        hash.getBytes(StandardCharsets.US_ASCII),
        c.getCodeHash().getBytes(StandardCharsets.US_ASCII))) throw bad("Mã xác minh không đúng");
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    c.setGrantHash(sha(token));
    c.setVerifiedAt(Instant.now());
    c.setExpiresAt(Instant.now().plusSeconds(600));
    return new Verified(c.getId() + "." + token, c.getExpiresAt());
  }

  @Transactional
  public void consume(String email, String grant) {
    if (!enabled) return;
    if (grant == null || !grant.matches("[a-f0-9-]{36}\\.[a-f0-9]{64}"))
      throw bad("Cần xác minh email trước khi đặt hàng");
    var pieces = grant.split("\\.");
    var c = challenges.lockId(pieces[0]).orElseThrow(() -> bad("Xác minh email không hợp lệ"));
    if (c.getVerifiedAt() == null
        || c.getConsumedAt() != null
        || !c.getExpiresAt().isAfter(Instant.now())
        || !c.getEmail().equals(email(email))
        || !MessageDigest.isEqual(
            sha(pieces[1]).getBytes(StandardCharsets.US_ASCII),
            c.getGrantHash().getBytes(StandardCharsets.US_ASCII)))
      throw bad("Phiên xác minh email hết hạn hoặc không phù hợp");
    c.setConsumedAt(Instant.now());
  }

  private String sha(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
