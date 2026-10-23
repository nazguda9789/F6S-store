package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import com.web.enums.PayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_draft")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class OrderDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime updateDate;

    private String fullName;

    private Long customerId;

    private String phone;

    private String email;

    private String note;

    private String addressDetail;

    private String wardCode;

    private String wardName;

    private String provinceCode;

    private String provinceName;

    private Double totalAmount = 0D;

    private Double discount = 0D;

    private String voucherCode;

    private Double shipCost = 0D;

    @ManyToOne
    @JoinColumn(updatable = false)
    private User createdBy;

}
