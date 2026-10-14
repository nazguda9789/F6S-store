package com.web.dto;

import com.web.entity.ProductDetail;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CartSession {

    private ProductDetail productDetail;

    private Integer quantity;

    private ProductView productView;

    private String colorImage;
}
