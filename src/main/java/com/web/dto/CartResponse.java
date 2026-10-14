package com.web.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CartResponse {

    private Double totalAmount;

    private List<CartDetail> cartDetails = new ArrayList<>();

    @Data
    public static class CartDetail {

        private Long dataId;

        private Long productId;

        private String productName;

        private String image;

        private Double price;

        private String colorName;

        private String sizeName;

        private Integer quantity;

        private String slug;

    }

    public static interface CartDetailInject {

        Long getDataId();
        String getProductName();
        String getColorName();
        String getSizeName();
        Integer getQuantity();
        String getSlug();
        Double getPrice();
        String getImage();
        Long getProductId();

    }

}
