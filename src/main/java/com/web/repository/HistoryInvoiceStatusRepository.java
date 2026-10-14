package com.web.repository;

import com.web.entity.HistoryInvoiceStatus;
import com.web.entity.Invoice;
import com.web.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

public interface HistoryInvoiceStatusRepository extends JpaRepository<HistoryInvoiceStatus, Long> {

    @Query("select h from HistoryInvoiceStatus h where h.invoice.id = ?1")
    List<HistoryInvoiceStatus> findByInvoice(Long invoiceId);

    @Query("SELECT COUNT(h) " +
            "FROM HistoryInvoiceStatus h " +
            "WHERE h.invoiceStatus = :status " +
            "AND h.createdDate >= :startOfDay " +
            "AND h.createdDate < :endOfDay")
    Long countByStatusAndDate(@Param("status") InvoiceStatus status,
                              @Param("startOfDay") LocalDateTime startOfDay,
                              @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT cast(sum(h.invoice.totalAmount) as double ) " +
            "FROM HistoryInvoiceStatus h " +
            "WHERE h.invoiceStatus = :status " +
            "AND h.createdDate >= :startOfDay " +
            "AND h.createdDate < :endOfDay")
    Double doanhThuHomNay(@Param("status") InvoiceStatus status,
                              @Param("startOfDay") LocalDateTime startOfDay,
                              @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT cast(sum(h.invoice.totalAmount - h.invoice.discount) as double ) " +
            "FROM HistoryInvoiceStatus h " +
            "WHERE h.invoiceStatus = ?3 " +
            "AND Month(h.createdDate) = ?1 " +
            "AND year(h.createdDate) = ?2")
    Double doanhThuThang(Integer month, Integer year,InvoiceStatus status);
}
