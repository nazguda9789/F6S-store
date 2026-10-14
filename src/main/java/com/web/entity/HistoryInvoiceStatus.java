package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "history_invoice_status")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
public class HistoryInvoiceStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus invoiceStatus;

    @CreatedDate
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime createdDate;

    private String createdBy;

    private Long userId;

    private String fullName;

    private String email;

    private String note;

    public String getLabel(){
        return invoiceStatus.getLabel();
    }

    public String getColor(){
        return invoiceStatus.getColor();
    }
}
