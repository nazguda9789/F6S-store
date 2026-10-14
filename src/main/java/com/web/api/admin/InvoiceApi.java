package com.web.api.admin;

import com.web.dto.HistoryResponse;
import com.web.dto.InvoiceRequest;
import com.web.dto.ProductSearchAdmin;
import com.web.dto.Result;
import com.web.entity.Invoice;
import com.web.entity.Product;
import com.web.enums.InvoiceStatus;
import com.web.service.InvoiceService;
import com.web.exception.MessageException;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/invoice")
public class InvoiceApi {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/filter")
    public ResponseEntity<?> filter(@RequestBody InvoiceRequest invoiceRequest, Pageable pageable){
        Page<Invoice> page = invoiceService.searchFull(invoiceRequest, pageable);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @PostMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam Long id){
        HistoryResponse result = invoiceService.getHistoryById(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestParam Long id, @RequestParam InvoiceStatus invoiceStatus){
        try{
            Result<?> result = invoiceService.changeStatus(id, invoiceStatus);
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        catch (MessageException e){
            Result<?> result = new Result<>(false, null, e.getDefaultMessage());
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }
}
