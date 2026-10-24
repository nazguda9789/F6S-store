package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.bad;

import com.example.sneaker.dto.DashboardApi.*;
import com.example.sneaker.repository.OrderRepository;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
  private final OrderRepository orders;
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  public Dashboard report(LocalDate from, LocalDate to, String group) {
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365)
      throw bad("Khoảng thống kê tối đa 366 ngày");
    if (!group.equals("day") && !group.equals("month"))
      throw bad("Nhóm thống kê phải là day hoặc month");
    Instant start = from.atStartOfDay(ZONE).toInstant(),
        end = to.plusDays(1).atStartOfDay(ZONE).toInstant();
    var revenue = orders.revenue(start, end);
    long total = orders.total(start, end), successful = orders.successful(start, end);
    BigDecimal rate =
        total == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(successful)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    var buckets = new ArrayList<Bucket>();
    LocalDate cursor = from;
    while (!cursor.isAfter(to)) {
      LocalDate next =
          group.equals("day") ? cursor.plusDays(1) : cursor.withDayOfMonth(1).plusMonths(1);
      if (next.isAfter(to.plusDays(1))) next = to.plusDays(1);
      var row =
          orders.revenue(
              cursor.atStartOfDay(ZONE).toInstant(), next.atStartOfDay(ZONE).toInstant());
      buckets.add(
          new Bucket(
              group.equals("day") ? cursor.toString() : YearMonth.from(cursor).toString(),
              row.getRevenue(),
              row.getCompletedOrders()));
      cursor = next;
    }
    return new Dashboard(
        from,
        to,
        ZONE.toString(),
        revenue.getRevenue(),
        total,
        successful,
        rate,
        buckets,
        orders.top(start, end, PageRequest.of(0, 10)).stream()
            .map(
                t ->
                    new TopProduct(
                        t.getProductId(),
                        t.getCode(),
                        t.getName(),
                        t.getQuantity(),
                        t.getGrossRevenue()))
            .toList());
  }
}
