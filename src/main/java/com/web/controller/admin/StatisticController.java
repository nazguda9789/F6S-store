package com.web.controller.admin;

import com.web.entity.HistoryInvoiceStatus;
import com.web.enums.InvoiceStatus;
import com.web.enums.Role;
import com.web.repository.*;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/thong-ke")
public class StatisticController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private HistoryInvoiceStatusRepository historyInvoiceStatusRepository;

    @GetMapping("/doanh-thu")
    public String viewPage(Model model){
        Date date = new Date(System.currentTimeMillis());
        String[] str = date.toString().split("-");
        Integer year = Integer.valueOf(str[0]);
        Integer month = Integer.valueOf(str[1]);
        model.addAttribute("numAdmin", userRepository.countByRole(Role.ROLE_ADMIN));
        model.addAttribute("numUser", userRepository.countByRole(Role.ROLE_USER));
        model.addAttribute("totalProduct", productDetailRepository.totalProduct());
        model.addAttribute("doanhThuThangNay", historyInvoiceStatusRepository.doanhThuThang(month, year, InvoiceStatus.HOAN_THANH));
        model.addAttribute("sanPhamBanChay", invoiceRepository.findTop10SellingProductsThisMonth());

        LocalDate localDate = date.toLocalDate();
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        model.addAttribute("soDonHoanThanhHomNay", historyInvoiceStatusRepository.countByStatusAndDate(InvoiceStatus.HOAN_THANH, startOfDay, endOfDay));
        model.addAttribute("doanhThuHomNay", historyInvoiceStatusRepository.doanhThuHomNay(InvoiceStatus.HOAN_THANH, startOfDay, endOfDay));

        return "admin/doanhthu";
    }
}
