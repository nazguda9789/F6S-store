package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_detail_draft")
@Getter
@Setter
public class OrderDetailDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private String productImage;

    private String colorName;

    private String sizeName;

    private String sku;

    private Float weight;

    @ManyToOne
    private OrderDraft orderDraft;

    private Long idProductDetail;

    private Double price;

    private Integer quantity;
}
