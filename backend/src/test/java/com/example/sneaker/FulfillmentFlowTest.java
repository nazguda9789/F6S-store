package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import com.example.sneaker.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:fulfillment;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-password-long-enough",
      "app.seed-demo=false",
      "app.vnpay.enabled=true",
      "app.vnpay.merchant=TESTSHOP",
      "app.vnpay.secret=test-fulfillment-secret-only-2026"
    })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class FulfillmentFlowTest {
  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @Autowired CatalogService catalog;
  @Autowired CheckoutService checkout;
  @Autowired OrderManagementService management;
  @Autowired OrderRepository orders;
  @Autowired VariantRepository variants;
  @Autowired ShopAccountRepository accounts;
  @Autowired PaymentRepository payments;
  @Autowired VnpayService vnpay;
  @Autowired RefundService refunds;
  ProductView product;
  ShopAccount account;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    var c = catalog.createCategory(new CategoryInput("Test " + UUID.randomUUID()));
    product =
        catalog.create(
            new ProductInput(
                "Giày giao hàng",
                "Test",
                BigDecimal.valueOf(100000),
                "Mô tả",
                "/images/catalog-photo-pending.svg",
                "",
                c.id(),
                false,
                List.of(new VariantInput("Đen", "40", 10))));
    account = new ShopAccount();
    account.setId(UUID.randomUUID().toString());
    account.setEmail(account.getId() + "@example.test");
    accounts.save(account);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  CustomerOrder order(String method) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                account.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    var key = UUID.randomUUID();
    try {
      checkout.checkout(
          new Checkout(
              key,
              "Khách",
              account.getEmail(),
              "0912345678",
              "123 Đường mẫu Hà Nội",
              "",
              List.of(new CheckoutLine(product.variants().get(0).id(), 1)),
              null,
              method));
    } finally {
      SecurityContextHolder.clearContext();
    }
    return orders.findByRequestKey(key.toString()).orElseThrow();
  }

  int stock() {
    return variants.findById(product.variants().get(0).id()).orElseThrow().getStock();
  }

  void shipped(CustomerOrder o) {
    management.status(o.getId(), "CONFIRMED");
    management.status(o.getId(), "PACKING");
    management.shipment(o.getId(), "Giao demo", "DEMO-123", "admin");
    management.status(o.getId(), "SHIPPING");
  }

  void failAndReturn(CustomerOrder o) {
    management.status(
        o.getId(), "DELIVERY_FAILED", "admin", "Khách từ chối nhận", null, false, false);
    management.status(o.getId(), "RETURNING", "admin", "Bàn giao hoàn về kho", null, false, false);
  }

  void expire(CustomerOrder o) {
    var p = payments.findByOrderId(o.getId()).orElseThrow();
    p.setExpiresAt(Instant.now().minusSeconds(1000));
    payments.save(p);
  }

  Map<String, String> ipn(CustomerOrder o) {
    var p = payments.findByOrderId(o.getId()).orElseThrow();
    var f = new TreeMap<String, String>();
    f.put("vnp_TmnCode", "TESTSHOP");
    f.put("vnp_TxnRef", p.getId());
    f.put("vnp_Amount", "10000000");
    f.put("vnp_ResponseCode", "00");
    f.put("vnp_TransactionStatus", "00");
    f.put("vnp_TransactionNo", "12345678");
    f.put(
        "vnp_SecureHash",
        VnpayService.hmac("test-fulfillment-secret-only-2026", VnpayService.canonical(f)));
    return f;
  }

  @Test
  void customerRequestDoesNotCancelOrReleaseUntilApproved() {
    var o = order("COD");
    var request =
        management.requestCancellation(o.getCode(), account.getId(), "Khách muốn đổi đơn");
    assertThat(request.cancellationStatus()).isEqualTo("REQUESTED");
    assertThat(request.fulfillmentStatus()).isEqualTo("PENDING");
    assertThat(stock()).isEqualTo(9);
    assertThatThrownBy(() -> management.status(o.getId(), "CONFIRMED"))
        .isInstanceOf(ResponseStatusException.class);
    var done =
        management.decideCancellation(o.getId(), true, "Đồng ý theo yêu cầu", "admin", false);
    management.decideCancellation(o.getId(), true, "Đồng ý theo yêu cầu", "admin", false);
    assertThat(done.status()).isEqualTo("CANCELLED");
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void rejectionAllowsProcessingAndAnotherCustomersRequestIsDenied() {
    var o = order("COD");
    assertThatThrownBy(
            () ->
                management.requestCancellation(
                    o.getCode(), UUID.randomUUID().toString(), "Không phải chủ đơn"))
        .isInstanceOf(ResponseStatusException.class);
    management.requestCancellation(o.getCode(), account.getId(), "Khách đề nghị hủy đơn");
    management.decideCancellation(
        o.getId(), false, "Khách đã xác nhận tiếp tục mua", "admin", false);
    assertThat(management.status(o.getId(), "CONFIRMED").status()).isEqualTo("CONFIRMED");
    assertThat(stock()).isEqualTo(9);
  }

  @Test
  void waybillMustBeCancelledBeforePreShipmentCancellation() {
    var o = order("COD");
    management.status(o.getId(), "CONFIRMED");
    management.shipment(o.getId(), "Giao demo", "DEMO-001", "admin");
    management.requestCancellation(o.getCode(), account.getId(), "Khách chưa muốn nhận");
    assertThatThrownBy(
            () ->
                management.decideCancellation(
                    o.getId(), true, "Đồng ý hủy theo khách", "admin", false))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(stock()).isEqualTo(9);
    management.decideCancellation(o.getId(), true, "Đã hủy vận đơn ngoài", "admin", true);
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void failedAndReturningDoNotReleaseStockAndReceiptRequiresInspection() {
    var o = order("COD");
    shipped(o);
    assertThatThrownBy(
            () ->
                management.requestCancellation(
                    o.getCode(), account.getId(), "Khách yêu cầu hủy muộn"))
        .isInstanceOf(ResponseStatusException.class);
    failAndReturn(o);
    assertThat(stock()).isEqualTo(9);
    assertThatThrownBy(
            () ->
                management.status(
                    o.getId(), "RETURNED", "admin", "Đã nhận toàn bộ hàng", null, false, false))
        .isInstanceOf(ResponseStatusException.class);
    var done =
        management.status(
            o.getId(), "RETURNED", "admin", "Đã nhận đủ và kiểm tra", null, true, false);
    management.status(o.getId(), "RETURNED", "admin", "Đã nhận đủ và kiểm tra", null, true, false);
    assertThat(done.stockReleased()).isTrue();
    assertThat(done.paymentStatus()).isEqualTo("UNPAID");
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void deliveryRetryKeepsStockReserved() {
    var o = order("COD");
    shipped(o);
    management.status(
        o.getId(), "DELIVERY_FAILED", "admin", "Khách hẹn giao lại", null, false, false);
    assertThat(
            management.list(0, "DELIVERY_FAILED", "ONLINE", o.getCode(), "UNPAID", null).content())
        .hasSize(1);
    management.status(o.getId(), "SHIPPING", "admin", "Giao lại theo lịch", null, false, false);
    management.status(o.getId(), "COMPLETED");
    assertThat(stock()).isEqualTo(9);
  }

  @Test
  void vnpayCanPrepareButCannotShipBeforePayment() {
    var o = order("VNPAY");
    management.status(o.getId(), "CONFIRMED");
    management.status(o.getId(), "PACKING");
    management.shipment(o.getId(), "Giao demo", "DEMO-001", "admin");
    assertThatThrownBy(() -> management.status(o.getId(), "SHIPPING"))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(vnpay.ipn(ipn(o)).get("RspCode")).isEqualTo("00");
    assertThat(management.status(o.getId(), "SHIPPING").paymentStatus()).isEqualTo("PAID");
  }

  @Test
  void expiryRequiresGraceAndReconciliationProofAndLatePaymentDoesNotResurrectOrder() {
    var o = order("VNPAY");
    assertThatThrownBy(
            () -> management.expireAfterReconciliation(o.getId(), "PORTAL-REF-1", "admin", false))
        .isInstanceOf(ResponseStatusException.class);
    expire(o);
    assertThat(management.overdue(0).content())
        .extracting(OrderManagementService.Entry::id)
        .contains(o.getId());
    assertThatThrownBy(() -> management.expireAfterReconciliation(o.getId(), "", "admin", false))
        .isInstanceOf(ResponseStatusException.class);
    management.expireAfterReconciliation(o.getId(), "PORTAL-REF-1", "admin", false);
    management.expireAfterReconciliation(o.getId(), "PORTAL-REF-1", "admin", false);
    assertThat(stock()).isEqualTo(10);
    assertThat(vnpay.ipn(ipn(o)).get("RspCode")).isEqualTo("00");
    var saved = orders.findById(o.getId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo("CANCELLED");
    assertThat(saved.getPaymentStatus()).isEqualTo("PAID");
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void paidOrderCannotBeExpiredAndReturnedPaidOrderRefundDoesNotRestockTwice() {
    var o = order("VNPAY");
    expire(o);
    vnpay.ipn(ipn(o));
    assertThatThrownBy(
            () -> management.expireAfterReconciliation(o.getId(), "PORTAL-REF-2", "admin", false))
        .isInstanceOf(ResponseStatusException.class);
    shipped(o);
    failAndReturn(o);
    management.status(
        o.getId(), "RETURNED", "admin", "Nhận đủ và kiểm tra hàng", null, true, false);
    refunds.request(o.getId(), "Khách trả lại toàn bộ", "admin");
    refunds.resolve(o.getId(), true, false, "REFUND-123", "admin");
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void concurrentReceiptOnlyReleasesOnce() throws Exception {
    var o = order("COD");
    shipped(o);
    failAndReturn(o);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var tasks =
          executor.invokeAll(
              List.of(
                  () ->
                      management.status(
                          o.getId(),
                          "RETURNED",
                          "admin",
                          "Đã nhận đủ và kiểm tra",
                          null,
                          true,
                          false),
                  () ->
                      management.status(
                          o.getId(),
                          "RETURNED",
                          "admin",
                          "Đã nhận đủ và kiểm tra",
                          null,
                          true,
                          false)));
      for (var result : tasks) result.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }
    assertThat(stock()).isEqualTo(10);
  }

  @Test
  void ipnAndExpiryRaceCannotLosePaymentOrDoubleRelease() throws Exception {
    var o = order("VNPAY");
    expire(o);
    var fields = ipn(o);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var tasks =
          executor.invokeAll(
              List.<Callable<Object>>of(
                  () -> vnpay.ipn(fields),
                  () -> {
                    try {
                      return management.expireAfterReconciliation(
                          o.getId(), "PORTAL-RACE-1", "admin", false);
                    } catch (ResponseStatusException e) {
                      return e.getStatusCode();
                    }
                  }));
      for (var result : tasks) result.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }
    var saved = orders.findById(o.getId()).orElseThrow();
    assertThat(saved.getPaymentStatus()).isEqualTo("PAID");
    assertThat(stock()).isEqualTo(saved.getStatus().equals("CANCELLED") ? 10 : 9);
  }

  @Test
  void reconciliationEndpointRequiresExplicitUnpaidAttestation() throws Exception {
    var o = order("VNPAY");
    expire(o);
    String header =
        "Basic "
            + Base64.getEncoder()
                .encodeToString(
                    "admin:test-password-long-enough"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/admin/orders/" + o.getId() + "/reconcile-expiry")
                .header("Authorization", header)
                .contentType("application/json")
                .content("{\"reference\":\"PORTAL-CONTRACT-1\",\"verifiedUnpaid\":false}"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isBadRequest());
    assertThat(stock()).isEqualTo(9);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/admin/orders/" + o.getId() + "/reconcile-expiry")
                .header("Authorization", header)
                .contentType("application/json")
                .content("{\"reference\":\"PORTAL-CONTRACT-1\",\"verifiedUnpaid\":true}"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    assertThat(stock()).isEqualTo(10);
  }
}
