package com.web.dto;

import com.web.enums.DiscountProductType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateDiscountProductRequest {

    private DiscountProductType discountProductType;

    private Double discountValue;

    private Double maxDiscountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

}