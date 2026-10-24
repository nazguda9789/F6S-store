package com.example.sneaker.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class PosApi {
  private PosApi() {}

  public record NewDraft(@NotNull UUID requestKey) {}

  public record Scan(
      @NotNull UUID eventKey,
      @NotBlank @Size(max = 120) String code,
      @Min(1) @Max(99) int quantity) {}

  public record Quantity(@Min(1) @Max(99) int quantity) {}

  public record Payment(
      @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 0) BigDecimal cashReceived,
      @Size(max = 30) String promotionCode,
      @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 0) BigDecimal expectedTotal) {
    public Payment(BigDecimal cashReceived, String promotionCode) {
      this(cashReceived, promotionCode, null);
    }
  }

  public record QuoteInput(@Size(max = 30) String promotionCode) {}

  public record Line(
      Long variantId,
      String sku,
      String name,
      String color,
      String size,
      int stock,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal,
      String imageUrl) {}

  public record Draft(
      Long id, String status, Instant createdAt, List<Line> lines, BigDecimal subtotal) {}

  public record Quote(
      BigDecimal subtotal, BigDecimal discount, BigDecimal total, String promotionCode) {}
}
