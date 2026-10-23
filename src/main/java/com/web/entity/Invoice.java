package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.enums.PayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String trackingCode;

    private Float weight;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime updateDate;

    private LocalDateTime completedDate;

    private String fullName;

    private String phone;

    private String email;

    private String note;

    private String addressDetail;

    private String wardCode;

    private String wardName;

    private String provinceCode;

    private String provinceName;

    private String districtName;

    private Double totalAmount;

    private Double discount = 0D;

    private String voucherCode;

    private Double shipCost;

    private String ipAddress;

    private Long userId;

    // Customer identity is separate from the staff member who created the sale.
    private Long customerId;

    private String token;

    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus invoiceStatus;

    @Enumerated(EnumType.STRING)
    private PayType payType;

    public String getLabelStatus(){
        return invoiceStatus.getLabel();
    }

    public String getInvoiceTypeLabel(){
        return invoiceType.getLabel();
    }
}
