package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import com.example.sneaker.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:integrationflows;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-password-long-enough",
      "app.seed-demo=false",
      "app.shipping-fee=30000",
      "app.shipping-free-above=500000",
      "app.vnpay.enabled=true",
      "app.vnpay.merchant=TESTSHOP",
      "app.vnpay.secret=test-signature-secret-not-real-2026",
      "app.otp.enabled=true",
      "app.otp.secret=test-email-secret-not-real-at-least-32-characters"
    })
@AutoConfigureMockMvc
class IntegrationFlowTest {
  static final String SECRET = "test-signature-secret-not-real-2026";
  @Autowired CheckoutService checkout;
  @Autowired CatalogService catalog;
  @Autowired OrderRepository orders;
  @Autowired VariantRepository variants;
  @Autowired PaymentRepository payments;
  @Autowired VnpayService vnpay;
  @Autowired OrderManagementService management;
  @Autowired RefundService refunds;
  @Autowired EmailOtpService otp;
  @Autowired EmailChallengeRepository challenges;
  @Autowired ShopAuthService auth;
  @Autowired MockMvc mvc;
  @Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;

  @Test
  void customerOnlySeesOrdersPlacedWithTheirSession() throws Exception {
    var sent = otp.request("owner@example.test");
    var code = ArgumentCaptor.forClass(String.class);
    verify(delivery).otp(eq("owner@example.test"), code.capture());
    var first = auth.login(sent.challengeId(), code.getValue());
    mvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer " + first.token())
                .contentType("application/json")
                .content(mapper.writeValueAsString(input("COD", "owner@example.test"))))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + first.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer " + first.token())
                .contentType("application/json")
                .content(mapper.writeValueAsString(input("COD", "someone-else@example.test"))))
        .andExpect(status().isBadRequest());
    var other = otp.request("other-owner@example.test");
    var otherCode = ArgumentCaptor.forClass(String.class);
    verify(delivery).otp(eq("other-owner@example.test"), otherCode.capture());
    var second = auth.login(other.challengeId(), otherCode.getValue());
    mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + second.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @MockBean EmailDelivery delivery;
  ProductView product;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(delivery);
    var c = catalog.createCategory(new CategoryInput("Test " + UUID.randomUUID()));
    product =
        catalog.create(
            new ProductInput(
                "Giày test",
                "Test",
                BigDecimal.valueOf(100000),
                "Mô tả",
                "/images/catalog-photo-pending.svg",
                "",
                c.id(),
                false,
                List.of(new VariantInput("Đen", "40", 10))));
  }

  @Test
  void zeroValueOrderCannotStartVnpayTransaction() {
    var order = new CustomerOrder();
    order.setTotal(BigDecimal.ZERO);
    assertThatThrownBy(() -> vnpay.prepare(order)).isInstanceOf(ResponseStatusException.class);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Checkout input(String method, String email) {
    return new Checkout(
        UUID.randomUUID(),
        "Khách kiểm thử",
        email,
        "0912345678",
        "123 Đường kiểm thử Hà Nội",
        "",
        List.of(new CheckoutLine(product.variants().get(0).id(), 1)),
        null,
        method);
  }

  CustomerOrder order(Checkout input) {
    checkout.checkout(input);
    return orders.findByRequestKey(input.requestKey().toString()).orElseThrow();
  }

  Map<String, String> signed(CustomerOrder order, String amount) {
    var p = payments.findByOrderId(order.getId()).orElseThrow();
    var fields = new TreeMap<String, String>();
    fields.put("vnp_TmnCode", "TESTSHOP");
    fields.put("vnp_TxnRef", p.getId());
    fields.put("vnp_Amount", amount);
    fields.put("vnp_ResponseCode", "00");
    fields.put("vnp_TransactionStatus", "00");
    fields.put("vnp_TransactionNo", "123456789");
    fields.put("vnp_SecureHash", VnpayService.hmac(SECRET, VnpayService.canonical(fields)));
    return fields;
  }

  @Test
  void signedIpnChecksAmountAndDoesNotDoubleSettle() {
    var input = input("VNPAY", "payment@example.test");
    var result = checkout.checkout(input);
    assertThat(result.total()).isEqualByComparingTo("130000");
    assertThat(result.paymentUrl()).startsWith("https://sandbox.vnpayment.vn/");
    var o = orders.findByRequestKey(input.requestKey().toString()).orElseThrow();
    assertThat(vnpay.ipn(signed(o, "100")).get("RspCode")).isEqualTo("04");
    var fields = signed(o, "13000000");
    fields.put("vnp_Amount", "1");
    assertThat(vnpay.ipn(fields).get("RspCode")).isEqualTo("97");
    fields = signed(o, "13000000");
    assertThat(vnpay.result(fields).get("paymentStatus")).isEqualTo("PENDING");
    assertThat(orders.findById(o.getId()).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
    assertThat(vnpay.ipn(fields).get("RspCode")).isEqualTo("00");
    assertThat(vnpay.ipn(fields).get("RspCode")).isEqualTo("02");
    assertThat(orders.findById(o.getId()).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
        .isEqualTo(9);
  }

  @Test
  void cancelledOrderPaidLateRequiresRefundWithoutDoubleRestocking() {
    var o = order(input("VNPAY", "late@example.test"));
    management.status(o.getId(), "CONFIRMED");
    management.status(o.getId(), "PACKING");
    assertThatThrownBy(() -> management.status(o.getId(), "SHIPPING"))
        .isInstanceOf(ResponseStatusException.class);
    management.status(o.getId(), "CANCELLED");
    assertThat(vnpay.ipn(signed(o, "13000000")).get("RspCode")).isEqualTo("00");
    refunds.request(o.getId(), "Thanh toán về sau hủy", "admin");
    var r = refunds.resolve(o.getId(), true, false, "VNPAY-PORTAL-TEST-123", "admin");
    assertThat(r.status()).isEqualTo("REFUNDED");
    assertThat(r.restocked()).isFalse();
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
        .isEqualTo(10);
  }

  @Test
  void deliveryRequiresWaybillAndReturnRestocksExactlyOnce() {
    var o = order(input("COD", "return@example.test"));
    management.status(o.getId(), "CONFIRMED");
    management.status(o.getId(), "PACKING");
    assertThatThrownBy(() -> management.status(o.getId(), "SHIPPING"))
        .isInstanceOf(ResponseStatusException.class);
    management.shipment(o.getId(), "Giao demo", "DEMO-123", "admin");
    management.status(o.getId(), "SHIPPING");
    management.status(o.getId(), "COMPLETED");
    refunds.request(o.getId(), "Khách trả toàn bộ hàng", "admin");
    assertThatThrownBy(() -> refunds.resolve(o.getId(), true, false, "REF-123", "admin"))
        .isInstanceOf(ResponseStatusException.class);
    refunds.resolve(o.getId(), true, true, "REF-123", "admin");
    refunds.resolve(o.getId(), true, true, "REF-123", "admin");
    assertThat(variants.findById(product.variants().get(0).id()).orElseThrow().getStock())
        .isEqualTo(10);
    assertThat(orders.findById(o.getId()).orElseThrow().getRefundedAmount())
        .isEqualByComparingTo("130000");
  }

  @Test
  void otpIsSingleUseAndIncorrectAttemptsArePersisted() {
    var sent = otp.request("attempt@example.test");
    var code = ArgumentCaptor.forClass(String.class);
    verify(delivery).otp(eq("attempt@example.test"), code.capture());
    String wrong = code.getValue().equals("000000") ? "000001" : "000000";
    for (int i = 0; i < 5; i++)
      assertThatThrownBy(() -> auth.login(sent.challengeId(), wrong))
          .isInstanceOf(ResponseStatusException.class);
    assertThat(challenges.findById(sent.challengeId()).orElseThrow().getAttempts()).isEqualTo(5);
    assertThatThrownBy(() -> auth.login(sent.challengeId(), code.getValue()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void customerTokenCannotAccessAdministrationAndRevokesOnLogout() throws Exception {
    var sent = otp.request("buyer@example.test");
    var code = ArgumentCaptor.forClass(String.class);
    verify(delivery).otp(eq("buyer@example.test"), code.capture());
    var login = auth.login(sent.challengeId(), code.getValue());
    String header = "Bearer " + login.token();
    mvc.perform(get("/api/me/profile").header("Authorization", header))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("buyer@example.test"));
    mvc.perform(get("/api/admin/orders").header("Authorization", header))
        .andExpect(status().isForbidden());
    assertThatThrownBy(() -> auth.login(sent.challengeId(), code.getValue()))
        .isInstanceOf(ResponseStatusException.class);
    auth.logout(login.token());
    mvc.perform(get("/api/me/profile").header("Authorization", header))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredOtpCannotAuthenticate() {
    var sent = otp.request("expired@example.test");
    var c = challenges.findById(sent.challengeId()).orElseThrow();
    c.setExpiresAt(Instant.now().minusSeconds(1));
    challenges.save(c);
    assertThatThrownBy(() -> auth.login(sent.challengeId(), "000000"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void disablingOnlineAccountPermanentlyRevokesExistingSessions() throws Exception {
    var sent = otp.request("disabled-online@example.test");
    var code = ArgumentCaptor.forClass(String.class);
    verify(delivery).otp(eq("disabled-online@example.test"), code.capture());
    var login = auth.login(sent.challengeId(), code.getValue());
    String id = auth.authenticate(login.token());
    assertThat(id).isNotNull();
    auth.active(id, false);
    mvc.perform(get("/api/me/profile").header("Authorization", "Bearer " + login.token()))
        .andExpect(status().isUnauthorized());
    auth.active(id, true);
    assertThat(auth.authenticate(login.token())).isNull();
  }
}
