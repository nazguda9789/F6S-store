package com.web.controller.user;

import com.web.dto.Result;
import com.web.entity.Invoice;
import com.web.entity.InvoiceDetail;
import com.web.service.InvoiceService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class InvoiceUserController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/tra-cuu-don-hang")
    public String findInvoice(Model model, @RequestParam(required = false) String code) {
        if(code != null){
            Result<Invoice> result = invoiceService.findInvoiceByCode(code);
            if(result.isSuccess() == false){
                model.addAttribute(TypeSystem.ERROR,"Invoice not found");
            }
            else{
                model.addAttribute("invoice", result.getData());
                List<InvoiceDetail> invoiceDetails = invoiceService.getDetailByInvoiceId(result.getData().getId());
                model.addAttribute("listDetail", invoiceDetails);
            }
        }
        return "user/find-invoice";
    }
}
