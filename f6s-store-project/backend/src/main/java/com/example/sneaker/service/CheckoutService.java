package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.bad;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {
  private final ProductRepository products;
  private final VariantRepository variants;
  private final OrderRepository orders;

  @Transactional
  public OrderView checkout(Checkout input) {
    String key = input.requestKey().toString();
    var existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    Map<Long, Integer> quantities = new TreeMap<>();
    for (var line : input.items())
      quantities.merge(line.variantId(), line.quantity(), Integer::sum);
    if (quantities.values().stream().anyMatch(n -> n > 99)) throw bad("Tối đa 99 đôi mỗi biến thể");
    var initial = variants.identities(quantities.keySet());
    if (initial.size() != quantities.size()) throw bad("Biến thể không tồn tại");
    // Every inventory mutation locks parent products in the same order, including admin edits.
    var locked = new HashMap<Long, Product>();
    initial.stream()
        .map(VariantRepository.Identity::getProductId)
        .distinct()
        .sorted()
        .forEach(
            id ->
                locked.put(
                    id, products.lockById(id).orElseThrow(() -> bad("Sản phẩm không tồn tại"))));
    // A concurrent retry may have committed while this request waited for stock locks.
    existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    CustomerOrder order = new CustomerOrder();
    order.setRequestKey(key);
    order.setCode("F6S-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
    order.setCustomerName(input.customerName().trim());
    order.setEmail(input.email().trim());
    order.setPhone(input.phone());
    order.setAddress(input.address().trim());
    order.setNote(input.note() == null ? "" : input.note().trim());
    BigDecimal total = BigDecimal.ZERO;
    for (var entry : quantities.entrySet()) {
      // Read stock AFTER acquiring the parent lock; avoid stale managed variants from initial
      // lookup.
      Product p =
          locked.get(
              initial.stream()
                  .filter(v -> v.getId().equals(entry.getKey()))
                  .findFirst()
                  .orElseThrow()
                  .getProductId());
      ProductVariant v =
          p.getVariants().stream()
              .filter(x -> x.getId().equals(entry.getKey()))
              .findFirst()
              .orElseThrow(() -> bad("Biến thể không tồn tại"));
      if (!p.isActive() || !p.getCategory().isActive() || !v.isActive())
        throw bad("Sản phẩm đã ngừng bán");
      if (v.getStock() < entry.getValue())
        throw bad("Không đủ tồn kho: " + p.getName() + " / " + v.getColor() + " / " + v.getSize());
      v.setStock(v.getStock() - entry.getValue());
      OrderItem item = new OrderItem();
      item.setOrder(order);
      item.setVariant(v);
      item.setProductName(p.getName());
      item.setSku(v.getSku());
      item.setColor(v.getColor());
      item.setSize(v.getSize());
      item.setQuantity(entry.getValue());
      item.setUnitPrice(p.getPrice());
      order.getItems().add(item);
      total = total.add(p.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
    }
    order.setTotal(total);
    orders.saveAndFlush(order);
    return view(order);
  }

  private OrderView replay(CustomerOrder order, Checkout input) {
    Map<Long, Integer> requested = new TreeMap<>(), saved = new TreeMap<>();
    input.items().forEach(i -> requested.merge(i.variantId(), i.quantity(), Integer::sum));
    order
        .getItems()
        .forEach(i -> saved.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
    if (!requested.equals(saved)
        || !order.getEmail().equals(input.email().trim())
        || !order.getPhone().equals(input.phone())
        || !order.getCustomerName().equals(input.customerName().trim())
        || !order.getAddress().equals(input.address().trim())
        || !order.getNote().equals(input.note() == null ? "" : input.note().trim()))
      throw bad("Mã yêu cầu đã được sử dụng cho nội dung khác");
    return view(order);
  }

  public static OrderView view(CustomerOrder o) {
    return new OrderView(
        o.getCode(),
        o.getStatus(),
        o.getPaymentMethod(),
        o.getTotal(),
        o.getCreatedAt(),
        o.getItems().stream()
            .map(
                i ->
                    new OrderLineView(
                        i.getProductName(),
                        i.getSku(),
                        i.getColor(),
                        i.getSize(),
                        i.getQuantity(),
                        i.getUnitPrice()))
            .toList());
  }
}
