package com.web.service;

import com.web.dto.*;
import com.web.enums.InvoiceStatus;
import com.web.enums.Role;
import com.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service thống kê nâng cao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {

    private final InvoiceRepository invoiceRepository;
    private final HistoryInvoiceStatusRepository historyInvoiceStatusRepository;
    private final UserRepository userRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;

    /**
     * Lấy toàn bộ dữ liệu dashboard thống kê
     */
    @Transactional(readOnly = true)
    public StatisticDashboardDTO getDashboardData(int year) {
        StatisticDashboardDTO dto = new StatisticDashboardDTO();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // === Tổng quan đơn hàng ===
        dto.setTotalOrders(invoiceRepository.count());
        dto.setCompletedOrders(invoiceRepository.countByStatus(InvoiceStatus.HOAN_THANH));
        dto.setCancelledOrders(invoiceRepository.countByStatus(InvoiceStatus.DA_HUY));
        dto.setPendingOrders(invoiceRepository.countByStatus(InvoiceStatus.DANG_CHO_VAN_CHUYEN));

        // === Doanh thu ===
        dto.setRevenueToday(
                safeDouble(historyInvoiceStatusRepository.doanhThuHomNay(
                        InvoiceStatus.HOAN_THANH, startOfDay, endOfDay)));

        dto.setRevenueThisMonth(
                safeDouble(historyInvoiceStatusRepository.doanhThuThang(
                        today.getMonthValue(), today.getYear(), InvoiceStatus.HOAN_THANH)));

        dto.setRevenueThisYear(calculateYearRevenue(year));

        // === Khách hàng & Sản phẩm ===
        dto.setTotalCustomers(userRepository.countByRole(Role.ROLE_USER).longValue());
        dto.setTotalProducts(productDetailRepository.totalProduct().longValue());
        dto.setNewCustomersToday(countNewCustomersToday(startOfDay, endOfDay));

        // === Doanh thu 12 tháng ===
        dto.setRevenueByMonth(getRevenueByMonth(year));

        // === Top 10 sản phẩm bán chạy tháng này ===
        dto.setTopSellingProducts(invoiceRepository.findTop10SellingProductsThisMonth());

        // === Doanh thu theo danh mục ===
        dto.setRevenueByCategory(getRevenueByCategory(today.getMonthValue(), today.getYear()));

        // === Trạng thái đơn hàng ===
        dto.setOrderStatusSummary(getOrderStatusSummary());

        // === Tăng trưởng so với tháng trước ===
        dto.setRevenueGrowthRate(calculateRevenueGrowthRate(today));
        dto.setOrderGrowthRate(calculateOrderGrowthRate(today));

        // === Doanh thu 30 ngày gần nhất ===
        dto.setDailyRevenueLast30Days(getDailyRevenue30Days());

        return dto;
    }

    /**
     * Doanh thu 12 tháng trong năm
     */
    public List<Double> getRevenueByMonth(int year) {
        List<Double> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Double revenue = historyInvoiceStatusRepository.doanhThuThang(
                    month, year, InvoiceStatus.HOAN_THANH);
            result.add(safeDouble(revenue));
        }
        return result;
    }

    /**
     * Doanh thu theo danh mục
     */
    public List<CategoryRevenueDTO> getRevenueByCategory(int month, int year) {
        try {
            // Native query trả về [categoryId, categoryName, totalRevenue, totalQuantity]
            List<Object[]> rows = invoiceDetailRepository.revenueByCategoryAndMonth(month, year);
            double total = rows.stream()
                    .mapToDouble(r -> r[2] != null ? ((Number) r[2]).doubleValue() : 0)
                    .sum();

            return rows.stream().map(r -> {
                CategoryRevenueDTO cat = new CategoryRevenueDTO();
                cat.setCategoryId(((Number) r[0]).longValue());
                cat.setCategoryName((String) r[1]);
                cat.setTotalRevenue(r[2] != null ? ((Number) r[2]).doubleValue() : 0);
                cat.setTotalQuantity(r[3] != null ? ((Number) r[3]).longValue() : 0);
                cat.setPercentage(total > 0 ? Math.round(cat.getTotalRevenue() / total * 10000.0) / 100.0 : 0);
                return cat;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getRevenueByCategory error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Doanh thu 30 ngày gần nhất (cho biểu đồ line chart)
     */
    public List<DailyRevenueDTO> getDailyRevenue30Days() {
        try {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            LocalDateTime to = LocalDateTime.now();
            List<Object[]> rows = invoiceRepository.revenueByDay(from, to);

            return rows.stream().map(r -> new DailyRevenueDTO(
                    (String) r[0],
                    r[1] != null ? ((Number) r[1]).doubleValue() : 0,
                    r[2] != null ? ((Number) r[2]).longValue() : 0
            )).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getDailyRevenue30Days error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Thống kê đơn hàng theo từng trạng thái
     */
    public List<OrderStatusCount> getOrderStatusSummary() {
        List<InvoiceStatus> statuses = Arrays.asList(InvoiceStatus.values());
        try {
            List<OrderCountByStatus> list = invoiceRepository.countOrdersByStatuses(statuses);
            List<OrderStatusCount> res = new ArrayList<>();
            for(OrderCountByStatus o : list){
                OrderStatusCount orderStatusCount = new OrderStatusCount(o.getStatus(), o.getOrderCount());
                res.add(orderStatusCount);
            }
            return res;
        } catch (Exception e) {
            log.warn("getOrderStatusSummary error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Tỉ lệ tăng trưởng doanh thu tháng này vs tháng trước (%)
     */
    public Double calculateRevenueGrowthRate(LocalDate today) {
        try {
            int thisMonth = today.getMonthValue();
            int thisYear = today.getYear();
            int lastMonth = thisMonth == 1 ? 12 : thisMonth - 1;
            int lastYear = thisMonth == 1 ? thisYear - 1 : thisYear;

            double thisRevenue = safeDouble(historyInvoiceStatusRepository.doanhThuThang(
                    thisMonth, thisYear, InvoiceStatus.HOAN_THANH));
            double lastRevenue = safeDouble(historyInvoiceStatusRepository.doanhThuThang(
                    lastMonth, lastYear, InvoiceStatus.HOAN_THANH));

            if (lastRevenue == 0) return thisRevenue > 0 ? 100.0 : 0.0;
            return Math.round((thisRevenue - lastRevenue) / lastRevenue * 10000.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Tỉ lệ tăng trưởng đơn hàng tháng này vs tháng trước (%)
     */
    public Double calculateOrderGrowthRate(LocalDate today) {
        try {
            LocalDateTime startThisMonth = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime startLastMonth = startThisMonth.minusMonths(1);
            LocalDateTime endLastMonth = startThisMonth;

            long thisCount = invoiceRepository.findByCreatedDateBetween(startThisMonth, LocalDateTime.now()).size();
            long lastCount = invoiceRepository.findByCreatedDateBetween(startLastMonth, endLastMonth).size();

            if (lastCount == 0) return thisCount > 0 ? 100.0 : 0.0;
            return Math.round((double)(thisCount - lastCount) / lastCount * 10000.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // =================== HELPER ===================

    private double calculateYearRevenue(int year) {
        double total = 0;
        for (int m = 1; m <= 12; m++) {
            total += safeDouble(historyInvoiceStatusRepository.doanhThuThang(
                    m, year, InvoiceStatus.HOAN_THANH));
        }
        return total;
    }

    private long countNewCustomersToday(LocalDateTime from, LocalDateTime to) {
        try {
            return invoiceRepository.findByCreatedDateBetween(from, to)
                    .stream()
                    .map(i -> i.getUserId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}