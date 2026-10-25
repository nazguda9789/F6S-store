package com.example.sneaker.config;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
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
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final ReferenceCatalog catalog;

  @Override
  @Transactional
  public void run(String... args) {
    if (categories.count() != 0) return;
    var data = catalog.data();
    var groups = new HashMap<String, Category>();
    for (var row : data.products()) {
      var category =
          groups.computeIfAbsent(
              row.category(),
              name -> {
                var c = new Category();
                c.setName(name);
                return categories.save(c);
              });
      var p = new Product();
      p.setCode(row.code());
      p.setName(row.name());
      p.setBrand(row.brand());
      p.setPrice(row.samplePrice());
      p.setDescription(ReferenceCatalog.description(row, data));
      p.setImageUrl(data.imageUrl());
      p.setHoverImageUrl(data.imageUrl());
      p.setCategory(category);
      p.setFeatured(row.featured());
      for (var color : data.colors())
        for (var size : data.sizes()) {
          var v = new ProductVariant();
          v.setProduct(p);
          v.setSku(ReferenceCatalog.sku(row, color, size));
          v.setColor(color.name());
          v.setSize(size);
          v.setPrice(row.samplePrice());
          v.setStock(ReferenceCatalog.stock(color, size));
          p.getVariants().add(v);
        }
      products.save(p);
    }
    products.flush();
  }
}
