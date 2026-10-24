package com.web.controller.employee;

import com.web.dto.Result;
import com.web.dto.StatusDto;
import com.web.entity.Invoice;
import com.web.entity.InvoiceDetail;
import com.web.repository.OrderDraftRepository;
import com.web.service.InvoiceService;
import com.web.service.LocationService;
import com.web.service.VoucherService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping(TypeSystem.EMPLOYEE_URL+ "/invoice")
public class InvoiceEmployeeController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private OrderDraftRepository orderDraftRepository;

    @GetMapping("/list")
    public String viewPage(Model model){
        List<StatusDto> statusDtos = invoiceService.toDtoList();
        Long total = 0L;
        for(StatusDto s : statusDtos){
            total += s.getQuantity() == null ? 0 : s.getQuantity();
        }
        model.addAttribute("listStatus", statusDtos);
        model.addAttribute("totalInvoice", total);
        model.addAttribute("isInvoicePage", true);
        return "employee/invoice";
    }

    @GetMapping("/detail")
    public String viewPage(Model model, @RequestParam Long id){
        Result<Invoice> result = invoiceService.findById(id);
        if(result.isSuccess() == true){
            model.addAttribute("invoice", result.getData());
            List<InvoiceDetail> invoiceDetails = invoiceService.getDetailByInvoiceId(result.getData().getId());
            model.addAttribute("invoiceDetails", invoiceDetails);
            model.addAttribute("invoiceHistory", invoiceService.getHistoryById(id).getHistoryInvoiceStatuses());
        }
        else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found");
        }
        model.addAttribute("isInvoicePage", true);
        model.addAttribute("employeeView",true);
        return "admin/invoice-detail";
    }

    @GetMapping("/create")
    public String viewCreatePage(Model model){
        model.addAttribute("isInvoicePage", true);
        model.addAttribute("provinces", locationService.getAllProvincesWithWards());
        model.addAttribute("vouchers", voucherService.available());
        model.addAttribute("orderDrafts", orderDraftRepository.findAll());

        model.addAttribute("employeeView",true);
        return "admin/create-invoice";
    }
}
