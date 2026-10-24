package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.repository.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderManagementService {
  private final OrderRepository orders;
  private final InventoryService inventory;
  private final PromotionRepository promotions;

  public record Entry(
      Long id,
      String customerName,
      String email,
      String phone,
      String address,
      String note,
      OrderView order) {}

  @Transactional(readOnly = true)
  public PageView<Entry> list(int page) {
    var result = orders.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20));
    return new PageView<>(
        result.getContent().stream()
            .map(
                o ->
                    new Entry(
                        o.getId(),
                        o.getCustomerName(),
                        o.getEmail(),
                        o.getPhone(),
                        o.getAddress(),
                        o.getNote(),
                        CheckoutService.view(o)))
            .toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  @Transactional
  public OrderView status(Long id, String status) {
    var order = orders.lockId(id).orElseThrow(() -> missing("Không tìm thấy đơn"));
    if (order.getStatus().equals(status)) return CheckoutService.view(order);
    if (!order.getStatus().equals("PENDING")
        || (!status.equals("COMPLETED") && !status.equals("CANCELLED")))
      throw bad(
          "Chỉ chuyển đơn chờ sang hoàn thành hoặc hủy; không tự hoàn tiền đơn đã hoàn thành");
    if (status.equals("CANCELLED")) {
      var quantities = new TreeMap<Long, Integer>();
      order.getItems().forEach(i -> quantities.put(i.getVariant().getId(), i.getQuantity()));
      var locked = inventory.lock(quantities);
      quantities.forEach(
          (variantId, quantity) -> {
            var v = locked.get(variantId);
            v.setStock(Math.addExact(v.getStock(), quantity));
          });
      if (order.getPromotionCode() != null) {
        var promotion =
            promotions
                .lockCode(order.getPromotionCode())
                .orElseThrow(() -> bad("Không tìm thấy khuyến mãi của đơn"));
        if (promotion.getUsedCount() < 1) throw bad("Lượt khuyến mãi không nhất quán");
        promotion.setUsedCount(promotion.getUsedCount() - 1);
      }
    } else order.setCompletedAt(Instant.now());
    order.setStatus(status);
    return CheckoutService.view(order);
  }
}
