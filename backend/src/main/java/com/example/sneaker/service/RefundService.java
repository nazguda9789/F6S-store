package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {
  private final OrderRepository orders;
  private final RefundRepository refunds;
  private final InventoryService inventory;
  private final OrderAuditService audit;

  public record View(
      Long id,
      String status,
      BigDecimal amount,
      String reason,
      String settlementReference,
      boolean restocked,
      Instant createdAt,
      Instant resolvedAt) {}

  private View view(OrderRefund r) {
    return new View(
        r.getId(),
        r.getStatus(),
        r.getAmount(),
        r.getReason(),
        r.getSettlementReference(),
        r.isRestocked(),
        r.getCreatedAt(),
        r.getResolvedAt());
  }

  @Transactional(readOnly = true)
  public View detail(Long orderId) {
    return refunds.findByOrderId(orderId).map(this::view).orElse(null);
  }

  @Transactional
  public View request(Long orderId, String reason, String actor) {
    var o = orders.lockId(orderId).orElseThrow(() -> missing("Không tìm thấy đơn"));
    var old = refunds.findByOrderId(orderId);
    if (old.isPresent()) return view(old.get());
    String paid = CheckoutService.view(o).paymentStatus();
    if (!Set.of("COMPLETED", "CANCELLED").contains(o.getStatus()) || !paid.equals("PAID"))
      throw bad("Chỉ hoàn đơn đã thu tiền và đã hoàn tất/hủy");
    var r = new OrderRefund();
    r.setOrder(o);
    r.setReason(reason.trim());
    r.setRequestedBy(actor);
    r.setAmount(o.getTotal());
    refunds.saveAndFlush(r);
    audit.record(o, o.getStatus(), actor, "Mở yêu cầu hoàn toàn bộ tiền: " + reason);
    return view(r);
  }

  @Transactional
  public View resolve(
      Long orderId, boolean approve, boolean received, String reference, String actor) {
    var o = orders.lockId(orderId).orElseThrow(() -> missing("Không tìm thấy đơn"));
    var r = refunds.findByOrderId(orderId).orElseThrow(() -> bad("Chưa có yêu cầu hoàn"));
    if (!r.getStatus().equals("REQUESTED")) return view(r);
    if (approve) {
      if (reference == null || reference.isBlank()) throw bad("Cần mã chứng từ hoàn tiền thực tế");
      if (o.getStatus().equals("COMPLETED")) {
        if (!received) throw bad("Cần xác nhận đã nhận lại và kiểm tra toàn bộ hàng");
        var quantities = new TreeMap<Long, Integer>();
        o.getItems()
            .forEach(i -> quantities.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
        var locked = inventory.lock(quantities);
        quantities.forEach(
            (id, qty) -> locked.get(id).setStock(Math.addExact(locked.get(id).getStock(), qty)));
        r.setRestocked(true);
      }
      r.setStatus("REFUNDED");
      r.setSettlementReference(reference.trim());
      o.setPaymentStatus("REFUNDED");
      o.setRefundedAmount(r.getAmount());
    } else r.setStatus("REJECTED");
    r.setResolvedAt(Instant.now());
    r.setResolvedBy(actor);
    audit.record(
        o,
        o.getStatus(),
        actor,
        approve ? "Đã hoàn tiền theo chứng từ " + reference : "Từ chối yêu cầu hoàn tiền");
    return view(r);
  }
}
