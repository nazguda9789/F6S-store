package com.web.api.admin;

import com.web.dto.Result;
import com.web.entity.OrderDetailDraft;
import com.web.entity.OrderDraft;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.service.OrderDraftService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/order-draft")
public class OrderDraftApi {

    @Autowired
    private OrderDraftService orderDraftService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody OrderDraft orderDraft) {
        Result<?> result = orderDraftService.create(orderDraft);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/add-to-order")
    public ResponseEntity<?> addToOrder(@RequestParam Long detailId, @RequestParam Long orderId) {
        Result<?> result = orderDraftService.addToOrder(detailId, orderId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/find-detail-by-order")
    public ResponseEntity<?> findDetailByOrder(@RequestParam Long orderId) {
        List<OrderDetailDraft> result = orderDraftService.findDetailByOrder(orderId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/find-detail-order")
    public ResponseEntity<?> detailOrder(@RequestParam Long orderId) {
        Result<?> result = orderDraftService.findOrderById(orderId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/change-quantity")
    public ResponseEntity<?> changeQuantity(@RequestParam Long detailId, @RequestParam Integer quantity) {
        Result<?> result = orderDraftService.changeQuantity(detailId, quantity);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete-detail")
    public ResponseEntity<?> deleteDetail(@RequestParam Long detailId) {
        Result<?> result = orderDraftService.deleteDetail(detailId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete-order")
    public ResponseEntity<?> deleteOrder(@RequestParam Long orderId) {
        Result<?> result = orderDraftService.deleteOrder(orderId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/save-order-to-invoice")
    public ResponseEntity<?> saveToInvoice(@RequestParam Long orderId, @RequestParam InvoiceType invoiceType,
                                           @RequestParam InvoiceStatus invoiceStatus) {
        Result<?> result = orderDraftService.changeToInvoice(orderId, invoiceType, invoiceStatus);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
