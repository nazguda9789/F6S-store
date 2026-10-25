package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VnpayService {
  private final PaymentRepository payments;
  private final OrderRepository orders;
  private final OrderAuditService audit;
  private final boolean enabled;
  private final String merchant, secret, payUrl, returnUrl;

  public VnpayService(
      PaymentRepository payments,
      OrderRepository orders,
      OrderAuditService audit,
      @Value("${app.vnpay.enabled:false}") boolean enabled,
      @Value("${app.vnpay.merchant:}") String merchant,
      @Value("${app.vnpay.secret:}") String secret,
      @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
          String payUrl,
      @Value("${app.vnpay.return-url:http://localhost:5173/payment/return}") String returnUrl) {
    this.payments = payments;
    this.orders = orders;
    this.audit = audit;
    this.enabled = enabled;
    this.merchant = merchant;
    this.secret = secret;
    this.payUrl = payUrl;
    this.returnUrl = returnUrl;
    if (enabled) {
      var uri = URI.create(payUrl);
      if (merchant.isBlank()
          || secret.length() < 16
          || !"https".equals(uri.getScheme())
          || !Set.of("sandbox.vnpayment.vn", "pay.vnpay.vn").contains(uri.getHost()))
        throw new IllegalArgumentException("Cấu hình VNPAY không hợp lệ");
      var back = URI.create(returnUrl);
      if (back.getHost() == null || !Set.of("http", "https").contains(back.getScheme()))
        throw new IllegalArgumentException("VNPAY_RETURN_URL không hợp lệ");
    }
  }

  public boolean enabled() {
    return enabled;
  }

  @Transactional
  public PaymentAttempt prepare(CustomerOrder order) {
    if (order.getTotal() == null || order.getTotal().signum() <= 0)
      throw bad("VNPAY yêu cầu số tiền lớn hơn 0; đơn miễn phí hãy chọn COD");
    if (!enabled)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "VNPAY chưa được cấu hình");
    var old = payments.findByOrderId(order.getId());
    if (old.isPresent()) return old.get();
    var p = new PaymentAttempt();
    p.setId(UUID.randomUUID().toString());
    p.setOrder(order);
    p.setAmount(order.getTotal());
    p.setExpiresAt(p.getCreatedAt().plusSeconds(900));
    return payments.saveAndFlush(p);
  }

  @Transactional(readOnly = true)
  public String url(CustomerOrder order, String ip) {
    if (!enabled
        || !"VNPAY".equals(order.getPaymentMethod())
        || !"PENDING".equals(order.getStatus())) return null;
    var p = payments.findByOrderId(order.getId()).orElse(null);
    if (p == null || !p.getStatus().equals("PENDING") || !p.getExpiresAt().isAfter(Instant.now()))
      return null;
    var fields = new TreeMap<String, String>();
    fields.put("vnp_Version", "2.1.0");
    fields.put("vnp_Command", "pay");
    fields.put("vnp_TmnCode", merchant);
    fields.put(
        "vnp_Amount",
        p.getAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
    fields.put("vnp_CurrCode", "VND");
    fields.put("vnp_TxnRef", p.getId());
    fields.put("vnp_OrderInfo", "Thanh toan " + order.getCode());
    fields.put("vnp_OrderType", "other");
    fields.put("vnp_Locale", "vn");
    fields.put("vnp_ReturnUrl", returnUrl);
    fields.put("vnp_IpAddr", ip == null ? "127.0.0.1" : ip);
    var format =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    fields.put("vnp_CreateDate", format.format(p.getCreatedAt()));
    fields.put("vnp_ExpireDate", format.format(p.getExpiresAt()));
    String query = canonical(fields);
    return payUrl + "?" + query + "&vnp_SecureHash=" + hmac(secret, query);
  }

  public static String canonical(Map<String, String> fields) {
    var pairs = new ArrayList<String>();
    new TreeMap<>(fields)
        .forEach(
            (k, v) -> {
              if (v != null
                  && !v.isBlank()
                  && !k.equals("vnp_SecureHash")
                  && !k.equals("vnp_SecureHashType"))
                pairs.add(
                    URLEncoder.encode(k, StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(v, StandardCharsets.UTF_8));
            });
    return String.join("&", pairs);
  }

  public static String hmac(String key, String data) {
    try {
      var mac = Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
      return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean valid(Map<String, String> fields) {
    String signature = fields.get("vnp_SecureHash");
    return enabled
        && signature != null
        && signature.matches("[a-fA-F0-9]{128}")
        && MessageDigest.isEqual(
            signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
            hmac(secret, canonical(fields)).getBytes(StandardCharsets.US_ASCII));
  }

  private Map<String, String> ack(String code, String message) {
    return Map.of("RspCode", code, "Message", message);
  }

  @Transactional
  public Map<String, String> ipn(Map<String, String> fields) {
    if (!valid(fields)) return ack("97", "Invalid signature");
    if (!merchant.equals(fields.get("vnp_TmnCode"))) return ack("97", "Invalid merchant");
    var found = payments.findById(fields.getOrDefault("vnp_TxnRef", "")).orElse(null);
    if (found == null) return ack("01", "Order not found");
    var order = orders.lockId(found.getOrder().getId()).orElseThrow();
    var p = payments.lockId(found.getId()).orElseThrow();
    if (!p.getAmount()
            .multiply(BigDecimal.valueOf(100))
            .toBigIntegerExact()
            .toString()
            .equals(fields.get("vnp_Amount"))
        || !"VND".equals(fields.getOrDefault("vnp_CurrCode", "VND")))
      return ack("04", "Invalid amount");
    if (!p.getStatus().equals("PENDING")) return ack("02", "Already confirmed");
    boolean success =
        "00".equals(fields.get("vnp_ResponseCode"))
            && "00".equals(fields.get("vnp_TransactionStatus"));
    String transaction = fields.get("vnp_TransactionNo");
    if (success && (transaction == null || !transaction.matches("[0-9]{1,40}")))
      return ack("99", "Invalid transaction");
    p.setStatus(success ? "PAID" : "FAILED");
    p.setSettledAt(Instant.now());
    p.setProviderTransaction(
        transaction != null && transaction.length() <= 40 ? transaction : null);
    order.setPaymentStatus(success ? "PAID" : "FAILED");
    audit.record(
        order,
        order.getStatus(),
        "VNPAY",
        success
            ? (order.getStatus().equals("CANCELLED")
                ? "VNPAY đã thu tiền sau khi hủy: cần hoàn tiền, không phục hồi đơn"
                : "VNPAY xác nhận đã thanh toán")
            : "VNPAY báo giao dịch không thành công");
    return ack("00", "Confirm success");
  }

  @Transactional(readOnly = true)
  public Map<String, String> result(Map<String, String> fields) {
    if (!valid(fields) || !merchant.equals(fields.get("vnp_TmnCode")))
      throw bad("Chữ ký thanh toán không hợp lệ");
    var p =
        payments
            .findById(fields.getOrDefault("vnp_TxnRef", ""))
            .orElseThrow(() -> missing("Không tìm thấy giao dịch"));
    return Map.of(
        "code",
        p.getOrder().getCode(),
        "paymentStatus",
        p.getStatus(),
        "status",
        p.getOrder().getStatus());
  }
}
