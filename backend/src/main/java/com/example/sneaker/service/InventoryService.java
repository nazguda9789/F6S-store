package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.bad;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {
  private final ProductRepository products;
  private final VariantRepository variants;

  public Map<Long, ProductVariant> lock(Map<Long, Integer> quantities) {
    if (quantities.isEmpty()
        || quantities.size() > 50
        || quantities.values().stream().anyMatch(q -> q < 1 || q > 99))
      throw bad("Giỏ hàng không hợp lệ, tối đa 50 biến thể và 99 đôi/biến thể");
    var identities = variants.identities(quantities.keySet());
    if (identities.size() != quantities.size()) throw bad("Biến thể không tồn tại");
    var locked = new HashMap<Long, ProductVariant>();
    identities.stream()
        .map(VariantRepository.Identity::getProductId)
        .distinct()
        .sorted()
        .forEach(
            id -> {
              var p = products.lockById(id).orElseThrow(() -> bad("Sản phẩm không tồn tại"));
              p.getVariants().forEach(v -> locked.put(v.getId(), v));
            });
    return locked;
  }

  public BigDecimal consume(
      CustomerOrder order, Map<Long, Integer> quantities, Map<Long, ProductVariant> locked) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var entry : quantities.entrySet()) {
      var v = locked.get(entry.getKey());
      var p = v.getProduct();
      int quantity = entry.getValue();
      if (!v.isActive() || !p.isActive() || !p.getCategory().isActive())
        throw bad("Sản phẩm đã ngừng bán");
      if (v.getStock() < quantity)
        throw bad("Không đủ tồn kho: " + p.getName() + " / " + v.getColor() + " / " + v.getSize());
      if (v.getPrice() == null || v.getPrice().signum() <= 0)
        throw bad("Biến thể chưa có giá hợp lệ");
      v.setStock(v.getStock() - quantity);
      var line = new OrderItem();
      line.setOrder(order);
      line.setVariant(v);
      line.setProductName(p.getName());
      line.setSku(v.getSku());
      line.setColor(v.getColor());
      line.setSize(v.getSize());
      line.setQuantity(quantity);
      line.setUnitPrice(v.getPrice());
      order.getItems().add(line);
      subtotal = subtotal.add(v.getPrice().multiply(BigDecimal.valueOf(quantity)));
    }
    return subtotal;
  }
}
