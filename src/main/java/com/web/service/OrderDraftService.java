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

    public Result<?> create(OrderDraft orderDraft){
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

    public Result<?> addToOrder(Long detailId, Long orderId) {
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findByDetailAndOrderId(detailId, orderId);
        if(orderDetailDraft.isPresent()){
            return new Result<>(false, null, "Đã tồn tại sản phẩm này trong đơn hàng chờ "+orderDetailDraft.get().getOrderDraft().getId());
        }
        OrderDraft orderDraft = new OrderDraft();
        orderDraft.setId(orderId);
        ProductDetail productDetail = productDetailRepository.findById(detailId).get();
        Float weight = productRepository.getWeightById(productDetail.getProductId());
        String image = productColorImageRepository.getImageByProductAndColor(productDetail.getProductId(), productDetail.getColor().getId());
        if(image == null){
            image = productRepository.getImageById(productDetail.getProductId());
        }
        OrderDetailDraft orderDetailDraft1 = new OrderDetailDraft();
        orderDetailDraft1.setIdProductDetail(detailId);
        orderDetailDraft1.setOrderDraft(orderDraft);
        orderDetailDraft1.setWeight(weight);
        orderDetailDraft1.setPrice(productDetail.getPrice());
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

    public Result<?> deleteDetail(Long detailId) {
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findById(detailId);
        if(orderDetailDraft.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy chi tiết đơn chờ");
        }
        Long orderId = orderDetailDraft.get().getOrderDraft().getId();
        orderDetailDraftRepository.deleteById(detailId);
        updateTotalAmount(orderId);
        Double total = orderDraftRepository.getTotalAmount(orderId);
        return new Result<>(true, total, "Đã xóa sản khỏi đơn chờ");
    }

    public Result<?> changeQuantity(Long detailId, Integer quantity) {
        Optional<OrderDetailDraft> orderDetailDraft = orderDetailDraftRepository.findById(detailId);
        if(orderDetailDraft.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy chi tiết đơn hàng chờ");
        }
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

    public Result<?> deleteOrder(Long orderId){
        orderDetailDraftRepository.deleteByOrder(orderId);
        orderDraftRepository.deleteById(orderId);
        return new Result<>(true, null, "Xóa đơn tạm thành công");
    }

    @Transactional
    public Result<?> changeToInvoice(Long orderId, InvoiceType invoiceType, InvoiceStatus invoiceStatus){
        Optional<OrderDraft> orderDraftOptional = orderDraftRepository.findById(orderId);
        if(orderDraftOptional.isEmpty()){
            return new Result<>(false, null, "Không tìm thấy đơn hàng chờ nào");
        }
        List<OrderDetailDraft> orderDetailDrafts = orderDetailDraftRepository.findByOrder(orderId);
        if(orderDetailDrafts.size() == 0){
            return new Result<>(false, null, "Chưa có sản phẩm nào trong đơn chờ này");
        }
        Float weight = 0F;
        for(OrderDetailDraft o : orderDetailDrafts){
            weight += o.getWeight() * o.getQuantity();
        }
        Invoice invoice = invoiceMapper.orderToInvoice(orderDraftOptional.get());
        List<InvoiceDetail> list = invoiceMapper.orderDetailToInvoiceDetail(orderDetailDrafts);
        invoice.setCreatedDate(LocalDateTime.now());
        invoice.setUpdateDate(LocalDateTime.now());
        invoice.setInvoiceType(invoiceType);
        invoice.setInvoiceStatus(invoiceStatus);
        invoice.setPayType(PayType.COD);
        invoice.setWeight(weight);
        invoice.setShipCost(0D);
        invoice.setDiscount(orderDraftOptional.get().getDiscount());
        invoiceRepository.save(invoice);

        for (InvoiceDetail i : list) {
            // Lock row ProductDetail
            ProductDetail product = productDetailRepository.findByIdForUpdate(i.getIdProductDetail())
                    .orElseThrow(() -> new RuntimeException("Không tồn tại sản phẩm: " + i.getProductName()));
            if (product.getStock() < i.getQuantity()) {
                // roleback lại sẽ xóa invoice tạo bên trên
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                if(product.getStock() == 0){
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
