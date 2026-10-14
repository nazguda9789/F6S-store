package com.web.service;

import com.web.bot.TeleSend;
import com.web.dto.*;
import com.web.entity.*;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.enums.PayType;
import com.web.exception.MessageException;
import com.web.repository.*;
import com.web.specification.InvoiceSpecification;
import com.web.utils.EmailInvoice;
import com.web.utils.TypeSystem;
import com.web.utils.UserUtils;
import com.web.vnpay.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDetailRepository invoiceDetailRepository;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private EmailInvoice emailInvoice;

    @Autowired
    private HistoryInvoiceStatusRepository historyInvoiceStatusRepository;

    @Autowired
    private TeleSend teleSend;

    @Autowired
    private HistoryPayRepository historyPayRepository;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private ProductRepository productRepository;

    @Value("${shipping.cost}")
    Double shipCost;

    @Value("${amount.freeShipping}")
    Double amountFreeShipping;

    private final SimpMessagingTemplate messagingTemplate;

    public InvoiceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Result<Invoice> createInvoice(HttpSession session, HttpServletRequest request, String fullName, String email, String phone, String province, String districtName,
                                         String ward, String addressDetail, String voucher, String note, List<Long> detailIds, Double phiShip){
        CartResponse cartResponse = cartService.myCartCheckout(session, detailIds);
        Result<?> check = validateCheckoutForm(fullName, email, phone, province, ward, addressDetail, voucher, cartResponse);
        if(!check.isSuccess()){
            return new Result<>(false, null, check.getMessage());
        }

        User user = userUtils.getUserWithAuthority();

        Invoice invoice = new Invoice();
        invoice.setFullName(fullName);
        invoice.setPhone(phone);
        invoice.setAddressDetail(addressDetail);
        invoice.setTotalAmount(cartResponse.getTotalAmount());
        invoice.setCreatedDate(LocalDateTime.now());
        invoice.setNote(note);
        invoice.setPayType(PayType.COD);
        invoice.setWeight(cartService.getWeightCart(session, cartResponse));
        invoice.setWardCode(ward);
        invoice.setEmail(email);
        invoice.setInvoiceType(InvoiceType.ONLINE);
        invoice.setInvoiceStatus(InvoiceStatus.DANG_CHO_VAN_CHUYEN);
        invoice.setWardName(ward);
        invoice.setProvinceName(province);
        invoice.setDistrictName(districtName);
        if(email != null){
            invoice.setEmail(email);
        }
        else{
            if(user != null){
                invoice.setEmail(user.getEmail());
            }
        }
        invoice.setUserId(user == null ? null : user.getId());
        if(phiShip != null){
            invoice.setShipCost(phiShip);
        }
        else{
            invoice.setShipCost(shipCost);
        }
        if(invoice.getTotalAmount() >= amountFreeShipping){
            invoice.setShipCost(0D);
        }
        invoice.setIpAddress(getIp(request));

        if (voucher != null && !voucher.isBlank()) {
            Optional<Voucher> v = voucherRepository.findByCode(voucher);
            if(v.isPresent()){
                invoice.setVoucherCode(v.get().getCode());
                invoice.setDiscount(v.get().getDiscount().doubleValue());
            }
        }

        invoice = invoiceRepository.save(invoice);

        List<InvoiceDetail> invoiceDetails = new ArrayList<>();
        for (CartResponse.CartDetail c : cartResponse.getCartDetails()) {
            ProductDetail product = productDetailRepository.findByIdForUpdate(c.getDataId())
                    .orElseThrow(() -> new RuntimeException("Không tồn tại sản phẩm: " + c.getProductName()));
            if (product.getStock() < c.getQuantity()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                if(product.getStock() == 0){
                    return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" đã hết hàng");
                }
                else {
                    return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" chỉ còn "+product.getStock()+" sản phẩm");
                }
            }

            product.setStock(product.getStock() - c.getQuantity());
            productDetailRepository.save(product);

            InvoiceDetail detail = new InvoiceDetail();
            detail.setIdInvoice(invoice.getId());
            detail.setProductName(c.getProductName());
            detail.setIdProductDetail(product.getId());
            detail.setProductImage(c.getImage());
            detail.setQuantity(c.getQuantity());
            detail.setPrice(c.getPrice());
            detail.setSku(product.getSku());
            detail.setColorName(product.getColor().getName());
            detail.setSizeName(product.getSize().getName());
            invoiceDetails.add(detail);
        }
        invoiceDetailRepository.saveAll(invoiceDetails);

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        invoice.setCode("INV-" + date + "-" + random + "-" + invoice.getId());
        invoice.setToken(UUID.randomUUID().toString()+"-"+invoice.getId().toString());
        invoiceRepository.save(invoice);

        emailInvoice.buildOrderSuccessEmail(invoice, invoiceDetails);
        HistoryInvoiceStatus historyInvoiceStatus = new HistoryInvoiceStatus(null, invoice, invoice.getInvoiceStatus(), LocalDateTime.now(), "",user==null?null:user.getId(),"","","");
        historyInvoiceStatusRepository.save(historyInvoiceStatus);

        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        teleSend.sendMessage("✅ Có đơn hàng mới\nMã đơn hàng: "+invoice.getCode()+"\nTổng tiền: "+vnFormat.format(invoice.getTotalAmount())+"\n"
                +"Phí ship: "+vnFormat.format(invoice.getShipCost())+"\nGiảm giá: "+vnFormat.format(invoice.getDiscount()));

        messagingTemplate.convertAndSend("/topic/order", invoiceRepository.donHangMoi());
        List<OrderStatusCount> list = new ArrayList<>();
        list.add(new OrderStatusCount(invoice.getInvoiceStatus(), invoiceRepository.countByStatus(invoice.getInvoiceStatus())));
        messagingTemplate.convertAndSend("/topic/count-order", list);

        return new Result<>(true, invoice, "Tạo đơn hàng thành công");
    }

    public Result<?> createInvoiceSession(HttpSession session, HttpServletRequest request, String fullName, String email, String phone, String province,String districtName, String ward, String addressDetail, String voucher, String note, List<Long> detailIds,Double phiship) {
        CartResponse cartResponse = cartService.myCartCheckout(session, detailIds);
        Result<?> check = validateCheckoutForm(fullName, email, phone, province, ward, addressDetail, voucher, cartResponse);
        if(!check.isSuccess()){
            return new Result<>(false, null, check.getMessage());
        }

        User user = userUtils.getUserWithAuthority();

        Invoice invoice = new Invoice();
        invoice.setFullName(fullName);
        invoice.setPhone(phone);
        invoice.setAddressDetail(addressDetail);
        invoice.setTotalAmount(cartResponse.getTotalAmount());
        invoice.setCreatedDate(LocalDateTime.now());
        invoice.setNote(note);
        invoice.setPayType(PayType.VNPAY);
        invoice.setWeight(cartService.getWeightCart(session, cartResponse));
        invoice.setProvinceCode(province);
        invoice.setEmail(email);
        invoice.setInvoiceType(InvoiceType.ONLINE);
        invoice.setInvoiceStatus(InvoiceStatus.DANG_CHO_VAN_CHUYEN);
        invoice.setWardName(ward);
        invoice.setProvinceName(province);
        invoice.setDistrictName(districtName);

        if(email != null){
            invoice.setEmail(email);
        }
        else{
            if(user != null){
                invoice.setEmail(user.getEmail());
            }
        }
        invoice.setUserId(user == null ? null : user.getId());
        if(phiship != null){
            invoice.setShipCost(phiship);
        }
        else{
            invoice.setShipCost(shipCost);
        }
        if(invoice.getTotalAmount() >= amountFreeShipping){
            invoice.setShipCost(0D);
        }
        invoice.setIpAddress(getIp(request));

        if (voucher != null && !voucher.isBlank()) {
            Optional<Voucher> v = voucherRepository.findByCode(voucher);
            if(v.isPresent()){
                invoice.setVoucherCode(v.get().getCode());
                invoice.setDiscount(v.get().getDiscount().doubleValue());
            }
        }

        List<InvoiceDetail> invoiceDetails = new ArrayList<>();
        for (CartResponse.CartDetail c : cartResponse.getCartDetails()) {
            ProductDetail product = productDetailRepository.findById(c.getDataId())
                    .orElseThrow(() -> new RuntimeException("Không tồn tại sản phẩm: " + c.getProductName()));
            if (product.getStock() < c.getQuantity()) {
                if(product.getStock() == 0){
                    return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" đã hết hàng");
                }
                else {
                    return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" chỉ còn "+product.getStock()+" sản phẩm");
                }
            }

            InvoiceDetail detail = new InvoiceDetail();
            detail.setIdInvoice(invoice.getId());
            detail.setProductName(c.getProductName());
            // ĐÃ SỬA: Phải lấy gán product.getId() (ID của ProductDetail) thay vì gán nhầm sang product.getProductId()
            detail.setIdProductDetail(product.getId());
            detail.setProductImage(c.getImage());
            detail.setQuantity(c.getQuantity());
            detail.setPrice(c.getPrice());
            detail.setSku(product.getSku());
            detail.setColorName(product.getColor().getName());
            detail.setSizeName(product.getSize().getName());
            invoiceDetails.add(detail);
        }
        session.setAttribute(TypeSystem.invoiceWait, invoice);
        session.setAttribute(TypeSystem.invoiceDetailWait, invoiceDetails);

        return new Result<>(true, invoice, "Tạo đơn hàng chờ thành công");
    }

    @Transactional
    public Result<?> checkVnpay(HttpServletRequest request, HttpSession session, String vnpOrderInfo){
        User user = userUtils.getUserWithAuthority();
        if(historyPayRepository.findByOrderIdAndRequestId(vnpOrderInfo, vnpOrderInfo).isPresent()){
            return new Result<>(false, null, "Đơn hàng đã được thanh toán");
        }
        int paymentStatus = vnPayService.orderReturn(request);
        if(paymentStatus != 1){
            return new Result<>(false, null, "Bạn chưa hoàn thành thanh toán");
        }

        Invoice invoice = (Invoice) session.getAttribute(TypeSystem.invoiceWait);
        List<InvoiceDetail> invoiceDetails = (List<InvoiceDetail>) session.getAttribute(TypeSystem.invoiceDetailWait);

        if (invoice == null || invoiceDetails == null) {
            return new Result<>(false, null, "Thông tin phiên giao dịch đã hết hạn hoặc không tìm thấy đơn hàng chờ");
        }

        invoiceRepository.save(invoice);
        for(InvoiceDetail ivd : invoiceDetails){
            ivd.setIdInvoice(invoice.getId());

            // ĐÃ SỬA: Thêm kiểm tra an toàn orElseThrow() thay vì gọi .get() trực tiếp gây crash ứng dụng
            ProductDetail product = productDetailRepository.findById(ivd.getIdProductDetail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm có ID: " + ivd.getIdProductDetail()));

            if (product.getStock() < ivd.getQuantity()) {
                return new Result<>(false, null, "Sản phẩm " + ivd.getProductName() + " đã hết hàng hoặc không đủ số lượng trong lúc bạn thanh toán.");
            }

            product.setStock(product.getStock() - ivd.getQuantity());
            productDetailRepository.save(product);
        }
        invoiceDetailRepository.saveAll(invoiceDetails);

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        invoice.setCode("INV-" + date + "-" + random + "-" + invoice.getId());
        invoice.setToken(UUID.randomUUID().toString()+"-"+invoice.getId().toString());
        invoiceRepository.save(invoice);

        emailInvoice.buildOrderSuccessEmail(invoice, invoiceDetails);
        HistoryInvoiceStatus historyInvoiceStatus = new HistoryInvoiceStatus(null, invoice, invoice.getInvoiceStatus(), LocalDateTime.now(), "",user==null?null:user.getId(),"","","");
        historyInvoiceStatusRepository.save(historyInvoiceStatus);

        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        teleSend.sendMessage("✅ Có đơn hàng vnpay mới\nMã đơn hàng: "+invoice.getCode()+"\nTổng tiền: "+vnFormat.format(invoice.getTotalAmount())+"\n"
                +"Phí ship: "+vnFormat.format(invoice.getShipCost())+"\nGiảm giá: "+vnFormat.format(invoice.getDiscount()));

        messagingTemplate.convertAndSend("/topic/order", invoiceRepository.donHangMoi());
        List<OrderStatusCount> list = new ArrayList<>();
        list.add(new OrderStatusCount(invoice.getInvoiceStatus(), invoiceRepository.countByStatus(invoice.getInvoiceStatus())));
        messagingTemplate.convertAndSend("/topic/count-order", list);

        // Xóa thuộc tính session sau khi xử lý thành công đơn hàng
        session.removeAttribute(TypeSystem.invoiceWait);
        session.removeAttribute(TypeSystem.invoiceDetailWait);

        return new Result<>(true, invoice, "Tạo đơn hàng thành công");
    }

    public Result<?> validateCheckoutForm(
            String fullName, String email, String phone, String province, String ward, String addressDetail, String voucher, CartResponse cartResponse
    ) {
        if(cartResponse.getCartDetails().isEmpty()){
            return new Result<>(false,null,"Bạn chưa có sản phẩm nào trong giỏ hàng");
        }
        String phoneRegex = "^(03|05|07|08|09)\\d{8}$";
        if (fullName == null || fullName.trim().length() < 2) {
            return new Result<>(false,null,"Vui lòng nhập họ tên hợp lệ");
        }
        if (email != null && !email.trim().isEmpty()) {
            String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
            if (!email.matches(emailRegex)) {
                return new Result<>(false,null,"Email không hợp lệ");
            }
        }
        if (phone == null || !phone.matches(phoneRegex)) {
            return new Result<>(false,null,"Số điện thoại không hợp lệ (phải là 10 số, đầu 03/05/07/08/09)");
        }
        if (addressDetail == null || addressDetail.trim().length() < 10) {
            return new Result<>(false,null,"Địa chỉ quá ngắn, vui lòng nhập đầy đủ");
        }
        if (province == null || province.trim().isEmpty()) {
            return new Result<>(false,null,"Vui lòng chọn tỉnh/thành phố");
        }
        if (ward == null || ward.trim().isEmpty()) {
            return new Result<>(false,null,"Vui lòng chọn xã/phường");
        }

        if(voucher != null && !voucher.isBlank()){
            Optional<Voucher> voucherOptional = voucherRepository.findByCode(voucher);
            if(voucherOptional.isEmpty()){
                return new Result<>(false,null,"Voucher không tồn tại");
            }
            else if(voucherOptional.get().getBlock() != null && voucherOptional.get().getBlock()){
                return new Result<>(false,null,"Voucher đã hết hạn sử dụng");
            }
            else if(cartResponse.getTotalAmount() < voucherOptional.get().getMinAmount()){
                Double missingAmount = voucherOptional.get().getMinAmount() - cartResponse.getTotalAmount();
                NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
                String formattedAmount = vnFormat.format(missingAmount) + " ₫";
                return new Result<>(false, null, "Hãy mua thêm " + formattedAmount + " để sử dụng voucher này");
            }
        }
        for(CartResponse.CartDetail c : cartResponse.getCartDetails()){
            Optional<ProductDetail> optional = productDetailRepository.findById(c.getDataId());
            if(optional.isEmpty()){
                return new Result<>(false,null,"Không tồn tại sản phẩm: "+c.getProductName());
            }
            else if(c.getQuantity() == 0){
                return new Result<>(false,null,c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" phải > 0");
            }
            else if(optional.get().getActive() != null && !optional.get().getActive()){
                return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" đang tạm dừng bán");
            }
            else if(optional.get().getStock() == 0){
                return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" đã hết hàng");
            }
            else if(optional.get().getStock() < c.getQuantity()){
                return new Result<>(false,null,"Xin lỗi bạn! "+c.getProductName()+" màu: "+c.getColorName()+" - size: "+c.getSizeName()+" chỉ còn "+optional.get().getStock()+" sản phẩm");
            }
        }
        return new Result<>(true,null,"Thông tin hợp lệ");
    }

    public String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public Result<Invoice> findInvoiceByCode(String code) {
        Optional<Invoice> invoice = invoiceRepository.findByCode(code);
        if(invoice.isPresent()){
            return new Result<>(true, invoice.get(),"");
        }
        return new Result<>(false, null,"Không tìm thấy đơn hàng nào");
    }

    public List<InvoiceDetail> getDetailByInvoiceId(Long id) {
        return invoiceDetailRepository.findByInvoiceId(id);
    }

    public Result<Invoice> findInvoiceByCodeAndToken(String code, String token) {
        Optional<Invoice> invoice = invoiceRepository.findByCodeAndToken(code, token);
        if(invoice.isPresent()){
            return new Result<>(true, invoice.get(),"");
        }
        return new Result<>(false, null,"Không tìm thấy đơn hàng nào");
    }

    public Result<Invoice> findById(Long id) {
        Optional<Invoice> invoice = invoiceRepository.findById(id);
        if(invoice.isPresent()){
            return new Result<>(true, invoice.get(),"");
        }
        return new Result<>(false, null,"Không tìm thấy đơn hàng nào");
    }

    public List<StatusDto> toDtoList() {
        List<InvoiceStatus> invoiceStatuses = Arrays.stream(InvoiceStatus.values()).toList();
        List<OrderCountByStatus> orderCountByStatuses = invoiceRepository.countOrdersByStatuses(invoiceStatuses);

        return Arrays.stream(InvoiceStatus.values())
                .map(status -> {
                    StatusDto dto = new StatusDto();
                    dto.setName(status.name());
                    dto.setLabel(status.getLabel());
                    dto.setQuantity(0L);
                    for(OrderCountByStatus o : orderCountByStatuses){
                        if(o.getStatus().equals(status)){
                            dto.setQuantity(o.getOrderCount() == null ? 0 : o.getOrderCount());
                            orderCountByStatuses.remove(o);
                            break;
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Page<Invoice> searchFull(InvoiceRequest request, Pageable pageable){
        InvoiceSpecification invoiceSpecification = new InvoiceSpecification(request.getInvoiceStatus(), request.getFrom(), request.getTo(), request.getSearch());
        return invoiceRepository.findAll(invoiceSpecification, pageable);
    }

    public HistoryResponse getHistoryById(Long invoiceId){
        Optional<Invoice> invoice = invoiceRepository.findById(invoiceId);
        if(invoice.isEmpty()){
            return null;
        }
        List<HistoryInvoiceStatus> historyInvoiceStatuses = historyInvoiceStatusRepository.findByInvoice(invoiceId);
        HistoryResponse historyResponse = new HistoryResponse();
        historyResponse.setInvoice(invoice.get());
        historyResponse.setHistoryInvoiceStatuses(historyInvoiceStatuses);
        historyResponse.setActiveStatus();
        return historyResponse;
    }

    @Transactional
    public Result<?> changeStatus(Long id, InvoiceStatus newStatus) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            return new Result<>(false, null, "Không tìm thấy đơn hàng");
        }

        Invoice invoice = invoiceOpt.get();
        InvoiceStatus currentStatus = invoice.getInvoiceStatus();

        Map<InvoiceStatus, List<InvoiceStatus>> available = StatusAvailable.available;
        List<InvoiceStatus> nextStatuses = available.getOrDefault(currentStatus, List.of());

        if (!nextStatuses.contains(newStatus)) {
            String allowed = nextStatuses.isEmpty()
                    ? "Không thể chuyển sang bất kỳ trạng thái nào khác"
                    : "Chỉ có thể chuyển sang: " + nextStatuses.stream().map(Enum::name).toList();
            return new Result<>(false, invoice,
                    "Không thể cập nhật từ " + currentStatus.name() + " sang " + newStatus.name() + ". " + allowed);
        }
        if(newStatus.equals(InvoiceStatus.HOAN_THANH)){
            invoice.setCompletedDate(LocalDateTime.now());
            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceId(id);
            List<Product> products = new ArrayList<>();
            for(InvoiceDetail iv : invoiceDetails){
                Product product = productDetailRepository.findProductById(iv.getIdProductDetail());
                product.setSold(product.getSold() + iv.getQuantity());
                products.add(product);
            }
            productRepository.saveAll(products);
        }
        if(newStatus.equals(InvoiceStatus.YEU_CAU_DOI_HANG)){
            if(invoice.getCompletedDate() != null){
                LocalDateTime now = LocalDateTime.now();
                long daysBetween = ChronoUnit.DAYS.between(invoice.getCompletedDate(), now);
                if (daysBetween > 3) {
                    throw new MessageException("Không thể đổi trả hàng vì đã quá 3 ngày nhận");
                }
            }
        }

        invoice.setInvoiceStatus(newStatus);
        invoiceRepository.save(invoice);

        HistoryInvoiceStatus historyInvoiceStatus = new HistoryInvoiceStatus();
        historyInvoiceStatus.setInvoice(invoice);
        historyInvoiceStatus.setInvoiceStatus(invoice.getInvoiceStatus());
        historyInvoiceStatus.setCreatedDate(LocalDateTime.now());
        User user = userUtils.getUserWithAuthority();
        if (user != null) {
            historyInvoiceStatus.setEmail(user.getEmail());
            historyInvoiceStatus.setFullName(user.getFullName());
            historyInvoiceStatus.setCreatedBy(user.getFullName());
            historyInvoiceStatus.setUserId(user.getId());
        }
        historyInvoiceStatusRepository.save(historyInvoiceStatus);

        messagingTemplate.convertAndSend("/topic/order", invoiceRepository.donHangMoi());
        List<OrderStatusCount> list = new ArrayList<>();
        list.add(new OrderStatusCount(currentStatus, invoiceRepository.countByStatus(currentStatus)));
        list.add(new OrderStatusCount(newStatus, invoiceRepository.countByStatus(newStatus)));
        messagingTemplate.convertAndSend("/topic/count-order", list);

        return new Result<>(true, list, "Đã cập nhật trạng thái đơn hàng");
    }

    public List<Invoice> findByUserId(Long id) {
        return invoiceRepository.findByUserId(id);
    }

    public List<InvoiceUserResponse> findInvoiceByUserId(Long id) {
        List<Invoice> invoices = invoiceRepository.findByUserId(id);

        List<Long> idInvoices = invoices.stream()
                .map(Invoice::getId)
                .toList();

        List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findAllByInvoiceId(idInvoices);

        Map<Long, List<InvoiceDetail>> detailMap = invoiceDetails.stream()
                .collect(Collectors.groupingBy(InvoiceDetail::getIdInvoice));

        List<InvoiceUserResponse> invoiceUserResponses = new ArrayList<>();
        for (Invoice invoice : invoices) {
            InvoiceUserResponse response = new InvoiceUserResponse();
            response.setInvoice(invoice);
            response.setInvoiceDetails(detailMap.getOrDefault(invoice.getId(), new ArrayList<>()));
            invoiceUserResponses.add(response);
        }
        return invoiceUserResponses;
    }

    public Page<CustomerSummary> getCustomers(
            String search, InvoiceStatus status, InvoiceType type, LocalDate from, LocalDate to, Pageable pageable
    ) {
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;
        return invoiceRepository.findCustomerSummary(search, status, type, fromDateTime, toDateTime, pageable);
    }
}