package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;

import com.example.sneaker.dto.*;
import com.example.sneaker.dto.Api.*;
import com.example.sneaker.dto.PosApi.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import com.example.sneaker.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:f6s_advanced;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-password-long-enough",
      "app.seed-demo=false"
    })
class AdvancedWorkflowTest {
  @Autowired VariantMatrixService matrix;
  @Autowired CatalogService catalog;
  @Autowired PosService pos;
  @Autowired PosOperatorRepository operators;
  @Autowired VariantRepository variants;
  @Autowired PosDraftRepository drafts;
  @Autowired OrderRepository orders;
  @Autowired PromotionService promotions;
  @Autowired PromotionRepository promoRepo;
  @Autowired CheckoutService checkout;
  @Autowired OrderManagementService management;
  @Autowired DashboardService dashboard;
  ProductView product;
  String owner;

  @BeforeEach
  void setup() {
    owner = "staff-" + UUID.randomUUID();
    var op = new PosOperator();
    op.setUsername(owner);
    operators.save(op);
    var c = catalog.createCategory(new CategoryInput("Nhóm " + UUID.randomUUID()));
    product =
        matrix.create(
            new MatrixApi.Create(
                "Giày matrix",
                "F6S",
                "Mô tả sản phẩm",
                "/images/shoe-0.svg",
                "",
                c.id(),
                false,
                new MatrixApi.Selection(
                    List.of("Đen", "Trắng"),
                    List.of("40", "41"),
                    10,
                    new BigDecimal("100001"),
                    List.of(new MatrixApi.Override("Trắng", "41", 0, new BigDecimal("120000"))))));
  }

  Draft draft() {
    return pos.create(owner, new NewDraft(UUID.randomUUID()));
  }

  Scan scan(String sku, int quantity) {
    return new Scan(UUID.randomUUID(), sku, quantity);
  }

  PromotionApi.View promo(String type, int value, int limit, List<Long> ids) {
    return promotions.create(
        new PromotionApi.Input(
            "TEST_" + UUID.randomUUID().toString().substring(0, 8),
            "Khuyến mãi",
            type,
            BigDecimal.valueOf(value),
            BigDecimal.ZERO,
            null,
            limit,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(3600),
            true,
            ids));
  }

  @Test
  void matrixCreatesFullCrossProductAndIndividualPrice() {
    assertThat(product.variants()).hasSize(4);
    assertThat(product.variants().stream().map(VariantView::sku).distinct()).hasSize(4);
    var zero =
        product.variants().stream()
            .filter(v -> v.color().equals("Trắng") && v.size().equals("41"))
            .findFirst()
            .orElseThrow();
    assertThat(zero.stock()).isZero();
    assertThat(zero.price()).isEqualByComparingTo("120000");
    assertThat(PosService.parseSku(zero.qrPayload())).isEqualTo(zero.sku());
  }

  @Test
  void matrixRejectsDuplicatesAndUnknownOverride() {
    assertThatThrownBy(
            () ->
                matrix.preview(
                    new MatrixApi.Selection(
                        List.of("Đen", " đen "), List.of("40"), 1, BigDecimal.TEN, List.of())))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                matrix.preview(
                    new MatrixApi.Selection(
                        List.of("Đen"),
                        List.of("40"),
                        1,
                        BigDecimal.TEN,
                        List.of(new MatrixApi.Override("Trắng", "40", 1, BigDecimal.TEN)))))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void matrixRejectsOverOneHundredCombinations() {
    var colors = java.util.stream.IntStream.range(0, 11).mapToObj(i -> "Màu " + i).toList();
    var sizes = java.util.stream.IntStream.range(35, 45).mapToObj(String::valueOf).toList();
    assertThatThrownBy(
            () ->
                matrix.preview(
                    new MatrixApi.Selection(colors, sizes, 1, BigDecimal.TEN, List.of())))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void maxFiveDraftsAndCreateReplay() {
    var request = new NewDraft(UUID.randomUUID());
    var first = pos.create(owner, request);
    assertThat(pos.create(owner, request).id()).isEqualTo(first.id());
    for (int i = 0; i < 4; i++) draft();
    assertThatThrownBy(this::draft).isInstanceOf(ResponseStatusException.class);
    assertThat(pos.list(owner)).hasSize(5);
    pos.cancel(first.id(), owner);
    draft();
    assertThat(pos.list(owner)).hasSize(5);
  }

  @Test
  void concurrentDraftLimitIsEnforced() throws Exception {
    var executor = Executors.newFixedThreadPool(6);
    var start = new CountDownLatch(1);
    var jobs = new ArrayList<Future<Boolean>>();
    try {
      for (int i = 0; i < 6; i++)
        jobs.add(
            executor.submit(
                () -> {
                  start.await();
                  try {
                    draft();
                    return true;
                  } catch (ResponseStatusException e) {
                    return false;
                  }
                }));
      start.countDown();
      int successes = 0;
      for (var job : jobs) if (job.get(20, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(5);
      assertThat(pos.list(owner)).hasSize(5);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void scanQrSkuAndRetryDoNotDoubleIncrement() {
    var d = draft();
    var v = product.variants().get(0);
    var scan = scan(v.qrPayload(), 1);
    assertThat(pos.scan(d.id(), owner, scan).lines().get(0).quantity()).isEqualTo(1);
    assertThat(pos.scan(d.id(), owner, scan).lines().get(0).quantity()).isEqualTo(1);
    assertThat(pos.scan(d.id(), owner, scan(v.sku(), 1)).lines().get(0).quantity()).isEqualTo(2);
    assertThatThrownBy(() -> pos.scan(d.id(), owner, new Scan(scan.eventKey(), v.sku(), 2)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void scanRejectsExternalUrlsSoldOutAndOtherOwner() {
    var d = draft();
    assertThatThrownBy(() -> pos.scan(d.id(), owner, scan("https://example.com", 1)))
        .isInstanceOf(ResponseStatusException.class);
    var zero = product.variants().stream().filter(v -> v.stock() == 0).findFirst().orElseThrow();
    assertThatThrownBy(() -> pos.scan(d.id(), owner, scan(zero.sku(), 1)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () -> pos.scan(d.id(), "other-staff", scan(product.variants().get(0).sku(), 1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void cashFailureRollsBackStockAndVoucherAndDraft() {
    var d = draft();
    var v = product.variants().get(0);
    pos.scan(d.id(), owner, scan(v.sku(), 2));
    var promo = promo("PERCENT", 10, 1, List.of(product.id()));
    assertThatThrownBy(() -> pos.pay(d.id(), owner, new Payment(BigDecimal.ONE, promo.code())))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(variants.findById(v.id()).orElseThrow().getStock()).isEqualTo(10);
    assertThat(promoRepo.findById(promo.id()).orElseThrow().getUsedCount()).isZero();
    assertThat(pos.list(owner)).hasSize(1);
    var payment = new Payment(new BigDecimal("200000"), promo.code());
    var invoice = pos.pay(d.id(), owner, payment);
    assertThat(invoice.discount()).isEqualByComparingTo("20000");
    assertThat(invoice.total()).isEqualByComparingTo("180002");
    assertThat(invoice.cashChange()).isEqualByComparingTo("19998");
    assertThat(pos.pay(d.id(), owner, payment).code()).isEqualTo(invoice.code());
    assertThat(variants.findById(v.id()).orElseThrow().getStock()).isEqualTo(8);
    assertThat(promoRepo.findById(promo.id()).orElseThrow().getUsedCount()).isEqualTo(1);
    assertThat(pos.list(owner)).isEmpty();
  }

  @Test
  void simultaneousPosCheckoutConsumesOnlyOnce() throws Exception {
    var d = draft();
    var v = product.variants().get(0);
    pos.scan(d.id(), owner, scan(v.sku(), 1));
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    Callable<String> pay =
        () -> {
          start.await();
          return pos.pay(d.id(), owner, new Payment(new BigDecimal("200000"), null)).code();
        };
    try {
      var a = executor.submit(pay);
      var b = executor.submit(pay);
      start.countDown();
      assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(b.get(20, TimeUnit.SECONDS));
      assertThat(variants.findById(v.id()).orElseThrow().getStock()).isEqualTo(9);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void promotionsApplyOnlySelectedProductsAndNeverNegative() {
    var other =
        catalog.create(
            new ProductInput(
                "Other",
                "F6S",
                new BigDecimal("50000"),
                "Description",
                "/images/shoe-0.svg",
                "",
                product.category().id(),
                false,
                List.of(new VariantInput("Đen", "40", 5))));
    var d = draft();
    pos.scan(d.id(), owner, scan(product.variants().get(0).sku(), 1));
    pos.scan(d.id(), owner, scan(other.variants().get(0).sku(), 1));
    var promo = promo("FIXED", 999999, 2, List.of(product.id()));
    var q = pos.quote(d.id(), owner, promo.code());
    assertThat(q.subtotal()).isEqualByComparingTo("150001");
    assertThat(q.discount()).isEqualByComparingTo("100001");
    assertThat(q.total()).isEqualByComparingTo("50000");
  }

  @Test
  void onlineCancellationRestoresOnceAndReleasesPromotion() {
    var p = promo("PERCENT", 10, 1, List.of(product.id()));
    var v = product.variants().get(0);
    UUID key = UUID.randomUUID();
    checkout.checkout(
        new Checkout(
            key,
            "Test",
            "test@example.com",
            "0912345678",
            "123 Nguyễn Trãi, Hà Nội",
            "",
            List.of(new CheckoutLine(v.id(), 1)),
            p.code()));
    var order = orders.findByRequestKey(key.toString()).orElseThrow();
    management.status(order.getId(), "CANCELLED");
    management.status(order.getId(), "CANCELLED");
    assertThat(variants.findById(v.id()).orElseThrow().getStock()).isEqualTo(10);
    assertThat(promoRepo.findById(p.id()).orElseThrow().getUsedCount()).isZero();
    assertThatThrownBy(() -> management.status(order.getId(), "COMPLETED"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void deliveryTimelineRejectsSkippedAndStaleTransitions() {
    var v = product.variants().get(0);
    UUID key = UUID.randomUUID();
    checkout.checkout(
        new Checkout(
            key,
            "Test",
            "test@example.com",
            "0912345678",
            "123 Nguyễn Trãi, Hà Nội",
            "",
            List.of(new CheckoutLine(v.id(), 1)),
            null));
    var id = orders.findByRequestKey(key.toString()).orElseThrow().getId();
    assertThatThrownBy(() -> management.status(id, "COMPLETED"))
        .isInstanceOf(ResponseStatusException.class);
    management.status(id, "CONFIRMED", "admin", "Đã xác minh", "PENDING");
    assertThatThrownBy(() -> management.status(id, "PACKING", "admin", "", "PENDING"))
        .isInstanceOf(ResponseStatusException.class);
    management.status(id, "PACKING", "admin", "Đóng gói", "CONFIRMED");
    management.shipment(id, "Demo", "DEMO-123", "admin");
    management.status(id, "SHIPPING", "admin", "Bàn giao", "PACKING");
    assertThatThrownBy(() -> management.status(id, "CANCELLED"))
        .isInstanceOf(ResponseStatusException.class);
    management.status(id, "COMPLETED", "admin", "Đã giao", "SHIPPING");
    management.status(id, "COMPLETED");
    assertThat(management.detail(id).timeline()).hasSize(6);
    assertThat(variants.findById(v.id()).orElseThrow().getStock()).isEqualTo(9);
  }

  @Test
  void dashboardUsesCompletionTimeVietnamZoneAndIgnoresPending() {
    var d = draft();
    pos.scan(d.id(), owner, scan(product.variants().get(0).sku(), 1));
    var invoice = pos.pay(d.id(), owner, new Payment(new BigDecimal("200000"), null));
    var order = orders.findByRequestKey("POS-" + d.id()).orElseThrow();
    order.setCreatedAt(Instant.parse("2040-02-01T18:00:00Z"));
    order.setCompletedAt(Instant.parse("2040-02-01T18:00:00Z"));
    orders.save(order);
    var report =
        dashboard.report(LocalDate.parse("2040-02-02"), LocalDate.parse("2040-02-02"), "day");
    assertThat(report.revenue()).isEqualByComparingTo(invoice.total());
    assertThat(report.totalOrders()).isEqualTo(1);
    assertThat(report.successRate()).isEqualByComparingTo("100");
    assertThat(report.topProducts().get(0).quantity()).isEqualTo(1);
    assertThat(
            dashboard
                .report(LocalDate.parse("2040-02-01"), LocalDate.parse("2040-02-01"), "day")
                .revenue())
        .isEqualByComparingTo("0");
  }

  @Test
  void changedPriceRequiresNewTotalConfirmationAndRollsBack() {
    var d = draft();
    var variant = product.variants().get(0);
    pos.scan(d.id(), owner, scan(variant.sku(), 1));
    assertThatThrownBy(
            () ->
                pos.pay(d.id(), owner, new Payment(new BigDecimal("200000"), null, BigDecimal.ONE)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Giá hoặc khuyến mãi");
    assertThat(variants.findById(variant.id()).orElseThrow().getStock()).isEqualTo(10);
    assertThat(pos.list(owner)).hasSize(1);
    assertThat(
            pos.pay(
                    d.id(),
                    owner,
                    new Payment(new BigDecimal("200000"), null, new BigDecimal("100001")))
                .total())
        .isEqualByComparingTo("100001");
  }
}
