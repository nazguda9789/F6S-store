package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.model.CustomerOrder;
import com.example.sneaker.repository.PromotionRepository;
import java.time.Instant;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caller must hold the order row lock. Both financial and physical return use this guard. */
@Service
@RequiredArgsConstructor
public class InventoryReleaseService {
  private final InventoryService inventory;
  private final PromotionRepository promotions;

  @Transactional
  public boolean release(CustomerOrder order, boolean releasePromotion) {
    if (order.getStockReleasedAt() != null) return false;
    var quantities = new TreeMap<Long, Integer>();
    order
        .getItems()
        .forEach(i -> quantities.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
    var locked = inventory.lock(quantities);
    quantities.forEach(
        (id, qty) -> locked.get(id).setStock(Math.addExact(locked.get(id).getStock(), qty)));
    if (releasePromotion && order.getPromotionCode() != null) {
      var p =
          promotions
              .lockCode(order.getPromotionCode())
              .orElseThrow(() -> bad("Không tìm thấy mã giảm của đơn"));
      if (p.getUsedCount() < 1) throw bad("Lượt khuyến mãi không nhất quán");
      p.setUsedCount(p.getUsedCount() - 1);
    }
    order.setStockReleasedAt(Instant.now());
    return true;
  }
}
