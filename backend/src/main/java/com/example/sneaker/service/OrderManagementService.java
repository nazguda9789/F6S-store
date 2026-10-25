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
  private final InventoryService inventory;
  private final PromotionRepository promotions;
  private final OrderEventRepository events;
  private final OrderAuditService audit;

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
      List<Event> timeline) {}

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
            .toList());
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
    if (status != null
        && !status.isBlank()
        && !Set.of("PENDING", "CONFIRMED", "PACKING", "SHIPPING", "COMPLETED", "CANCELLED")
            .contains(status)) throw bad("Trạng thái không hợp lệ");
    if (channel != null && !channel.isBlank() && !Set.of("POS", "ONLINE").contains(channel))
      throw bad("Kênh bán không hợp lệ");
    Specification<CustomerOrder> filter =
        (r, query, cb) -> {
          var terms = new ArrayList<jakarta.persistence.criteria.Predicate>();
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
          case "SHIPPING" -> Set.of("COMPLETED");
          default -> Set.<String>of();
        };
    if (!allowed.contains(status))
      throw bad("Không được bỏ qua bước hoặc hủy/hoàn kho đơn đã giao cho vận chuyển");
    if (status.equals("CANCELLED")) {
      var quantities = new TreeMap<Long, Integer>();
      order
          .getItems()
          .forEach(i -> quantities.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
      var locked = inventory.lock(quantities);
      quantities.forEach(
          (variantId, quantity) -> {
            var v = locked.get(variantId);
            v.setStock(Math.addExact(v.getStock(), quantity));
          });
      if (order.getPromotionCode() != null) {
        var p =
            promotions
                .lockCode(order.getPromotionCode())
                .orElseThrow(() -> bad("Không tìm thấy mã của đơn"));
        if (p.getUsedCount() < 1) throw bad("Lượt mã không nhất quán");
        p.setUsedCount(p.getUsedCount() - 1);
      }
    }
    if (status.equals("COMPLETED")) order.setCompletedAt(Instant.now());
    order.setStatus(status);
    audit.record(order, from, actor, reason);
    return CheckoutService.view(order);
  }
}
