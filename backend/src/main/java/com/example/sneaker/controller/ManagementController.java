package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.dto.DashboardApi.Dashboard;
import com.example.sneaker.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class ManagementController {
  private final DashboardService dashboard;
  private final OrderManagementService orders;

  @GetMapping("/dashboard")
  public Dashboard dashboard(
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(defaultValue = "day") String group) {
    return dashboard.report(from, to, group);
  }

  @GetMapping("/orders")
  public PageView<OrderManagementService.Entry> orders(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String channel,
      @RequestParam(required = false) @Size(max = 120) String q,
      @RequestParam(required = false) String paymentStatus,
      @RequestParam(required = false) String cancellationStatus) {
    return orders.list(page, status, channel, q, paymentStatus, cancellationStatus);
  }

  @GetMapping("/orders/{id}")
  public OrderManagementService.Entry detail(@PathVariable Long id) {
    return orders.detail(id);
  }

  public record Status(
      @NotBlank
          @Pattern(
              regexp =
                  "CONFIRMED|PACKING|SHIPPING|COMPLETED|CANCELLED|DELIVERY_FAILED|RETURNING|RETURNED")
          String status,
      @Size(max = 500) String reason,
      @Size(max = 30) String expectedStatus,
      boolean goodsReceived,
      boolean waybillCancelled) {}

  @PutMapping("/orders/{id}/status")
  public OrderView status(
      @PathVariable Long id, @Valid @RequestBody Status input, java.security.Principal user) {
    return orders.status(
        id,
        input.status(),
        user.getName(),
        input.reason(),
        input.expectedStatus(),
        input.goodsReceived(),
        input.waybillCancelled());
  }
}
