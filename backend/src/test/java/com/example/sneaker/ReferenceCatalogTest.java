package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;

import com.example.sneaker.config.*;
import com.example.sneaker.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:reference_catalog;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-reference-password-long",
      "app.seed-demo=false"
    })
@Transactional
class ReferenceCatalogTest {
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired VariantRepository variants;
  @Autowired ReferenceCatalog catalog;

  @Test
  void bootstrapHasRealModelNamesStableSkusAndClearlyMarkedDemoInventory() {
    var seeder = new DemoData(categories, products, catalog);
    seeder.run();
    assertThat(categories.count()).isEqualTo(5);
    assertThat(products.count()).isEqualTo(25);
    assertThat(variants.count()).isEqualTo(250);
    assertThat(products.findAll())
        .extracting(p -> p.getName())
        .contains("Nike Air Force 1 '07", "adidas Samba OG", "HOKA Clifton 9", "Converse Chuck 70")
        .doesNotContain("Velocity Run 01");
    assertThat(products.findAll())
        .allSatisfy(
            p -> {
              assertThat(p.getCode()).startsWith("F6S-RC-");
              assertThat(p.getDescription()).contains("cấu hình demo", "không phải mã phối màu");
              assertThat(p.getImageUrl()).isEqualTo("/images/catalog-photo-pending.svg");
            });
    assertThat(variants.findAll())
        .extracting(v -> v.getSku())
        .contains("SKU-RC001-BLK-38")
        .doesNotHaveDuplicates();
    assertThat(variants.findAll().stream().mapToInt(v -> v.getStock()).sum()).isEqualTo(2250);
    var first = variants.findBySku("SKU-RC001-BLK-38").orElseThrow();
    first.setStock(3);
    variants.saveAndFlush(first);
    seeder.run();
    assertThat(variants.count()).isEqualTo(250);
    assertThat(variants.findBySku(first.getSku()).orElseThrow().getStock()).isEqualTo(3);
  }

  @Test
  void existingCategoryStopsAutomaticBootstrapWithoutOverwriting() {
    var category = new com.example.sneaker.model.Category();
    category.setName("Existing user category");
    categories.save(category);
    new DemoData(categories, products, catalog).run();
    assertThat(products.count()).isZero();
    assertThat(categories.count()).isEqualTo(1);
  }
}
