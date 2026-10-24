package com.example.sneaker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardApi {
  private DashboardApi() {}

  public record Bucket(String label, BigDecimal revenue, long completedOrders) {}

  public record TopProduct(
      Long productId, String code, String name, long quantity, BigDecimal grossRevenue) {}

  public record Dashboard(
      LocalDate from,
      LocalDate to,
      String zone,
      BigDecimal revenue,
      long totalOrders,
      long successfulOrders,
      BigDecimal successRate,
      List<Bucket> buckets,
      List<TopProduct> topProducts) {}
}
