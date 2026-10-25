package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.Api.OrderView;
import com.example.sneaker.dto.PosApi.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PosService {
  private final PosDraftRepository drafts;
  private final PosOperatorRepository operators;
  private final PosScanRepository scans;
  private final VariantRepository variants;
  private final OrderRepository orders;
  private final InventoryService inventory;
  private final PromotionService promotions;
  private final CustomerRepository customers;
  private final CustomerAddressRepository customerAddresses;
  private final OrderAuditService audit;

  public List<Draft> list(String owner) {
    return drafts.findByOwnerAndStatusOrderByIdAsc(owner, "OPEN").stream().map(this::view).toList();
  }

  @Transactional
  public Draft create(String owner, NewDraft input) {
    operators.lock(owner).orElseThrow(() -> bad("Quầy chưa được khởi tạo cho tài khoản"));
    var old = drafts.findByRequestKey(input.requestKey().toString());
    if (old.isPresent()) {
      checkOwner(old.get(), owner);
      return view(old.get());
    }
    if (drafts.countByOwnerAndStatus(owner, "OPEN") >= 5)
      throw bad("Tối đa 5 hóa đơn tạm đang mở cho mỗi nhân viên");
    var draft = new PosDraft();
    draft.setOwner(owner);
    draft.setRequestKey(input.requestKey().toString());
    drafts.saveAndFlush(draft);
    return view(draft);
  }

  private void checkOwner(PosDraft draft, String owner) {
    if (!draft.getOwner().equals(owner))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn của bạn");
  }

  private PosDraft lock(Long id, String owner) {
    var draft = drafts.lock(id).orElseThrow(() -> missing("Không tìm thấy hóa đơn"));
    checkOwner(draft, owner);
    return draft;
  }

  private void open(PosDraft d) {
    if (!d.getStatus().equals("OPEN")) throw bad("Hóa đơn không còn ở trạng thái mở");
  }

  public static String parseSku(String value) {
    String code = value.trim().toUpperCase(Locale.ROOT);
    if (code.startsWith("F6S:VARIANT:")) code = code.substring(12);
    if (!code.matches("SKU-[A-Z0-9-]{1,50}"))
      throw bad("QR không hợp lệ. Dùng SKU hoặc F6S:VARIANT:SKU-...");
    return code;
  }

  @Transactional
  public Draft scan(Long id, String owner, Scan input) {
    if (input.quantity() < 1 || input.quantity() > 99) throw bad("Số lượng quét phải từ 1 đến 99");
    var d = lock(id, owner);
    open(d);
    String sku = parseSku(input.code());
    var prior = scans.findById(input.eventKey().toString());
    if (prior.isPresent()) {
      var event = prior.get();
      if (!event.getDraft().getId().equals(id)
          || !event.getSku().equals(sku)
          || event.getQuantity() != input.quantity())
        throw bad("Mã quét đã được dùng cho yêu cầu khác");
      return view(d);
    }
    var variant = variants.findBySku(sku).orElseThrow(() -> missing("Không tìm thấy SKU"));
    var p = variant.getProduct();
    if (!variant.isActive() || !p.isActive() || !p.getCategory().isActive())
      throw bad("Sản phẩm đã ngừng bán");
    var line =
        d.getLines().stream()
            .filter(l -> l.getVariant().getId().equals(variant.getId()))
            .findFirst()
            .orElse(null);
    int quantity = (line == null ? 0 : line.getQuantity()) + input.quantity();
    if (quantity > 99 || quantity > variant.getStock())
      throw bad("Không đủ tồn kho hoặc quá 99 đôi/biến thể");
    if (line == null) {
      if (d.getLines().size() >= 50) throw bad("Tối đa 50 biến thể mỗi hóa đơn");
      line = new PosLine();
      line.setDraft(d);
      line.setVariant(variant);
      d.getLines().add(line);
    }
    line.setQuantity(quantity);
    var event = new PosScanEvent();
    event.setEventKey(input.eventKey().toString());
    event.setDraft(d);
    event.setSku(sku);
    event.setQuantity(input.quantity());
    scans.save(event);
    drafts.flush();
    return view(d);
  }

  @Transactional
  public Draft quantity(Long id, Long variantId, String owner, Quantity input) {
    if (input.quantity() < 1 || input.quantity() > 99) throw bad("Số lượng phải từ 1 đến 99");
    var d = lock(id, owner);
    open(d);
    var line =
        d.getLines().stream()
            .filter(l -> l.getVariant().getId().equals(variantId))
            .findFirst()
            .orElseThrow(() -> missing("Không tìm thấy dòng hàng"));
    if (input.quantity() > line.getVariant().getStock()) throw bad("Số lượng vượt tồn kho");
    line.setQuantity(input.quantity());
    return view(d);
  }

  @Transactional
  public Draft remove(Long id, Long variantId, String owner) {
    var d = lock(id, owner);
    open(d);
    if (!d.getLines().removeIf(l -> l.getVariant().getId().equals(variantId)))
      throw missing("Không tìm thấy dòng hàng");
    return view(d);
  }

  @Transactional
  public void cancel(Long id, String owner) {
    var d = lock(id, owner);
    open(d);
    d.setStatus("CANCELLED");
  }

  @Transactional
  public Draft customer(Long id, String owner, Long customerId) {
    var d = lock(id, owner);
    open(d);
    d.setCustomer(
        customerId == null
            ? null
            : customers
                .findById(customerId)
                .filter(Customer::isActive)
                .orElseThrow(() -> bad("Khách hàng không tồn tại hoặc đã ngừng hoạt động")));
    return view(d);
  }

  private SelectedCustomer selectedCustomer(Customer c) {
    if (c == null) return null;
    var address =
        customerAddresses
            .findByCustomerIdAndActiveTrueOrderByDefaultAddressDescIdAsc(c.getId())
            .stream()
            .findFirst()
            .map(CustomerAddress::fullAddress)
            .orElse("");
    return new SelectedCustomer(
        c.getId(), c.getCode(), c.getFullName(), c.getPhone(), c.getEmail(), address);
  }

  public Quote quote(Long id, String owner, String code) {
    var d = drafts.findById(id).orElseThrow(() -> missing("Không tìm thấy hóa đơn"));
    checkOwner(d, owner);
    open(d);
    var quantities = quantities(d);
    return promotions.quote(code, quantities, variantMap(d));
  }

  @Transactional
  public OrderView pay(Long id, String owner, Payment input) {
    var d = lock(id, owner);
    if (d.getStatus().equals("CHECKED_OUT")) {
      // Reload by the unique placement key AFTER the draft lock. A nullable join snapshot
      // may have been read before a competing transaction committed its invoice relation.
      var o =
          orders
              .findByRequestKey("POS-" + id)
              .orElseThrow(() -> bad("Hóa đơn chưa đồng bộ, vui lòng tải lại"));
      if (o.getCashReceived().compareTo(input.cashReceived()) != 0
          || !PromotionService.normalize(o.getPromotionCode())
              .equals(PromotionService.normalize(input.promotionCode())))
        throw bad("Hóa đơn đã thanh toán với nội dung khác");
      return CheckoutService.view(o);
    }
    open(d);
    var quantities = quantities(d);
    var locked = inventory.lock(quantities);
    var order = new CustomerOrder();
    order.setRequestKey("POS-" + id);
    order.setCode("POS-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
    order.setCustomerName("Khách lẻ");
    order.setEmail("");
    order.setPhone("");
    order.setAddress("Tại quầy");
    order.setNote("");
    if (d.getCustomer() != null) {
      var c = d.getCustomer();
      if (!c.isActive())
        throw bad("Khách hàng đã ngừng hoạt động; hãy chọn khách khác hoặc khách lẻ");
      var snapshot = selectedCustomer(c);
      order.setCustomer(c);
      order.setCustomerName(snapshot.fullName());
      order.setPhone(snapshot.phone());
      order.setEmail(snapshot.email() == null ? "" : snapshot.email());
      order.setAddress(snapshot.address().isBlank() ? "Tại quầy" : snapshot.address());
    }
    order.setChannel("POS");
    order.setPaymentMethod("CASH");
    order.setPaymentStatus("PAID");
    order.setStatus("COMPLETED");
    order.setCompletedAt(Instant.now());
    inventory.consume(order, quantities, locked);
    promotions.applyOrder(order, input.promotionCode(), quantities, locked);
    if (input.expectedTotal() != null && input.expectedTotal().compareTo(order.getTotal()) != 0)
      throw bad(
          "Giá hoặc khuyến mãi đã thay đổi. Tải lại hóa đơn, tính lại và xác nhận tổng tiền trước"
              + " khi thanh toán.");
    if (input.cashReceived().compareTo(order.getTotal()) < 0) throw bad("Tiền khách đưa chưa đủ");
    order.setCashReceived(input.cashReceived());
    order.setCashChange(input.cashReceived().subtract(order.getTotal()));
    orders.saveAndFlush(order);
    audit.record(order, null, owner, "Thanh toán tiền mặt tại quầy");
    d.setInvoice(order);
    d.setStatus("CHECKED_OUT");
    return CheckoutService.view(order);
  }

  public OrderView receipt(Long id, String owner) {
    var d = drafts.findById(id).orElseThrow(() -> missing("Không tìm thấy hóa đơn"));
    checkOwner(d, owner);
    if (d.getInvoice() == null) throw bad("Hóa đơn chưa thanh toán");
    return CheckoutService.view(d.getInvoice());
  }

  private Map<Long, Integer> quantities(PosDraft d) {
    var result = new TreeMap<Long, Integer>();
    d.getLines().forEach(l -> result.put(l.getVariant().getId(), l.getQuantity()));
    if (result.isEmpty()) throw bad("Hóa đơn chưa có sản phẩm");
    return result;
  }

  private Map<Long, ProductVariant> variantMap(PosDraft d) {
    var result = new HashMap<Long, ProductVariant>();
    d.getLines().forEach(l -> result.put(l.getVariant().getId(), l.getVariant()));
    return result;
  }

  private Draft view(PosDraft d) {
    var lines =
        d.getLines().stream()
            .map(
                l -> {
                  var v = l.getVariant();
                  var p = v.getProduct();
                  return new Line(
                      v.getId(),
                      v.getSku(),
                      p.getName(),
                      v.getColor(),
                      v.getSize(),
                      v.isActive() && p.isActive() && p.getCategory().isActive() ? v.getStock() : 0,
                      l.getQuantity(),
                      v.getPrice(),
                      v.getPrice().multiply(BigDecimal.valueOf(l.getQuantity())),
                      p.getImageUrl());
                })
            .toList();
    return new Draft(
        d.getId(),
        d.getStatus(),
        d.getCreatedAt(),
        lines,
        lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
        selectedCustomer(d.getCustomer()));
  }
}
