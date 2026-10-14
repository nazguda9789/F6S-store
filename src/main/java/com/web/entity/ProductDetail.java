package com.web.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_detail")
@Getter
@Setter
public class ProductDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Long productId;

    private Long colorId;

    @ManyToOne
    @JoinColumn(name = "color")
    private Color color;

    private Long sizeId;

    @ManyToOne
    @JoinColumn(name = "size")
    private Size size;

    private Integer stock;

    private Double price;

    private Double oldPrice;

    private String sku;

    private Boolean active = true;
}
