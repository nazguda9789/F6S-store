package com.example.sneaker.service;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShopAuthService {
  private final EmailOtpService otp;
  private final ShopAccountRepository accounts;
  private final ShopSessionRepository sessions;
  private final OrderRepository orders;

  public record Profile(String email, String fullName, String phone, String address) {}

  public record Login(String token, Instant expiresAt, Profile profile) {}

  public record AdminAccount(
      String id, String email, String fullName, String phone, boolean active, Instant createdAt) {}

  @Transactional(readOnly = true)
  public com.example.sneaker.dto.Api.PageView<AdminAccount> list(String q, int page) {
    var result =
        accounts.findAll(
            (root, query, cb) -> {
              if (q == null || q.isBlank()) return cb.conjunction();
              String key =
                  "%"
                      + q.trim()
                          .toLowerCase(Locale.ROOT)
                          .replace("\\", "\\\\")
                          .replace("%", "\\%")
                          .replace("_", "\\_")
                          .replace("[", "\\[")
                      + "%";
              return cb.or(
                  cb.like(cb.lower(root.get("email")), key, '\\'),
                  cb.like(cb.lower(root.get("fullName")), key, '\\'));
            },
            org.springframework.data.domain.PageRequest.of(
                page, 20, org.springframework.data.domain.Sort.by("createdAt").descending()));
    return new com.example.sneaker.dto.Api.PageView<>(
        result.getContent().stream()
            .map(
                a ->
                    new AdminAccount(
                        a.getId(),
                        a.getEmail(),
                        a.getFullName(),
                        a.getPhone(),
                        a.isActive(),
                        a.getCreatedAt()))
            .toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  @Transactional
  public void active(String id, boolean active) {
    var a =
        accounts
            .findById(id)
            .orElseThrow(
                () ->
                    com.example.sneaker.service.CatalogService.missing("Không tìm thấy tài khoản"));
    a.setActive(active);
    if (!active) sessions.deleteByAccountId(id);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public Login login(String id, String code) {
    var grant = otp.verify(id, code);
    String email = otp.emailForGrant(grant.token());
    otp.consume(email, grant.token());
    var account =
        accounts
            .findByEmail(email)
            .orElseGet(
                () -> {
                  var a = new ShopAccount();
                  a.setId(UUID.randomUUID().toString());
                  a.setEmail(email);
                  return accounts.saveAndFlush(a);
                });
    if (!account.isActive())
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã ngừng hoạt động");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    var session = new ShopSession();
    session.setTokenHash(hash(token));
    session.setAccount(account);
    session.setExpiresAt(Instant.now().plusSeconds(86400));
    sessions.deleteExpired(Instant.now());
    sessions.save(session);
    return new Login(token, session.getExpiresAt(), profile(account));
  }

  @Transactional(readOnly = true)
  public String authenticate(String token) {
    if (token == null || !token.matches("[a-f0-9]{64}")) return null;
    var s = sessions.findById(hash(token)).orElse(null);
    return s != null && s.getExpiresAt().isAfter(Instant.now()) && s.getAccount().isActive()
        ? s.getAccount().getId()
        : null;
  }

  public ShopAccount current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")))
      return null;
    return accounts
        .findById(auth.getName())
        .filter(ShopAccount::isActive)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đã kết thúc"));
  }

  private ShopAccount required() {
    var a = current();
    if (a == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần đăng nhập khách hàng");
    return a;
  }

  private Profile profile(ShopAccount a) {
    return new Profile(a.getEmail(), a.getFullName(), a.getPhone(), a.getAddress());
  }

  @Transactional(readOnly = true)
  public Profile me() {
    return profile(required());
  }

  @Transactional
  public Profile update(String name, String phone, String address) {
    var a = required();
    a.setFullName(name.trim());
    a.setPhone(phone);
    a.setAddress(address.trim());
    return profile(a);
  }

  @Transactional
  public void logout(String token) {
    if (token != null && token.matches("[a-f0-9]{64}")) sessions.deleteById(hash(token));
  }

  @Transactional(readOnly = true)
  public java.util.List<com.example.sneaker.dto.Api.OrderView> myOrders() {
    return orders.findTop50ByShopAccountIdOrderByCreatedAtDesc(required().getId()).stream()
        .map(CheckoutService::view)
        .toList();
  }
}
