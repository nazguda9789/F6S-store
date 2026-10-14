package com.web.dto;

import com.web.entity.HistoryInvoiceStatus;
import com.web.entity.Invoice;
import com.web.enums.InvoiceStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class HistoryResponse {

    private List<Status> statuses = new ArrayList<>();

    private Invoice invoice;

    private List<HistoryInvoiceStatus> historyInvoiceStatuses = new ArrayList<>();

    public HistoryResponse(){
        Arrays.stream(InvoiceStatus.values()).forEach(p->{
            Status s = new Status();
            s.setInvoiceStatus(p);
            statuses.add(s);
        });
    }

    @Data
    public static class Status {

        private InvoiceStatus invoiceStatus;

        private Boolean active = false;

        public String getLabel(){
            return invoiceStatus.getLabel();
        }

        public String getColor(){
            return invoiceStatus.getColor();
        }

        public String getIcon(){
            return invoiceStatus.getIcon();
        }

    }

    public void setActiveStatus(){
        for(Status s : statuses){
            for(HistoryInvoiceStatus h : historyInvoiceStatuses){
                if(s.getInvoiceStatus().equals(h.getInvoiceStatus())){
                    s.setActive(true);
                    break;
                }
            }
        }
    }
}
