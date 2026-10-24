package com.example.sneaker.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PromotionApi {
  private PromotionApi() {}

  public record Input(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,30}") String code,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Pattern(regexp = "PERCENT|FIXED") String type,
      @NotNull @DecimalMin("1") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0)
          BigDecimal value,
      @NotNull @DecimalMin("0") @DecimalMax("100000000000") @Digits(integer = 12, fraction = 0)
          BigDecimal minSubtotal,
      @DecimalMin("1") @Digits(integer = 12, fraction = 0) BigDecimal maxDiscount,
      @Min(1) @Max(1000000) int maxUses,
      @NotNull Instant startsAt,
      @NotNull Instant endsAt,
      boolean active,
      @NotEmpty @Size(max = 500) List<@NotNull Long> productIds) {}

  public record View(
      Long id,
      String code,
      String name,
      String type,
      BigDecimal value,
      BigDecimal minSubtotal,
      BigDecimal maxDiscount,
      int maxUses,
      int usedCount,
      Instant startsAt,
      Instant endsAt,
      boolean active,
      List<Long> productIds) {}
}
