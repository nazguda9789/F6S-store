package com.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO doanh thu theo danh mục sản phẩm
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRevenueDTO {
    private Long categoryId;
    private String categoryName;
    private Double totalRevenue;
    private Long totalQuantity;
    private Double percentage; // % trong tổng doanh thu
}