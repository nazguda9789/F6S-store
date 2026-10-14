package com.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp cho dashboard thống kê nâng cao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticDashboardDTO {

    // === Tổng quan ===
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long pendingOrders;

    private Double revenueToday;
    private Double revenueThisMonth;
    private Double revenueThisYear;

    private Long newCustomersToday;
    private Long totalCustomers;
    private Long totalProducts;

    // === Doanh thu theo tháng (12 tháng) ===
    private List<Double> revenueByMonth;

    // === Top sản phẩm ===
    private List<TopProductDTO> topSellingProducts;

    // === Doanh thu theo danh mục ===
    private List<CategoryRevenueDTO> revenueByCategory;

    // === Tỉ lệ đơn hàng theo trạng thái ===
    private List<OrderStatusCount> orderStatusSummary;

    // === So sánh tháng này vs tháng trước ===
    private Double revenueGrowthRate;   // % tăng trưởng doanh thu
    private Double orderGrowthRate;     // % tăng trưởng đơn hàng

    // === Doanh thu theo ngày (30 ngày gần nhất) ===
    private List<DailyRevenueDTO> dailyRevenueLast30Days;
}