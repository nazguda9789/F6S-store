package com.example.sneaker.config;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.repository.CategoryRepository;
import com.example.sneaker.service.CatalogService;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private final CatalogService catalog;
  private final CategoryRepository categories;

  @Override
  @Transactional
  public void run(String... args) {
    if (categories.count() != 0) return;
    String[] groups = {"Chạy bộ", "Tập luyện", "Bóng rổ", "Đi bộ", "Sneaker"};
    String[] names = {"Velocity Run", "Power Train", "Court Jump", "Daily Walk", "Urban Move"};
    String[] brands = {"Nike", "Adidas", "Puma", "New Balance", "Converse"};
    for (int i = 0; i < groups.length; i++) {
      var c = catalog.createCategory(new CategoryInput(groups[i]));
      for (int j = 1; j <= 5; j++) {
        var variants = new ArrayList<VariantInput>();
        for (String color : List.of("Đen", "Trắng"))
          for (int size = 38; size <= 42; size++)
            variants.add(
                new VariantInput(color, "" + size, size == 42 && color.equals("Đen") ? 0 : 10));
        catalog.create(
            new ProductInput(
                names[i] + " " + String.format("%02d", j),
                brands[i],
                BigDecimal.valueOf(790000L + i * 200000L + j * 50000L),
                "Giày thể thao mẫu F6S, đệm êm và thiết kế linh hoạt. Dữ liệu minh họa, không phải"
                    + " thông số sản phẩm chính hãng.",
                "/images/shoe-" + i + ".svg",
                "/images/shoe-" + ((i + 1) % 5) + ".svg",
                c.id(),
                j == 1,
                variants));
      }
    }
  }
}
