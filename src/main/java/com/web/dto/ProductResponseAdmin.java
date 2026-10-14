package com.web.dto;

import com.web.entity.Product;
import com.web.entity.ProductDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductResponseAdmin {

    private ProductView productView;

    private List<ProductDetail> productDetails = new ArrayList<>();
}
