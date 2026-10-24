package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.bad;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {
  private final InventoryService inventory;
  private final OrderRepository orders;
  private final PromotionService promotions;

  @Transactional
  public OrderView checkout(Checkout input) {
    String key = input.requestKey().toString();
    var existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    Map<Long, Integer> quantities = new TreeMap<>();
    input.items().forEach(i -> quantities.merge(i.variantId(), i.quantity(), Integer::sum));
    var locked = inventory.lock(quantities);
    existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    var order = new CustomerOrder();
    order.setRequestKey(key);
    order.setCode("F6S-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
    order.setCustomerName(input.customerName().trim());
    order.setEmail(input.email().trim());
    order.setPhone(input.phone());
    order.setAddress(input.address().trim());
    order.setNote(input.note() == null ? "" : input.note().trim());
    inventory.consume(order, quantities, locked);
    promotions.applyOrder(order, input.promotionCode(), quantities, locked);
    orders.saveAndFlush(order);
    return view(order);
  }

  private OrderView replay(CustomerOrder order, Checkout input) {
    Map<Long, Integer> requested = new TreeMap<>(), saved = new TreeMap<>();
    input.items().forEach(i -> requested.merge(i.variantId(), i.quantity(), Integer::sum));
    order
        .getItems()
        .forEach(i -> saved.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
    if (!order.getChannel().equals("ONLINE")
        || !requested.equals(saved)
        || !order.getEmail().equals(input.email().trim())
        || !order.getPhone().equals(input.phone())
        || !order.getCustomerName().equals(input.customerName().trim())
        || !order.getAddress().equals(input.address().trim())
        || !order.getNote().equals(input.note() == null ? "" : input.note().trim())
        || !PromotionService.normalize(order.getPromotionCode())
            .equals(PromotionService.normalize(input.promotionCode())))
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
            .toList(),
        o.getSubtotal(),
        o.getDiscount(),
        o.getPromotionCode(),
        o.getChannel(),
        o.getCashReceived(),
        o.getCashChange());
  }
}
