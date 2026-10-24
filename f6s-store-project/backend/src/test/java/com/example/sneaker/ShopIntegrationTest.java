package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.repository.*;
import com.example.sneaker.service.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:f6s;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-password-long-enough",
      "app.seed-demo=false"
    })
@AutoConfigureMockMvc
class ShopIntegrationTest {
  @Autowired CatalogService catalog;
  @Autowired CheckoutService checkout;
  @Autowired OrderRepository orders;
  @Autowired VariantRepository variants;
  @Autowired MockMvc mvc;
  ProductView product;

  @BeforeEach
  void prepare() {
    var c = catalog.createCategory(new CategoryInput("Chạy bộ " + UUID.randomUUID()));
    product = catalog.create(input(c.id(), 2));
  }

  ProductInput input(Long category, int stock) {
    return new ProductInput(
        "Giày kiểm thử",
        "F6S",
        new BigDecimal("100000"),
        "Mô tả giày thể thao",
        "/images/shoe-0.svg",
        "/images/shoe-1.svg",
        category,
        true,
        List.of(new VariantInput("Đen", "40", stock), new VariantInput("Trắng", "41", 0)));
  }

  Checkout request(UUID key, int quantity) {
    return new Checkout(
        key,
        "Nguyễn Văn An",
        "an@example.com",
        "0912345678",
        "123 Nguyễn Trãi, Hà Nội",
        "",
        List.of(new CheckoutLine(product.variants().get(0).id(), quantity)));
  }

  @Test
  void filtersMatchSameInStockVariant() {
    var c = product.category().id();
    assertThat(
            catalog
                .search(c, null, null, null, "40", "Đen", null, null, "newest", 0, 12)
                .totalElements())
        .isEqualTo(1);
    assertThat(
            catalog
                .search(c, null, null, null, "41", "Đen", null, null, "newest", 0, 12)
                .totalElements())
        .isZero();
    assertThat(
            catalog
                .search(c, null, null, null, "41", "Trắng", null, null, "newest", 0, 12)
                .totalElements())
        .isZero();
  }

  @Test
  void checkoutUsesServerPriceAndReplaysOnce() {
    var request = request(UUID.randomUUID(), 2);
    var first = checkout.checkout(request);
    var second = checkout.checkout(request);
    assertThat(first.total()).isEqualByComparingTo("200000");
    assertThat(second.code()).isEqualTo(first.code());
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock()).isZero();
    assertThatThrownBy(() -> checkout.checkout(request(UUID.randomUUID(), 1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rollbackOnInsufficientStock() {
    long count = orders.count();
    assertThatThrownBy(() -> checkout.checkout(request(UUID.randomUUID(), 3)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(orders.count()).isEqualTo(count);
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
        .isEqualTo(2);
  }

  @Test
  void duplicateLinesAggregateAndChangedReplayRejected() {
    var id = product.variants().get(0).id();
    var key = UUID.randomUUID();
    var request =
        new Checkout(
            key,
            "Nguyễn Văn An",
            "an@example.com",
            "0912345678",
            "123 Nguyễn Trãi, Hà Nội",
            "",
            List.of(new CheckoutLine(id, 1), new CheckoutLine(id, 1)));
    assertThat(checkout.checkout(request).items()).hasSize(1);
    assertThatThrownBy(() -> checkout.checkout(request(key, 1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void concurrentCheckoutCannotOversell() throws Exception {
    catalog.update(product.id(), input(product.category().id(), 1));
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    Callable<Boolean> attempt =
        () -> {
          start.await();
          try {
            checkout.checkout(request(UUID.randomUUID(), 1));
            return true;
          } catch (ResponseStatusException e) {
            return false;
          }
        };
    try {
      var a = executor.submit(attempt);
      var b = executor.submit(attempt);
      start.countDown();
      assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
      assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
          .isZero();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void mutationRetainsSkuAndHistory() {
    var order = checkout.checkout(request(UUID.randomUUID(), 1));
    String sku = product.variants().get(0).sku();
    var changed = catalog.update(product.id(), input(product.category().id(), 5));
    assertThat(changed.variants().get(0).sku()).isEqualTo(sku);
    catalog.delete(product.id());
    assertThatThrownBy(() -> catalog.detail(product.id()))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(order.items().get(0).unitPrice()).isEqualByComparingTo("100000");
  }

  @Test
  void rejectsDuplicateVariants() {
    var i = input(product.category().id(), 2);
    var duplicate =
        new ProductInput(
            i.name(),
            i.brand(),
            i.price(),
            i.description(),
            i.imageUrl(),
            i.hoverImageUrl(),
            i.categoryId(),
            false,
            List.of(i.variants().get(0), i.variants().get(0)));
    assertThatThrownBy(() -> catalog.create(duplicate)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsInvalidRanges() {
    assertThatThrownBy(
            () ->
                catalog.search(
                    null,
                    null,
                    new BigDecimal("10"),
                    BigDecimal.ONE,
                    null,
                    null,
                    null,
                    null,
                    "newest",
                    0,
                    12))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void publicReadAdminProtectionAndValidation() throws Exception {
    mvc.perform(get("/api/products")).andExpect(status().isOk());
    mvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
    mvc.perform(delete("/api/admin/products/" + product.id())).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/orders").contentType("application/json").content("{\"items\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void failureOnSecondLineRollsBackFirstLine() {
    long count = orders.count();
    var request =
        new Checkout(
            UUID.randomUUID(),
            "Nguyễn Văn An",
            "an@example.com",
            "0912345678",
            "123 Nguyễn Trãi, Hà Nội",
            "",
            List.of(
                new CheckoutLine(product.variants().get(0).id(), 1),
                new CheckoutLine(product.variants().get(1).id(), 1)));
    assertThatThrownBy(() -> checkout.checkout(request))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(orders.count()).isEqualTo(count);
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
        .isEqualTo(2);
  }

  @Test
  void simultaneousReplayConsumesStockOnlyOnce() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    var request = request(UUID.randomUUID(), 1);
    Callable<String> attempt =
        () -> {
          start.await();
          return checkout.checkout(request).code();
        };
    try {
      var a = executor.submit(attempt);
      var b = executor.submit(attempt);
      start.countDown();
      assertThat(a.get(10, TimeUnit.SECONDS)).isEqualTo(b.get(10, TimeUnit.SECONDS));
      assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void authenticatedAdminCrudAndDecimalPriceValidation() throws Exception {
    String authorization =
        "Basic "
            + Base64.getEncoder()
                .encodeToString(
                    "admin:test-password-long-enough"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    String body = mapper.writeValueAsString(input(product.category().id(), 3));
    String created =
        mvc.perform(
                post("/api/admin/products")
                    .header("Authorization", authorization)
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = mapper.readTree(created).get("id").asLong();
    mvc.perform(
            put("/api/admin/products/" + id)
                .header("Authorization", authorization)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/admin/products")
                .header("Authorization", authorization)
                .contentType("application/json")
                .content(body.replace("100000", "100000.5")))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/admin/products/" + id).header("Authorization", authorization))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
  }
}
