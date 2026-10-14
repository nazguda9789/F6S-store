package com.web.dto;

import com.web.enums.DiscountProductType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DiscountProductRequest {

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long productId;

    private DiscountProductType type;

    private Double discountValue;

    private Double maxDiscountValue;

}
