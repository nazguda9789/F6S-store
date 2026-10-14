package com.web.entity;

import com.web.enums.DiscountProductType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "discount_product")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class DiscountProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    private DiscountProductType discountProductType;

    private Double discountValue;

    private Double maxDiscountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @ManyToOne
    private Product product;

    @ManyToOne
    private User user;
}
