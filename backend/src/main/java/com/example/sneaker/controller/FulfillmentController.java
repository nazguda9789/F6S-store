package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class FulfillmentController {
  private final OrderManagementService orders;
  private final ShopAuthService auth;

  public record Cancel(@NotBlank @Size(min = 5, max = 500) String reason) {}

  public record Decision(
      boolean approve,
      @NotBlank @Size(min = 5, max = 490) String reason,
      boolean waybillCancelled) {}

  public record Reconcile(
      @NotBlank @Size(min = 5, max = 120) String reference,
      @AssertTrue boolean verifiedUnpaid,
      boolean waybillCancelled) {}

  @PostMapping("/api/me/orders/{code}/cancellation")
  public OrderView request(
      @PathVariable @Size(max = 40) String code, @Valid @RequestBody Cancel input) {
    return orders.requestCancellation(code, auth.current().getId(), input.reason());
  }

  @PutMapping("/api/admin/orders/{id}/cancellation")
  public OrderView decide(
      @PathVariable Long id, @Valid @RequestBody Decision input, Principal actor) {
    return orders.decideCancellation(
        id, input.approve(), input.reason(), actor.getName(), input.waybillCancelled());
  }

  @GetMapping("/api/admin/payment-expiry")
  public PageView<OrderManagementService.Entry> overdue(
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return orders.overdue(page);
  }

  @PostMapping("/api/admin/orders/{id}/reconcile-expiry")
  public OrderView reconcile(
      @PathVariable Long id, @Valid @RequestBody Reconcile input, Principal actor) {
    return orders.expireAfterReconciliation(
        id, input.reference(), actor.getName(), input.waybillCancelled());
  }
}
