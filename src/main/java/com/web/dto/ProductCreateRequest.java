package com.web.dto;
import com.web.entity.Category;
import lombok.Data;

import java.util.List;

@Data
public class ProductCreateRequest {

    private String name;
    private Long brandId;
    private Long soleId;
    private Long materialId;
    private String slug;
    private String image;
    private Double price;
    private Double oldPrice;
    private Float weight;
    private String description;
    private String shortDescription;
    private String metaTitle;
    private String metaDescription;
    private List<Long> categoryIds;
    private Boolean active;

    private List<ProductDetailRequest> details;
    private List<String> imageLinks;

    @Data
    public static class ProductDetailRequest {
        private Long colorId;
        private Long sizeId;
        private Integer stock;
        private Double price;
        private Double oldPrice;
        private String sku;
    }
}
