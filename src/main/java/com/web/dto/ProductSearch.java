package com.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductSearch {

    private List<Long> categoriesId = new ArrayList<>();

    private Double minPrice;

    private Double maxPrice;


}
