package com.web.dto;

import com.web.enums.DiscountProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActiveDiscountResponse {

    private Long id;

    private DiscountProductType type;

    private Double discountValue;

    private Double maxDiscountValue;

    private Double discountAmount;   // số tiền được giảm thực tế

    private Double finalPrice;       // giá sau giảm

    public ActiveDiscountResponse(Long id, DiscountProductType type, Double discountValue, Double maxDiscountValue) {
        this.id = id;
        this.type = type;
        this.discountValue = discountValue;
        this.maxDiscountValue = maxDiscountValue;
    }
}