package com.web.dto;

public interface ProductView {

    Long getId();
    String getName();
    String getImage();
    String getSlug();
    Double getPrice();
    Double getOlePrice();

    Double getDiscountAmount();  // số tiền giảm

    Double getFinalPrice();      // giá sau giảm

    Integer getDiscountPercent(); // % giảm


}
