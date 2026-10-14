package com.web.dto;

import com.web.entity.Product;
import com.web.entity.ProductDetail;
import com.web.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailAdmin {

    private Product product;

    private List<ProductImage> productImages;

    private List<ProductDetail> productDetails;
}
