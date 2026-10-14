package com.web.controller.admin;

import com.web.dto.CustomerSummary;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.service.ContactService;
import com.web.service.InvoiceService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/customer")
public class CustomerAdminController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/list")
    public String listCustomers(Model model,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) InvoiceStatus status,
                                @RequestParam(required = false) InvoiceType type,
                                @RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                                @RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
                                @RequestParam(defaultValue = "10") Integer size,
                                Pageable pageable) {

        if (pageable == null) {
            pageable = PageRequest.of(0, 20);
        }

        Page<CustomerSummary> page = invoiceService.getCustomers(search, status, type, from, to, pageable);

        model.addAttribute("page", page);
        model.addAttribute("listCustomer", page.getContent());

        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        return "admin/customer"; // file templates/admin/customer/list.html
    }
}
