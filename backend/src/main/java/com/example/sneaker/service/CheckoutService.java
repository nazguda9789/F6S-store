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
  private final OrderAuditService audit;
  private final ShippingService shipping;
  private final VnpayService vnpay;
  private final ShopAuthService accounts;

  @Transactional
  public OrderView checkout(Checkout input) {
    String key = input.requestKey().toString();
    var account = accounts.current();
    if (account != null && !account.getEmail().equalsIgnoreCase(input.email().trim()))
      throw bad("Email đặt hàng phải khớp tài khoản đã xác minh");
    var existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    String method = input.paymentMethod() == null ? "COD" : input.paymentMethod();
    if (!Set.of("COD", "VNPAY").contains(method)) throw bad("Phương thức thanh toán không hợp lệ");
    if (method.equals("VNPAY") && !vnpay.enabled()) throw bad("VNPAY chưa được cấu hình");
    Map<Long, Integer> quantities = new TreeMap<>();
    input.items().forEach(i -> quantities.merge(i.variantId(), i.quantity(), Integer::sum));
    var locked = inventory.lock(quantities);
    existing = orders.findByRequestKey(key);
    if (existing.isPresent()) return replay(existing.get(), input);
    var order = new CustomerOrder();
    order.setRequestKey(key);
    order.setShopAccount(account);
    order.setCode("F6S-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
    order.setCustomerName(input.customerName().trim());
    order.setEmail(input.email().trim());
    order.setPhone(input.phone());
    order.setAddress(input.address().trim());
    order.setNote(input.note() == null ? "" : input.note().trim());
    order.setPaymentMethod(method);
    order.setPaymentStatus("UNPAID");
    inventory.consume(order, quantities, locked);
    promotions.applyOrder(order, input.promotionCode(), quantities, locked);
    order.setShippingFee(shipping.quote(order.getTotal()));
    order.setTotal(order.getTotal().add(order.getShippingFee()));
    orders.saveAndFlush(order);
    if (method.equals("VNPAY")) vnpay.prepare(order);
    audit.record(order, null, "guest", "Đặt hàng online " + method);
    return result(order);
  }

  private OrderView replay(CustomerOrder order, Checkout input) {
    var account = accounts.current();
    if (order.getShopAccount() != null
        && (account == null || !order.getShopAccount().getId().equals(account.getId())))
      throw bad("Đơn thuộc tài khoản khác");
    Map<Long, Integer> requested = new TreeMap<>(), saved = new TreeMap<>();
    input.items().forEach(i -> requested.merge(i.variantId(), i.quantity(), Integer::sum));
    order
        .getItems()
        .forEach(i -> saved.merge(i.getVariant().getId(), i.getQuantity(), Integer::sum));
    if (!order.getChannel().equals("ONLINE")
        || !order
            .getPaymentMethod()
            .equals(input.paymentMethod() == null ? "COD" : input.paymentMethod())
        || !requested.equals(saved)
        || !order.getEmail().equals(input.email().trim())
        || !order.getPhone().equals(input.phone())
        || !order.getCustomerName().equals(input.customerName().trim())
        || !order.getAddress().equals(input.address().trim())
        || !order.getNote().equals(input.note() == null ? "" : input.note().trim())
        || !PromotionService.normalize(order.getPromotionCode())
            .equals(PromotionService.normalize(input.promotionCode())))
      throw bad("Mã yêu cầu đã được sử dụng cho nội dung khác");
    return result(order);
  }

  private OrderView result(CustomerOrder o) {
    var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    String ip =
        attrs instanceof org.springframework.web.context.request.ServletRequestAttributes a
            ? a.getRequest().getRemoteAddr()
            : "127.0.0.1";
    return view(o, vnpay.url(o, ip));
  }

  public static OrderView view(CustomerOrder o) {
    return view(o, null);
  }

  private static OrderView view(CustomerOrder o, String paymentUrl) {
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
        o.getCashChange(),
        o.getCustomerName(),
        o.getPhone(),
        o.getPaymentStatus() == null
            ? (o.getChannel().equals("POS") || o.getStatus().equals("COMPLETED")
                ? "PAID"
                : "UNPAID")
            : o.getPaymentStatus(),
        o.getShippingFee() == null ? java.math.BigDecimal.ZERO : o.getShippingFee(),
        paymentUrl);
  }
}
