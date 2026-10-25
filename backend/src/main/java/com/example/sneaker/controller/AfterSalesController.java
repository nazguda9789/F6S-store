package com.example.sneaker.controller;

import com.example.sneaker.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders/{id}")
@RequiredArgsConstructor
public class AfterSalesController {
  private final RefundService refunds;
  private final OrderManagementService management;

  public record Shipment(
      @NotBlank @Size(max = 60) String carrier,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,100}") String trackingCode) {}

  public record Request(@NotBlank @Size(min = 5, max = 500) String reason) {}

  public record Resolve(
      boolean approve, boolean goodsReceived, @Size(max = 120) String settlementReference) {}

  @PutMapping("/shipment")
  public OrderManagementService.Entry shipment(
      @PathVariable Long id, @Valid @RequestBody Shipment input, Principal user) {
    return management.shipment(id, input.carrier(), input.trackingCode(), user.getName());
  }

  @GetMapping("/refund")
  public RefundService.View refund(@PathVariable Long id) {
    return refunds.detail(id);
  }

  @PostMapping("/refund")
  public RefundService.View refund(
      @PathVariable Long id, @Valid @RequestBody Request input, Principal user) {
    return refunds.request(id, input.reason(), user.getName());
  }

  @PutMapping("/refund")
  public RefundService.View refund(
      @PathVariable Long id, @Valid @RequestBody Resolve input, Principal user) {
    return refunds.resolve(
        id, input.approve(), input.goodsReceived(), input.settlementReference(), user.getName());
  }
}
