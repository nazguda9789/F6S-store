package com.web.service;

import com.web.bot.TeleSend;
import com.web.dto.CartResponse;
import com.web.dto.OrderStatusCount;
import com.web.dto.Result;
import com.web.entity.*;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.enums.PayType;
import com.web.mapper.InvoiceMapper;
import com.web.repository.*;
import com.web.utils.EmailInvoice;
import com.web.utils.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderDraftService {

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private OrderDraftRepository orderDraftRepository;

    @Autowired
    private OrderDetailDraftRepository orderDetailDraftRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductColorImageRepository productColorImageRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDetailRepository invoiceDetailRepository;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private EmailInvoice emailInvoice;

    @Autowired
    private HistoryInvoiceStatusRepository historyInvoiceStatusRepository;

    @Autowired
    private TeleSend teleSend;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserUtils userUtils;

    @Autowired private UserRepository userRepository;

    @Transactional
    public Result<?> create(OrderDraft input){
        if ((input.getDiscount() != null && (!Double.isFinite(input.getDiscount()) || input.getDiscount() < 0))
                || (input.getShipCost() != null && (!Double.isFinite(input.getShipCost()) || input.getShipCost() < 0)))
            return Result.fail("Giảm giá và phí vận chuyển phải là số không âm.");
        OrderDraft orderDraft;
        if (input.getId() == null) orderDraft = new OrderDraft();
        else {
            orderDraft = orderDraftRepository.findByIdForUpdate(input.getId()).orElse(null);
            if (orderDraft == null) return Result.fail("Đơn chờ không tồn tại. Vui lòng tải lại danh sách.");
        }
        if (input.getCustomerId() != null) {
            User customer = userRepository.findById(input.getCustomerId()).orElse(null);
            if (customer == null || customer.getRole() != com.web.enums.Role.ROLE_USER
                    || !Boolean.TRUE.equals(customer.getActived()) || Boolean.TRUE.equals(customer.getDeleted())
                    || !Boolean.TRUE.equals(customer.getActivationStatus())) return Result.fail("Khách hàng không hợp lệ hoặc đã ngừng hoạt động.");
        }
        orderDraft.setCustomerId(input.getCustomerId());
        orderDraft.setFullName(input.getFullName() == null || input.getFullName().isBlank() ? "Khách lẻ" : input.getFullName().strip());
        orderDraft.setPhone(input.getPhone()); orderDraft.setEmail(input.getEmail()); orderDraft.setNote(input.getNote());
        orderDraft.setAddressDetail(input.getAddressDetail()); orderDraft.setProvinceCode(input.getProvinceCode());
        orderDraft.setProvinceName(input.getProvinceName()); orderDraft.setWardCode(input.getWardCode()); orderDraft.setWardName(input.getWardName());
        orderDraft.setDiscount(input.getDiscount() == null ? 0D : input.getDiscount());
        orderDraft.setShipCost(input.getShipCost() == null ? 0D : input.getShipCost()); orderDraft.setVoucherCode(input.getVoucherCode());
        Double serverTotal = input.getId() == null ? Double.valueOf(0D) : orderDetailDraftRepository.totalByOrderId(input.getId());
        orderDraft.setTotalAmount(serverTotal == null ? 0D : serverTotal);
        String mess = "Cập nhật đơn chờ thành công";
        boolean isCreate = false;
        if(orderDraft.getId() == null){
            isCreate = true;
            orderDraft.setCreatedBy(userUtils.getUserWithAuthority());
            mess = "Tạo đơn chờ thành công";
        }
        orderDraftRepository.save(orderDraft);
        return new Result<>(true, isCreate==true?orderDraft:null, mess);
    }

    @Transactional
    public Result<?> addToOrder(Long detailId, Long orderId) {
        if (orderDraftRepository.findByIdForUpdate(orderId).isEmpty()) return Result.fail("Đơn chờ không tồn tại hoặc đã thanh toán.");
        ProductDetail selectedVariant = productDetailRepository.findById(detailId).orElse(null);
        if (selectedVariant == null || !Boolean.TRUE.equals(selectedVariant.getActive()) || selectedVariant.getStock() == null || selectedVariant.getStock() < 1)
            return Result.fail("Biến thể không tồn tại, đã ngừng bán hoặc hết hàng.");
        Product selectedProduct = productRepository.findById(selectedVariant.getProductId()).orElse(null);
        if (selectedProduct == null || !Boolean.TRUE.equals(selectedProduct.getActive())) return Result.fail("Sản phẩm đã ngừng kinh doanh.");
        Double price = selectedVariant.getPrice() == null ? selectedProduct.getPrice() : selectedVariant.getPrice();
        if (price == null || !Double.isFinite(price) || price < 0) return Result.fail("Giá sản phẩm chưa hợp lệ.");
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findByDetailAndOrderId(detailId, orderId);
        if(orderDetailDraft.isPresent()){
            return new Result<>(false, null, "Đã tồn tại sản phẩm này trong đơn hàng chờ "+orderDetailDraft.get().getOrderDraft().getId());
        }
        OrderDraft orderDraft = new OrderDraft();
        orderDraft.setId(orderId);
        ProductDetail productDetail = selectedVariant;
        Float weight = productRepository.getWeightById(productDetail.getProductId());
        String image = productColorImageRepository.getImageByProductAndColor(productDetail.getProductId(), productDetail.getColor().getId());
        if(image == null){
            image = productRepository.getImageById(productDetail.getProductId());
        }
        OrderDetailDraft orderDetailDraft1 = new OrderDetailDraft();
        orderDetailDraft1.setIdProductDetail(detailId);
        orderDetailDraft1.setOrderDraft(orderDraft);
        orderDetailDraft1.setWeight(weight);
        orderDetailDraft1.setPrice(price);
        orderDetailDraft1.setSku(productDetail.getSku());
        orderDetailDraft1.setColorName(productDetail.getColor().getName());
        orderDetailDraft1.setQuantity(1);
        orderDetailDraft1.setProductImage(image);
        orderDetailDraft1.setSizeName(productDetail.getSize().getName());
        orderDetailDraft1.setProductName(productRepository.getNameById(productDetail.getProductId()));
        orderDetailDraftRepository.save(orderDetailDraft1);
        updateTotalAmount(orderId);
        Map<String, Object> map = new HashMap<>();
        map.put("detail", orderDetailDraft1);
        map.put("total", orderDraftRepository.getTotalAmount(orderId));
        return new Result<>(true, map, "Thêm sản phẩm vào đơn chờ thành công");
    }

    public List<OrderDetailDraft> findDetailByOrder(Long orderId) {
        List<OrderDetailDraft> list = orderDetailDraftRepository.findByOrder(orderId);
        return list;
    }

    @Transactional
    public Result<?> deleteDetail(Long detailId) {
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findById(detailId);
        if(orderDetailDraft.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy chi tiết đơn chờ");
        }
        Long orderId = orderDetailDraft.get().getOrderDraft().getId();
        if (orderDraftRepository.findByIdForUpdate(orderId).isEmpty()) return Result.fail("Đơn chờ không tồn tại hoặc đã thanh toán.");
        orderDetailDraftRepository.deleteById(detailId);
        updateTotalAmount(orderId);
        Double total = orderDraftRepository.getTotalAmount(orderId);
        return new Result<>(true, total, "Đã xóa sản khỏi đơn chờ");
    }

    @Transactional
    public Result<?> changeQuantity(Long detailId, Integer quantity) {
        if (quantity == null || quantity < 1) return Result.fail("Số lượng phải là số nguyên lớn hơn 0.");
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findById(detailId);
        if(orderDetailDraft.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy chi tiết đơn hàng chờ");
        }
        if (orderDraftRepository.findByIdForUpdate(orderDetailDraft.get().getOrderDraft().getId()).isEmpty())
            return Result.fail("Đơn chờ không tồn tại hoặc đã thanh toán.");
        ProductDetail variant = productDetailRepository.findById(orderDetailDraft.get().getIdProductDetail()).orElse(null);
        if (variant == null || !Boolean.TRUE.equals(variant.getActive()) || variant.getStock() == null || quantity > variant.getStock())
            return Result.fail("Biến thể ngừng bán hoặc số lượng vượt tồn kho hiện tại.");
        orderDetailDraft.get().setQuantity(quantity);
        orderDetailDraftRepository.save(orderDetailDraft.get());
        updateTotalAmount(orderDetailDraft.get().getOrderDraft().getId());
        Double total = orderDraftRepository.getTotalAmount(orderDetailDraft.get().getOrderDraft().getId());
        return new Result<>(true, total, "Đã cập nhật số lượng");
    }

    public Result<?> findOrderById(Long orderId) {
        Optional<OrderDraft> orderDraft = orderDraftRepository.findById(orderId);
        if(orderDraft.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy đơn chờ");
        }
        return new Result<>(true, orderDraft.get(), "");
    }

    public void updateTotalAmount(Long orderId){
        Double total = orderDetailDraftRepository.totalByOrderId(orderId);
        orderDraftRepository.updateTotalAmount(total, orderId);
    }

    @Transactional
    public Result<?> deleteOrder(Long orderId){
        if (orderDraftRepository.findByIdForUpdate(orderId).isEmpty()) return Result.fail("Đơn chờ không tồn tại hoặc đã thanh toán.");
        orderDetailDraftRepository.deleteByOrder(orderId);
        orderDraftRepository.deleteById(orderId);
        return new Result<>(true, null, "Xóa đơn tạm thành công");
    }

    @Transactional
    public Result<?> changeToInvoice(Long orderId, InvoiceType invoiceType, InvoiceStatus invoiceStatus){
        if ((invoiceType != InvoiceType.TAI_QUAY && invoiceType != InvoiceType.FACEBOOK)
                || (invoiceStatus != InvoiceStatus.DANG_CHO_VAN_CHUYEN && invoiceStatus != InvoiceStatus.HOAN_THANH))
            return Result.fail("Loại giao dịch hoặc trạng thái khởi tạo không hợp lệ.");
        Optional<OrderDraft> orderDraftOptional = orderDraftRepository.findByIdForUpdate(orderId);
        if(orderDraftOptional.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy đơn hàng chờ nào");
        }
        List<OrderDetailDraft> orderDetailDrafts = orderDetailDraftRepository.findByOrder(orderId);
        if(orderDetailDrafts.size() == 0){
            return new Result<>(false, null, "Chưa có sản phẩm nào trong đơn chờ này");
        }
        double merchandiseTotal = 0D;
        for (OrderDetailDraft line : orderDetailDrafts) {
            if (line.getQuantity() == null || line.getQuantity() < 1 || line.getPrice() == null || !Double.isFinite(line.getPrice()) || line.getPrice() < 0)
                return Result.fail("Giỏ hàng có số lượng hoặc giá không hợp lệ. Vui lòng kiểm tra lại.");
            merchandiseTotal += line.getPrice() * line.getQuantity();
        }
        Double discount = orderDraftOptional.get().getDiscount();
        Double shipping = orderDraftOptional.get().getShipCost();
        if (!Double.isFinite(merchandiseTotal) || discount == null || !Double.isFinite(discount) || discount < 0 || discount > merchandiseTotal
                || shipping == null || !Double.isFinite(shipping) || shipping < 0)
            return Result.fail("Tiền giảm giá hoặc phí vận chuyển không hợp lệ.");
        orderDraftOptional.get().setTotalAmount(merchandiseTotal);
        Float weight = 0F;
        for(OrderDetailDraft o : orderDetailDrafts){
            weight += (o.getWeight() == null ? 0F : o.getWeight()) * o.getQuantity();
        }
        Invoice invoice = invoiceMapper.orderToInvoice(orderDraftOptional.get());
        List<InvoiceDetail> list = invoiceMapper.orderDetailToInvoiceDetail(orderDetailDrafts);
        invoice.setCreatedDate(LocalDateTime.now());
        invoice.setUpdateDate(LocalDateTime.now());
        invoice.setInvoiceType(invoiceType);
        invoice.setInvoiceStatus(invoiceStatus);
        invoice.setCustomerId(orderDraftOptional.get().getCustomerId());
        if (invoiceStatus == InvoiceStatus.HOAN_THANH) invoice.setCompletedDate(LocalDateTime.now());
        invoice.setPayType(PayType.COD);
        invoice.setWeight(weight);
        invoice.setShipCost(shipping);
        invoice.setDiscount(orderDraftOptional.get().getDiscount());
        invoiceRepository.save(invoice);

        for (InvoiceDetail i : list) {
            // Lock row ProductDetail
            ProductDetail product = productDetailRepository.findByIdForUpdate(i.getIdProductDetail())
                    .orElseThrow(() -> new RuntimeException("Không tồn tại sản phẩm: " + i.getProductName()));
            Product parent = productRepository.findById(product.getProductId()).orElse(null);
            if (!Boolean.TRUE.equals(product.getActive()) || parent == null || !Boolean.TRUE.equals(parent.getActive())) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.fail("Sản phẩm hoặc biến thể đã ngừng kinh doanh. Vui lòng kiểm tra lại giỏ.");
            }
            if (product.getStock() == null || product.getStock() < i.getQuantity()) {
                // roleback lại sẽ xóa invoice tạo bên trên
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                if(product.getStock() == null || product.getStock() == 0){
                    return new Result<>(false,null,"Xin lỗi bạn! "+i.getProductName()+" màu: "+i.getColorName()+" - size: "+i.getSizeName()+" đã hết hàng");
                }
                else{
                    return new Result<>(false,null,"Xin lỗi bạn! "+i.getProductName()+" màu: "+i.getColorName()+" - size: "+i.getSizeName()+" chỉ còn "+product.getStock()+" sản phẩm");
                }
            }
            i.setIdInvoice(invoice.getId());
            // Trừ stock
            product.setStock(product.getStock() - i.getQuantity());
            productDetailRepository.save(product);
            // Tạo invoice detail
        }
        invoiceDetailRepository.saveAll(list);

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 3).toUpperCase(); // 3 ký tự
        invoice.setCode("INV-" + date + "-" + random + "-" + invoice.getId());
        invoice.setToken(UUID.randomUUID().toString()+"-"+invoice.getId().toString());
        invoiceRepository.save(invoice);

        // Consume the draft in the same transaction as invoice and stock writes.
        // A concurrent/retried request cannot create a second invoice from it.
        // Remove managed child entities as entities, not via bulk JPQL; otherwise
        // Hibernate still sees children referencing a removed draft at flush.
        orderDetailDraftRepository.deleteAll(orderDetailDrafts);
        orderDetailDraftRepository.flush();
        orderDraftRepository.delete(orderDraftOptional.get());

        emailInvoice.buildOrderSuccessEmail(invoice, list);
        HistoryInvoiceStatus historyInvoiceStatus = new HistoryInvoiceStatus(null, invoice, invoice.getInvoiceStatus(), LocalDateTime.now(), "",invoice.getUserId(),"","","");
        historyInvoiceStatusRepository.save(historyInvoiceStatus);
        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        teleSend.sendMessage("✅ Có đơn hàng mới tại quầy\nMã đơn hàng: "+invoice.getCode()+"\nTổng tiền: "+vnFormat.format(invoice.getTotalAmount())+"\n"
                +"Phí ship: "+vnFormat.format(invoice.getShipCost())+"\nGiảm giá: "+vnFormat.format(invoice.getDiscount()));

        if(invoiceStatus.equals(InvoiceStatus.DANG_CHO_VAN_CHUYEN)){
            messagingTemplate.convertAndSend("/topic/order", invoiceRepository.donHangMoi());
            List<OrderStatusCount> listStt = new ArrayList<>();
            listStt.add(new OrderStatusCount(invoice.getInvoiceStatus(), invoiceRepository.countByStatus(invoice.getInvoiceStatus())));
            messagingTemplate.convertAndSend("/topic/count-order", listStt);
        }

        return new Result<>(true, orderId, "Đã chuyển sang đơn hàng chính thức");
    }
}
