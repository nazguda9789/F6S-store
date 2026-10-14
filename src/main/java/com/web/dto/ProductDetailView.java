package com.web.dto;

import com.web.entity.ProductDetail;
import com.web.entity.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductDetailView {

    private Long colorId;

    private String colorCode;

    private String colorName;

    private String image;

    private List<Sizes> sizes = new ArrayList<>();

    @Data
    public static class Sizes {
        private Long detailId;
        private Long sizeId;
        private String sizeName;
        private Integer stock;
        private String sku;
        private Double price;
        private Double oldPrice;
    }
}
