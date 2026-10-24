package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.PosApi.Quote;
import com.example.sneaker.dto.PromotionApi.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {
  private final PromotionRepository promotions;
  private final ProductRepository products;

  public static String normalize(String code) {
    return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
  }

  public List<View> list() {
    return promotions.findAll().stream().map(PromotionService::view).toList();
  }

  @Transactional
  public View create(Input input) {
    var p = new Promotion();
    apply(p, input);
    promotions.saveAndFlush(p);
    return view(p);
  }

  @Transactional
  public View update(Long id, Input input) {
    var p = promotions.lockId(id).orElseThrow(() -> missing("Không tìm thấy khuyến mãi"));
    if (!p.getCode().equals(normalize(input.code())))
      throw bad("Không đổi mã đã tạo; hãy tạo khuyến mãi mới");
    apply(p, input);
    return view(p);
  }

  @Transactional
  public void disable(Long id) {
    promotions.lockId(id).orElseThrow(() -> missing("Không tìm thấy khuyến mãi")).setActive(false);
  }

  private void apply(Promotion p, Input i) {
    if (!i.endsAt().isAfter(i.startsAt())) throw bad("Ngày kết thúc phải sau ngày bắt đầu");
    if (i.type().equals("PERCENT") && i.value().compareTo(BigDecimal.valueOf(100)) > 0)
      throw bad("Phần trăm không được vượt 100");
    if (i.maxUses() < p.getUsedCount()) throw bad("Hạn mức không được nhỏ hơn lượt đã sử dụng");
    var selected = products.findAllById(new HashSet<>(i.productIds()));
    if (selected.size() != new HashSet<>(i.productIds()).size())
      throw bad("Sản phẩm khuyến mãi không tồn tại");
    p.setCode(normalize(i.code()));
    p.setName(i.name().trim());
    p.setType(i.type());
    p.setValue(i.value());
    p.setMinSubtotal(i.minSubtotal());
    p.setMaxDiscount(i.maxDiscount());
    p.setMaxUses(i.maxUses());
    p.setStartsAt(i.startsAt());
    p.setEndsAt(i.endsAt());
    p.setActive(i.active());
    p.setProducts(new HashSet<>(selected));
  }

  public Quote quote(
      String code, Map<Long, Integer> quantities, Map<Long, ProductVariant> variants) {
    return calculate(code, quantities, variants, false);
  }

  @Transactional
  public void applyOrder(
      CustomerOrder order,
      String code,
      Map<Long, Integer> quantities,
      Map<Long, ProductVariant> variants) {
    var quote = calculate(code, quantities, variants, true);
    order.setSubtotal(quote.subtotal());
    order.setDiscount(quote.discount());
    order.setTotal(quote.total());
    order.setPromotionCode(quote.promotionCode().isBlank() ? null : quote.promotionCode());
  }

  private Quote calculate(
      String code,
      Map<Long, Integer> quantities,
      Map<Long, ProductVariant> variants,
      boolean redeem) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var e : quantities.entrySet()) {
      var v = variants.get(e.getKey());
      if (v == null
          || !v.isActive()
          || !v.getProduct().isActive()
          || !v.getProduct().getCategory().isActive()
          || v.getStock() < 0) throw bad("Biến thể không hợp lệ");
      subtotal = subtotal.add(v.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
    }
    String normalized = normalize(code);
    if (normalized.isEmpty()) return new Quote(subtotal, BigDecimal.ZERO, subtotal, "");
    var p =
        (redeem ? promotions.lockCode(normalized) : promotions.findByCode(normalized))
            .orElseThrow(() -> bad("Mã giảm giá không tồn tại"));
    Instant now = Instant.now();
    if (!p.isActive()
        || now.isBefore(p.getStartsAt())
        || !now.isBefore(p.getEndsAt())
        || p.getUsedCount() >= p.getMaxUses())
      throw bad("Mã đã hết hạn, hết lượt hoặc ngừng hoạt động");
    var ids = p.getProducts().stream().map(Product::getId).toList();
    BigDecimal eligible = BigDecimal.ZERO;
    for (var e : quantities.entrySet()) {
      var v = variants.get(e.getKey());
      if (ids.contains(v.getProduct().getId()))
        eligible = eligible.add(v.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
    }
    if (eligible.signum() <= 0 || eligible.compareTo(p.getMinSubtotal()) < 0)
      throw bad("Sản phẩm áp dụng chưa đạt điều kiện khuyến mãi");
    BigDecimal discount =
        p.getType().equals("PERCENT")
            ? eligible.multiply(p.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
            : p.getValue();
    if (p.getMaxDiscount() != null) discount = discount.min(p.getMaxDiscount());
    discount = discount.min(eligible);
    if (redeem) p.setUsedCount(p.getUsedCount() + 1);
    return new Quote(subtotal, discount, subtotal.subtract(discount), normalized);
  }

  public static View view(Promotion p) {
    return new View(
        p.getId(),
        p.getCode(),
        p.getName(),
        p.getType(),
        p.getValue(),
        p.getMinSubtotal(),
        p.getMaxDiscount(),
        p.getMaxUses(),
        p.getUsedCount(),
        p.getStartsAt(),
        p.getEndsAt(),
        p.isActive(),
        p.getProducts().stream().map(Product::getId).sorted().toList());
  }
}
