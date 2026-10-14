package com.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartRequest {

    private Long id;

    private Integer quantity = 0;
}
