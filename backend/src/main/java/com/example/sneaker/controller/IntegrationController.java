package com.example.sneaker.controller;

import com.example.sneaker.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
public class IntegrationController {
  private final VnpayService vnpay;
  private final EmailOtpService otp;
  private final ShopAuthService auth;
  private final ShippingService shipping;

  public record Active(boolean active) {}

  @GetMapping("/api/admin/shop-accounts")
  public com.example.sneaker.dto.Api.PageView<ShopAuthService.AdminAccount> accounts(
      @RequestParam(required = false) @Size(max = 120) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return auth.list(q, page);
  }

  @PutMapping("/api/admin/shop-accounts/{id}")
  public void active(@PathVariable UUID id, @RequestBody Active input) {
    auth.active(id.toString(), input.active());
  }

  public record EmailInput(@NotBlank @Email @Size(max = 254) String email) {}

  public record Verify(
      @NotNull UUID challengeId, @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}

  public record Profile(
      @NotBlank @Size(max = 120) String fullName,
      @NotBlank @Pattern(regexp = "0[35789][0-9]{8}") String phone,
      @NotBlank @Size(min = 10, max = 500) String address) {}

  @GetMapping("/api/integrations")
  public Map<String, Object> config() {
    return Map.of(
        "vnpay", vnpay.enabled(), "emailOtp", otp.enabled(), "shipping", shipping.policy());
  }

  @PostMapping("/api/auth/otp")
  public EmailOtpService.Sent request(@Valid @RequestBody EmailInput input) {
    return otp.request(input.email());
  }

  @PostMapping("/api/auth/verify")
  public ShopAuthService.Login verify(@Valid @RequestBody Verify input) {
    return auth.login(input.challengeId().toString(), input.code());
  }

  @GetMapping("/api/me/profile")
  public ShopAuthService.Profile me() {
    return auth.me();
  }

  @PutMapping("/api/me/profile")
  public ShopAuthService.Profile me(@Valid @RequestBody Profile input) {
    return auth.update(input.fullName(), input.phone(), input.address());
  }

  @PostMapping("/api/me/logout")
  public void logout(@RequestHeader("Authorization") String header) {
    auth.logout(header.substring(7));
  }

  @GetMapping("/api/me/orders")
  public List<com.example.sneaker.dto.Api.OrderView> orders() {
    return auth.myOrders();
  }

  private Map<String, String> fields(HttpServletRequest request) {
    var values = new TreeMap<String, String>();
    request
        .getParameterMap()
        .forEach(
            (k, v) -> {
              if (!k.startsWith("vnp_") || v.length != 1 || v[0].length() > 1000)
                throw com.example.sneaker.service.CatalogService.bad("Tham số VNPAY không hợp lệ");
              values.put(k, v[0]);
            });
    return values;
  }

  @GetMapping("/api/payments/vnpay/ipn")
  public Map<String, String> ipn(HttpServletRequest request) {
    return vnpay.ipn(fields(request));
  }

  @GetMapping("/api/payments/vnpay/return")
  public Map<String, String> result(HttpServletRequest request) {
    return vnpay.result(fields(request));
  }
}
