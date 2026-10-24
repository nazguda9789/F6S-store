package com.example.sneaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Api {
  private Api() {}

  public record CategoryInput(@NotBlank @Size(max = 100) String name) {}

  public record CategoryView(Long id, String name) {}

  public record VariantInput(
      @NotBlank @Size(max = 60) String color,
      @NotBlank @Pattern(regexp = "(?:3[5-9]|4[0-9]|50)(?:\\.5)?") String size,
      @Min(0) @Max(100000) int stock,
      @DecimalMin("1") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0)
          BigDecimal price) {
    public VariantInput(String color, String size, int stock) {
      this(color, size, stock, null);
    }
  }

  public record ProductInput(
      @NotBlank @Size(max = 180) String name,
      @NotBlank @Size(max = 100) String brand,
      @NotNull @DecimalMin("1") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0)
          BigDecimal price,
      @NotBlank @Size(max = 4000) String description,
      @NotBlank @Size(max = 1000) @Pattern(regexp = "(?:https://[^\\s]+|/[^\\s]*)") String imageUrl,
      @Size(max = 1000) @Pattern(regexp = "(?:https://[^\\s]+|/[^\\s]*)?") String hoverImageUrl,
      @NotNull Long categoryId,
      boolean featured,
      @NotEmpty @Size(max = 100) List<@Valid VariantInput> variants) {}

  public record VariantView(
      Long id,
      String sku,
      String color,
      String size,
      int stock,
      BigDecimal price,
      String qrPayload) {}

  public record ProductView(
      Long id,
      String code,
      String name,
      String brand,
      BigDecimal price,
      int stock,
      String description,
      String imageUrl,
      String hoverImageUrl,
      boolean featured,
      CategoryView category,
      List<String> colorways,
      List<VariantView> variants) {}

  public record PageView<T>(List<T> content, int page, int totalPages, long totalElements) {}

  public record CheckoutLine(@NotNull Long variantId, @Min(1) @Max(99) int quantity) {}

  public record Checkout(
      @NotNull java.util.UUID requestKey,
      @NotBlank @Size(max = 120) String customerName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "0[35789][0-9]{8}") String phone,
      @NotBlank @Size(min = 10, max = 500) String address,
      @Size(max = 500) String note,
      @NotEmpty @Size(max = 50) List<@Valid CheckoutLine> items,
      @Size(max = 30) String promotionCode) {
    public Checkout(
        java.util.UUID requestKey,
        String customerName,
        String email,
        String phone,
        String address,
        String note,
        List<CheckoutLine> items) {
      this(requestKey, customerName, email, phone, address, note, items, null);
    }
  }

  public record OrderView(
      String code,
      String status,
      String paymentMethod,
      BigDecimal total,
      Instant createdAt,
      List<OrderLineView> items,
      BigDecimal subtotal,
      BigDecimal discount,
      String promotionCode,
      String channel,
      BigDecimal cashReceived,
      BigDecimal cashChange) {}

  public record OrderLineView(
      String productName,
      String sku,
      String color,
      String size,
      int quantity,
      BigDecimal unitPrice) {}
}
