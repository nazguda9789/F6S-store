package com.web.dto;


public interface TopProductDTO {
    Long getProductId();
    String getProductName();
    String getProductImage();
    Long getTotalQuantity();
    Double getPrice();
}