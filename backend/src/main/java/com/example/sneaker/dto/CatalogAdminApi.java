package com.example.sneaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class CatalogAdminApi {
  private CatalogAdminApi() {}

  public record AttributeInput(
      @NotBlank @Pattern(regexp = "BRAND|SOLE|MATERIAL|COLOR|SIZE") String type,
      @NotBlank @Size(max = 100) String name,
      boolean active) {}

  public record AttributeView(Long id, String code, String type, String name, boolean active) {}

  public record ColorImage(
      @NotBlank @Size(max = 60) String color, @NotNull java.util.UUID mediaId) {}

  public record Metadata(
      Long brandId,
      Long soleId,
      Long materialId,
      @Size(max = 100) List<@Valid ColorImage> images) {}

  public record ImageView(String color, String mediaId, String url, int displayOrder) {}

  public record Details(
      AttributeView brand,
      AttributeView sole,
      AttributeView material,
      List<ImageView> images,
      Instant createdAt,
      Instant updatedAt,
      String createdBy,
      String updatedBy) {}

  public record MediaView(
      String id,
      String url,
      String name,
      int width,
      int height,
      long byteSize,
      String uploadedBy,
      Instant createdAt) {}
}
