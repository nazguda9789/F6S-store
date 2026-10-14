package com.web.mapper;

import com.web.entity.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceMapper {

    @Autowired
    private ModelMapper mapper;

    public Invoice orderToInvoice(OrderDraft orderDraft){
        Invoice invoice = mapper.map(orderDraft, Invoice.class);
        invoice.setId(null);
        invoice.setUserId(orderDraft.getCreatedBy().getId());
        return invoice;
    }


    public List<InvoiceDetail> orderDetailToInvoiceDetail(List<OrderDetailDraft> orderDetailDrafts){
        return orderDetailDrafts.stream()
                .map(draft -> {
                    InvoiceDetail detail = mapper.map(draft, InvoiceDetail.class);
                    detail.setId(null); // reset ID để Hibernate hiểu là INSERT mới
                    return detail;
                })
                .collect(Collectors.toList());
    }
}
