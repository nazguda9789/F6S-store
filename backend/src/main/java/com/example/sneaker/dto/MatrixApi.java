package com.example.sneaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class MatrixApi {
  private MatrixApi() {}

  public record Selection(
      @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 60) String> colors,
      @NotEmpty @Size(max = 20)
          List<@NotBlank @Pattern(regexp = "(?:3[5-9]|4[0-9]|50)(?:\\.5)?") String> sizes,
      @Min(0) @Max(100000) int defaultStock,
      @NotNull @DecimalMin("1") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0)
          BigDecimal defaultPrice,
      @NotNull @Size(max = 100) List<@Valid Override> overrides) {}

  public record Override(
      @NotBlank @Size(max = 60) String color,
      @NotBlank String size,
      @NotNull @Min(0) @Max(100000) Integer stock,
      @NotNull @DecimalMin("1") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0)
          BigDecimal price) {}

  public record Create(
      @NotBlank @Size(max = 180) String name,
      @NotBlank @Size(max = 100) String brand,
      @NotBlank @Size(max = 4000) String description,
      @NotBlank @Size(max = 1000) @Pattern(regexp = "(?:https://[^\\s]+|/[^\\s]*)") String imageUrl,
      @Size(max = 1000) @Pattern(regexp = "(?:https://[^\\s]+|/[^\\s]*)?") String hoverImageUrl,
      @NotNull Long categoryId,
      boolean featured,
      @NotNull @Valid Selection matrix) {}
}
