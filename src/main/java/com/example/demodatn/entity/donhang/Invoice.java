package com.example.demodatn.entity.donhang;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    @Column(name = "invoice_code", nullable = false, unique = true)
    private String invoiceCode;
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    @Column(name = "customer_name", nullable = false)
    @org.hibernate.annotations.Nationalized private String customerName;
    @Column(name = "customer_phone")
    @org.hibernate.annotations.Nationalized private String customerPhone;
    @Column(name = "employee_name")
    @org.hibernate.annotations.Nationalized private String employeeName;
    @Column(name = "cash_received")
    private BigDecimal cashReceived;
    @Column(name = "change_amount")
    private BigDecimal changeAmount;
}
