package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderManagementService {
  private final OrderRepository orders;
  private final OrderEventRepository events;
  private final OrderAuditService audit;
  private final InventoryReleaseService release;
  private final PaymentRepository payments;

  @org.springframework.beans.factory.annotation.Value(
      "${app.vnpay.reconciliation-grace-seconds:300}")
  private long reconciliationGrace;

  public record PaymentInfo(
      Instant expiresAt, Instant reconciledAt, String reference, boolean overdue) {}

  public record Event(
      Long id, String fromStatus, String toStatus, String actor, String note, Instant createdAt) {}

  public record Entry(
      Long id,
      String customerName,
      String email,
      String phone,
      String address,
      String note,
      OrderView order,
      List<Event> timeline,
      String carrier,
      String trackingCode,
      Instant shippedAt,
      String deliveryIssue,
      PaymentInfo payment) {}

  private Entry entry(CustomerOrder o) {
    return new Entry(
        o.getId(),
        o.getCustomerName(),
        o.getEmail(),
        o.getPhone(),
        o.getAddress(),
        o.getNote(),
        CheckoutService.view(o),
        events.findByOrderIdOrderByIdAsc(o.getId()).stream()
            .map(
                e ->
                    new Event(
                        e.getId(),
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getActor(),
                        e.getNote(),
                        e.getCreatedAt()))
            .toList(),
        o.getCarrier(),
        o.getTrackingCode(),
        o.getShippedAt(),
        o.getDeliveryIssue(),
        payments
            .findByOrderId(o.getId())
            .map(
                p ->
                    new PaymentInfo(
                        p.getExpiresAt(),
                        p.getReconciledAt(),
                        p.getReconciliationReference(),
                        !p.getExpiresAt()
                                .plusSeconds(Math.max(0, reconciliationGrace))
                                .isAfter(Instant.now())
                            && Set.of("PENDING", "CONFIRMED", "PACKING").contains(o.getStatus())
                            && Set.of("UNPAID", "FAILED")
                                .contains(CheckoutService.view(o).paymentStatus())))
            .orElse(null));
  }

  @Transactional
  public Entry shipment(Long id, String carrier, String tracking, String actor) {
    var o = orders.lockId(id).orElseThrow(() -> missing("Không tìm thấy đơn"));
    if ("REQUESTED".equals(o.getCancellationStatus()))
      throw bad("Cần xử lý yêu cầu hủy trước khi tạo/đổi vận đơn");
    if (!o.getChannel().equals("ONLINE")
        || !Set.of("CONFIRMED", "PACKING", "DELIVERY_FAILED").contains(o.getStatus()))
      throw bad("Chỉ gắn vận đơn cho đơn online đã xác nhận/đóng gói");
    if (!Objects.equals(o.getCarrier(), carrier)
        || !Objects.equals(o.getTrackingCode(), tracking)) {
      o.setCarrier(carrier.trim());
      o.setTrackingCode(tracking.trim());
      audit.record(o, o.getStatus(), actor, "Gắn vận đơn " + carrier + ": " + tracking);
    }
    return entry(o);
  }

  @Transactional(readOnly = true)
  public Entry detail(Long id) {
    return entry(orders.findById(id).orElseThrow(() -> missing("Không tìm thấy đơn")));
  }

  @Transactional(readOnly = true)
  public PageView<Entry> list(int page) {
    return list(page, null, null, null);
  }

  @Transactional(readOnly = true)
  public PageView<Entry> list(int page, String status, String channel, String q) {
    return list(page, status, channel, q, null, null);
  }

  @Transactional(readOnly = true)
  public PageView<Entry> list(
      int page,
      String status,
      String channel,
      String q,
      String paymentStatus,
      String cancellationStatus) {
    if (status != null
        && !status.isBlank()
        && !Set.of(
                "PENDING",
                "CONFIRMED",
                "PACKING",
                "SHIPPING",
                "COMPLETED",
                "CANCELLED",
                "DELIVERY_FAILED",
                "RETURNING",
                "RETURNED")
            .contains(status)) throw bad("Trạng thái không hợp lệ");
    if (channel != null && !channel.isBlank() && !Set.of("POS", "ONLINE").contains(channel))
      throw bad("Kênh bán không hợp lệ");
    if (paymentStatus != null
        && !paymentStatus.isBlank()
        && !Set.of("PAID", "UNPAID", "FAILED", "REFUNDED").contains(paymentStatus))
      throw bad("Trạng thái thanh toán không hợp lệ");
    if (cancellationStatus != null
        && !cancellationStatus.isBlank()
        && !Set.of("REQUESTED", "APPROVED", "REJECTED").contains(cancellationStatus))
      throw bad("Trạng thái yêu cầu hủy không hợp lệ");
    Specification<CustomerOrder> filter =
        (r, query, cb) -> {
          var terms = new ArrayList<jakarta.persistence.criteria.Predicate>();
          if (paymentStatus != null && !paymentStatus.isBlank()) {
            var effective =
                cb.<String>selectCase()
                    .when(cb.isNotNull(r.get("paymentStatus")), r.get("paymentStatus"))
                    .when(
                        cb.or(
                            cb.equal(r.get("channel"), "POS"),
                            cb.equal(r.get("status"), "COMPLETED")),
                        "PAID")
                    .otherwise("UNPAID");
            terms.add(cb.equal(effective, paymentStatus));
          }
          if (cancellationStatus != null && !cancellationStatus.isBlank())
            terms.add(cb.equal(r.get("cancellationStatus"), cancellationStatus));
          if (status != null && !status.isBlank()) terms.add(cb.equal(r.get("status"), status));
          if (channel != null && !channel.isBlank()) terms.add(cb.equal(r.get("channel"), channel));
          if (q != null && !q.isBlank()) {
            String key =
                "%"
                    + q.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                        .replace("[", "\\[")
                    + "%";
            terms.add(
                cb.or(
                    cb.like(cb.lower(r.get("code")), key, '\\'),
                    cb.like(cb.lower(r.get("customerName")), key, '\\'),
                    cb.like(r.get("phone"), key, '\\')));
          }
          return cb.and(terms.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    var result =
        orders.findAll(
            filter,
            PageRequest.of(
                page, 20, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
    return new PageView<>(
        result.getContent().stream().map(this::entry).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  @Transactional
  public OrderView status(Long id, String status) {
    return status(id, status, "system", "", null);
  }

  @Transactional
  public OrderView status(
      Long id, String status, String actor, String reason, String expectedStatus) {
    return status(id, status, actor, reason, expectedStatus, false, false);
  }

  @Transactional
  public OrderView status(
      Long id,
      String status,
      String actor,
      String reason,
      String expectedStatus,
      boolean goodsReceived,
      boolean waybillCancelled) {
    var order = orders.lockId(id).orElseThrow(() -> missing("Không tìm thấy đơn"));
    String from = order.getStatus();
    if (from.equals(status)) return CheckoutService.view(order);
    if (expectedStatus != null && !expectedStatus.equals(from))
      throw bad("Đơn đã được người khác cập nhật; hãy tải lại trước khi xác nhận");
    var allowed =
        switch (from) {
          case "PENDING" -> Set.of("CONFIRMED", "CANCELLED");
          case "CONFIRMED" -> Set.of("PACKING", "CANCELLED");
          case "PACKING" -> Set.of("SHIPPING", "CANCELLED");
          case "SHIPPING" -> Set.of("COMPLETED", "DELIVERY_FAILED");
          case "DELIVERY_FAILED" -> Set.of("SHIPPING", "RETURNING");
          case "RETURNING" -> Set.of("RETURNED");
          default -> Set.<String>of();
        };
    if (!allowed.contains(status))
      throw bad("Không được bỏ qua bước hoặc hủy/hoàn kho đơn đã giao cho vận chuyển");
    if ("REQUESTED".equals(order.getCancellationStatus()) && !status.equals("CANCELLED"))
      throw bad("Cần duyệt hoặc từ chối yêu cầu hủy trước khi tiếp tục xử lý đơn");
    if (Set.of("DELIVERY_FAILED", "RETURNING", "RETURNED").contains(status)
        && (reason == null || reason.trim().length() < 5))
      throw bad("Cần ghi rõ lý do giao thất bại/hoàn hàng (ít nhất 5 ký tự)");
    if (status.equals("RETURNED") && !goodsReceived)
      throw bad("Chỉ hoàn tồn sau khi nhận và kiểm tra toàn bộ hàng có thể bán lại");
    if (status.equals("CANCELLED")) {
      if (order.getTrackingCode() != null
          && !order.getTrackingCode().isBlank()
          && !waybillCancelled)
        throw bad("Cần xác nhận đã hủy vận đơn bên ngoài trước khi hủy đơn và trả tồn");
      release.release(order, true);
      if ("REQUESTED".equals(order.getCancellationStatus())) {
        order.setCancellationStatus("APPROVED");
        order.setCancellationResolvedAt(Instant.now());
        order.setCancellationResolvedBy(actor);
        order.setCancellationDecisionReason(
            reason == null
                ? "Duyệt hủy theo quyết định quản trị"
                : reason.substring(0, Math.min(500, reason.length())));
      }
    }
    if (Set.of("SHIPPING", "COMPLETED").contains(status)
        && order.getPaymentMethod().equals("VNPAY")
        && !"PAID".equals(order.getPaymentStatus()))
      throw bad("Cần VNPAY xác nhận đã thu tiền trước khi xử lý giao hàng");
    if (status.equals("RETURNED")) release.release(order, true);
    if (status.equals("DELIVERY_FAILED") || status.equals("RETURNING"))
      order.setDeliveryIssue(reason.trim());
    if (status.equals("SHIPPING")) {
      if (order.getTrackingCode() == null || order.getCarrier() == null)
        throw bad("Cần gắn hãng giao hàng và mã vận đơn trước khi giao");
      order.setShippedAt(Instant.now());
    }
    if (status.equals("COMPLETED")) {
      order.setCompletedAt(Instant.now());
      if (order.getPaymentMethod().equals("COD")) order.setPaymentStatus("PAID");
    }
    order.setStatus(status);
    audit.record(order, from, actor, reason);
    return CheckoutService.view(order);
  }

  @Transactional
  public OrderView requestCancellation(String code, String accountId, String reason) {
    if (reason == null || reason.trim().length() < 5 || reason.length() > 500)
      throw bad("Cần lý do hủy từ 5 đến 500 ký tự");
    var found = orders.findByCode(code).orElseThrow(() -> missing("Không tìm thấy đơn của bạn"));
    var o = orders.lockId(found.getId()).orElseThrow();
    if (o.getShopAccount() == null
        || !o.getShopAccount().getId().equals(accountId)
        || !o.getChannel().equals("ONLINE")) throw missing("Không tìm thấy đơn của bạn");
    if (!Set.of("PENDING", "CONFIRMED", "PACKING").contains(o.getStatus()))
      throw bad("Chỉ yêu cầu hủy trước khi đơn được bàn giao vận chuyển");
    if ("REQUESTED".equals(o.getCancellationStatus())) return CheckoutService.view(o);
    o.setCancellationStatus("REQUESTED");
    o.setCancellationReason(reason.trim());
    o.setCancellationRequestedAt(Instant.now());
    o.setCancellationResolvedAt(null);
    o.setCancellationResolvedBy(null);
    o.setCancellationDecisionReason(null);
    audit.record(o, o.getStatus(), "customer:" + accountId, "Khách yêu cầu hủy: " + reason);
    return CheckoutService.view(o);
  }

  @Transactional
  public OrderView decideCancellation(
      Long id, boolean approve, String reason, String actor, boolean waybillCancelled) {
    if (reason == null || reason.trim().length() < 5 || reason.length() > 490)
      throw bad("Cần lý do quyết định từ 5 đến 490 ký tự");
    var o = orders.lockId(id).orElseThrow(() -> missing("Không tìm thấy đơn"));
    if (!"REQUESTED".equals(o.getCancellationStatus())) {
      if ((approve && "APPROVED".equals(o.getCancellationStatus()))
          || (!approve && "REJECTED".equals(o.getCancellationStatus())))
        return CheckoutService.view(o);
      throw bad("Không còn yêu cầu hủy đang chờ duyệt");
    }
    if (approve)
      return status(
          id, "CANCELLED", actor, "Duyệt hủy: " + reason, o.getStatus(), false, waybillCancelled);
    o.setCancellationStatus("REJECTED");
    o.setCancellationResolvedAt(Instant.now());
    o.setCancellationResolvedBy(actor);
    o.setCancellationDecisionReason(reason.trim());
    audit.record(o, o.getStatus(), actor, "Từ chối yêu cầu hủy: " + reason);
    return CheckoutService.view(o);
  }

  @Transactional(readOnly = true)
  public PageView<Entry> overdue(int page) {
    var result =
        payments.expired(
            Instant.now().minusSeconds(Math.max(0, reconciliationGrace)),
            PageRequest.of(page, 20, Sort.by("expiresAt")));
    return new PageView<>(
        result.getContent().stream().map(p -> entry(p.getOrder())).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  @Transactional
  public OrderView expireAfterReconciliation(
      Long id, String reference, String actor, boolean waybillCancelled) {
    var o = orders.lockId(id).orElseThrow(() -> missing("Không tìm thấy đơn"));
    var p = payments.findByOrderId(id).orElseThrow(() -> bad("Đơn không có giao dịch VNPAY"));
    if (!o.getPaymentMethod().equals("VNPAY")) throw bad("Chỉ áp dụng cho VNPAY");
    if ("PAID".equals(o.getPaymentStatus())
        || "PAID".equals(p.getStatus())
        || "REFUNDED".equals(o.getPaymentStatus()))
      throw bad("Đơn đã thu/hoàn tiền; không được hủy do chưa thanh toán");
    if (o.getStatus().equals("CANCELLED") && p.getReconciledAt() != null)
      return CheckoutService.view(o);
    if (!Set.of("PENDING", "CONFIRMED", "PACKING").contains(o.getStatus())
        || p.getExpiresAt().plusSeconds(Math.max(0, reconciliationGrace)).isAfter(Instant.now()))
      throw bad("Đơn chưa hết hạn và thời gian chờ đối soát, hoặc đã giao hàng");
    if (reference == null || reference.trim().length() < 5 || reference.length() > 120)
      throw bad("Cần mã/tài liệu đối soát thực tế từ merchant portal");
    p.setReconciledAt(Instant.now());
    p.setReconciledBy(actor);
    p.setReconciliationReference(reference.trim());
    return status(
        id,
        "CANCELLED",
        actor,
        "Hết hạn, admin đối soát chưa thu tiền: " + reference,
        o.getStatus(),
        false,
        waybillCancelled);
  }
}
