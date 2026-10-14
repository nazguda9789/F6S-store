package com.web.api.admin;

import com.web.dto.*;
import com.web.enums.InvoiceStatus;
import com.web.repository.HistoryInvoiceStatusRepository;
import com.web.service.StatisticService;
import com.web.utils.TypeSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * API thống kê nâng cao cho admin dashboard
 *
 * Base URL: /admin/api/statistic
 *
 * Endpoints:
 *  GET /revenue-year?year=2025          → Doanh thu 12 tháng (biểu đồ bar/line)
 *  GET /dashboard?year=2025             → Toàn bộ dashboard data
 *  GET /category?month=5&year=2025      → Doanh thu theo danh mục (pie chart)
 *  GET /daily?days=30                   → Doanh thu theo ngày (line chart)
 *  GET /growth                          → Tỉ lệ tăng trưởng tháng này
 *  GET /order-status                    → Phân bố trạng thái đơn hàng
 */
@RestController
@RequestMapping("admin/api/statistic")
@RequiredArgsConstructor
public class StatisticApiV2 {

    private final StatisticService statisticService;
    private final HistoryInvoiceStatusRepository historyInvoiceStatusRepository;

    /**
     * Toàn bộ dữ liệu dashboard (1 request duy nhất)
     */
    @GetMapping("/dashboard")
    public StatisticDashboardDTO getDashboard(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return statisticService.getDashboardData(year);
    }


    /**
     * Doanh thu theo danh mục - dùng cho pie/donut chart
     */
    @GetMapping("/category")
    public List<CategoryRevenueDTO> doanhThuDanhMuc(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return statisticService.getRevenueByCategory(month, year);
    }

    /**
     * Doanh thu 30 ngày gần nhất - dùng cho line chart
     */
    @GetMapping("/daily")
    public List<DailyRevenueDTO> doanhThuNgay(
            @RequestParam(defaultValue = "30") int days) {
        // days param dùng để mở rộng sau, hiện tại luôn trả 30 ngày
        return statisticService.getDailyRevenue30Days();
    }

    /**
     * Tỉ lệ tăng trưởng tháng này vs tháng trước
     */
    @GetMapping("/growth")
    public GrowthRateDTO getTangTruong() {
        LocalDate today = LocalDate.now();
        GrowthRateDTO dto = new GrowthRateDTO();
        dto.setRevenueGrowthRate(statisticService.calculateRevenueGrowthRate(today));
        dto.setOrderGrowthRate(statisticService.calculateOrderGrowthRate(today));
        return dto;
    }

    /**
     * Phân bố trạng thái đơn hàng - dùng cho pie chart
     */
    @GetMapping("/order-status")
    public List<OrderStatusCount> trangThaiDonHang() {
        return statisticService.getOrderStatusSummary();
    }

    // === Inner DTO ===
    public static class GrowthRateDTO {
        private Double revenueGrowthRate;
        private Double orderGrowthRate;
        public Double getRevenueGrowthRate() { return revenueGrowthRate; }
        public void setRevenueGrowthRate(Double v) { this.revenueGrowthRate = v; }
        public Double getOrderGrowthRate() { return orderGrowthRate; }
        public void setOrderGrowthRate(Double v) { this.orderGrowthRate = v; }
    }
}