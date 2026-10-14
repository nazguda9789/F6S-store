package com.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO doanh thu theo ngày
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyRevenueDTO {
    private String date;        // "dd/MM"
    private Double revenue;
    private Long orderCount;
}