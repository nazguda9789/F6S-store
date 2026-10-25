package com.example.sneaker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Single versioned source for the Java bootstrap and the downloadable SQL catalog. */
@Component
public class ReferenceCatalog {
  public record Color(String name, String code) {}

  public record Row(
      String code,
      String name,
      String brand,
      String category,
      BigDecimal samplePrice,
      boolean featured,
      String brandUrl) {}

  public record Dataset(
      String release,
      String notice,
      List<String> sizes,
      List<Color> colors,
      String imageUrl,
      List<Row> products) {}

  private final Dataset data;

  public ReferenceCatalog(ObjectMapper mapper) throws IOException {
    try (var input = new ClassPathResource("catalog/real-products.json").getInputStream()) {
      data = mapper.readValue(input, Dataset.class);
    }
    if (data.products().size() != 25
        || data.products().stream().map(Row::code).distinct().count() != 25)
      throw new IllegalStateException("Catalog must contain 25 unique product codes");
    if (data.products().stream()
        .anyMatch(
            p ->
                p.samplePrice() == null
                    || p.samplePrice().signum() <= 0
                    || !p.code().matches("F6S-RC-[0-9]{3}")))
      throw new IllegalStateException("Invalid catalog code or sample price");
  }

  public Dataset data() {
    return data;
  }

  public static String sku(Row row, Color color, String size) {
    return "SKU-RC" + row.code().substring(7) + "-" + color.code() + "-" + size.replace(".", "P");
  }

  public static int stock(Color color, String size) {
    return color.code().equals("BLK") && size.equals("42") ? 0 : 10;
  }

  public static String description(Row row, Dataset data) {
    return row.name()
        + " — model của "
        + row.brand()
        + ". Nhóm sử dụng trong catalog F6S: "
        + row.category()
        + ". "
        + data.notice()
        + " Mã F6S/SKU là mã nội bộ, không phải mã phối màu của hãng. Website hãng để tham khảo: "
        + row.brandUrl();
  }
}
