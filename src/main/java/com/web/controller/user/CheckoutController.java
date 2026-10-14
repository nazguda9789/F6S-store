package com.web.controller.user;

import com.web.dto.CartResponse;
import com.web.dto.Result;
import com.web.entity.Invoice;
import com.web.entity.InvoiceDetail;
import com.web.entity.User;
import com.web.ghn.GhnClient;
import com.web.repository.VoucherRepository;
import com.web.service.CartService;
import com.web.service.InvoiceService;
import com.web.service.LocationService;
import com.web.utils.TypeSystem;
import com.web.utils.UserUtils;
import com.web.vnpay.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private VNPayService vnPayService;

    @Value("${shipping.cost}")
    Double shippingCost;

    @Autowired
    private GhnClient ghnClient;

    @GetMapping({"/checkout"})
    public String checkoutView(Model model, HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes,
                               @RequestParam(value = "detail", required = false) List<Long> detailIds ){
        // ĐÃ SỬA: Kiểm tra an toàn cho tham số detail tránh sập trang lỗi 400
        if (detailIds == null || detailIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Không tìm thấy thông tin sản phẩm cần thanh toán.");
            return "redirect:/cart";
        }

        CartResponse cartResponse = cartService.myCartCheckout(session, detailIds);
        if(cartResponse.getCartDetails().isEmpty()){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Bạn chưa có sản phẩm nào trong giỏ hàng");
            return "redirect:/cart";
        }
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("cartResponse", cartResponse);
        model.addAttribute("provinces", locationService.getAllProvincesWithWards());
        User user = userUtils.getUserWithAuthority();
        if(user != null){
            model.addAttribute("userCheckout", user);
        }
        model.addAttribute("voucherAvailable", voucherRepository.availableCheckout(cartResponse.getTotalAmount()));
        model.addAttribute("voucherNotAvailable", voucherRepository.notAvailableCheckout(cartResponse.getTotalAmount()));
        Float weight = cartService.getWeightCart(session, cartResponse);
        int roundedWeight = (int) Math.ceil(weight);
        model.addAttribute("weight", weight);
        model.addAttribute("roundedWeight", roundedWeight);
        Map<String, Object> province = ghnClient.getProvince();
        model.addAttribute("province", province);

        return "user/checkout";
    }

    @GetMapping({"/success-order/{code}"})
    public String checkoutPost(HttpServletRequest request, HttpSession session, Model model, RedirectAttributes redirectAttributesr,
                               @PathVariable String code, @RequestParam String token){
        Result<Invoice> result = invoiceService.findInvoiceByCodeAndToken(code,token);
        if(result.isSuccess()){
            model.addAttribute("isCheck", true);
            model.addAttribute("invoice", result.getData());
            List<InvoiceDetail> invoiceDetails = invoiceService.getDetailByInvoiceId(result.getData().getId());
            model.addAttribute("invoiceDetails", invoiceDetails);
        }
        else{
            return "redirect:/error";
        }
        return "user/response-order";
    }

    @PostMapping({"/checkout"})
    public String checkoutPost(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes,
                               @RequestParam String fullName, @RequestParam String email, @RequestParam String phone, @RequestParam String provinceName,
                               @RequestParam String districtName, @RequestParam String wardName, @RequestParam String addressDetail,
                               @RequestParam String voucher, @RequestParam Double phiShip, @RequestParam String note,
                               @RequestParam(value = "detail", required = false) List<Long> detailIds ){

        // ĐÃ SỬA: Kiểm tra an toàn cho tham số detail khi Submit Form COD
        if (detailIds == null || detailIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Lỗi hệ thống: Danh sách sản phẩm thanh toán trống (detail).");
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/cart");
        }

        Result<?> result = invoiceService.createInvoice(session, request, fullName, email, phone, provinceName, districtName, wardName, addressDetail, voucher, note, detailIds, phiShip);
        if(result.isSuccess()){
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, result.getMessage());
        }
        else{
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, result.getMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + referer;
        }
        Invoice invoice = (Invoice) result.getData();
        return "redirect:/success-order/"+invoice.getCode()+"?token="+invoice.getToken();
    }

    @PostMapping({"/checkout-vnpay"})
    public String checkoutPostVnpay(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes,
                                    @RequestParam String fullName, @RequestParam String email, @RequestParam String phone, @RequestParam Double phiShip ,
                                    @RequestParam String provinceName,
                                    @RequestParam String districtName, @RequestParam String wardName, @RequestParam String addressDetail,
                                    @RequestParam String voucher, @RequestParam String note,
                                    @RequestParam(value = "detail", required = false) List<Long> detailIds ){

        // ĐÃ SỬA: Kiểm tra an toàn cho tham số detail khi Submit sang VNPay
        if (detailIds == null || detailIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Lỗi hệ thống: Danh sách sản phẩm thanh toán trống (detail).");
            return "redirect:/cart";
        }

        Result<?> result = invoiceService.createInvoiceSession(session, request, fullName, email, phone, provinceName, districtName, wardName, addressDetail, voucher, note, detailIds, phiShip);
        if(!result.isSuccess()){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, result.getMessage());
            return "redirect:/checkout?detail=" + detailIds.toString().replaceAll("[\\[\\] ]", "");
        }
        CartResponse cartResponse = cartService.myCartCheckout(session, detailIds);
        String domain = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        String returnUrl = domain + "/check-vnpay";
        Invoice invoice = (Invoice) result.getData();
        String orderId = String.valueOf(System.currentTimeMillis());
        String url = vnPayService.createOrder(cartResponse.getTotalAmount().intValue() + invoice.getShipCost().intValue() - invoice.getDiscount().intValue(), orderId, returnUrl);
        return "redirect:"+url;
    }

    @GetMapping({"/check-vnpay"})
    public String checkVnpay(@RequestParam("vnp_OrderInfo") String vnpOrderInfo, HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes){
        Result<?> result = invoiceService.checkVnpay(request, session, vnpOrderInfo);
        if(result.isSuccess()){
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, result.getMessage());
        }
        else{
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, result.getMessage());
            return "redirect:/cart";
        }
        Invoice invoice = (Invoice) result.getData();
        return "redirect:/success-order/"+invoice.getCode()+"?token="+invoice.getToken();
    }
}