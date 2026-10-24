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
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return orders.list(page);
  }

  public record Status(@NotBlank @Pattern(regexp = "COMPLETED|CANCELLED") String status) {}

  @PutMapping("/orders/{id}/status")
  public OrderView status(@PathVariable Long id, @Valid @RequestBody Status input) {
    return orders.status(id, input.status());
  }
}
