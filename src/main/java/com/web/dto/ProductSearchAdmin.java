package com.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchAdmin {

    private Long idCategory;
    private Long brandId;
    private Double smallPrice;
    private Double largePrice;
    private Boolean active;
    private String search;

}
