package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_detail")
@Getter
@Setter
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private String productImage;

    private String colorName;

    private String sizeName;

    private String sku;

    private Long idInvoice;

    private Long idProductDetail;

    private Double price;

    private Integer quantity;


}
