package com.web.repository;

import com.web.dto.CustomerSummary;
import com.web.dto.OrderCountByStatus;
import com.web.dto.TopProductDTO;
import com.web.entity.Invoice;
import com.web.enums.InvoiceStatus;
import com.web.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    @Query("select i from Invoice i where i.code = ?1")
    Optional<Invoice> findByCode(String code);

    @Query("select i from Invoice i where i.code = ?1 and i.token = ?2")
    Optional<Invoice> findByCodeAndToken(String code, String token);

    @Query("SELECT i.invoiceStatus AS status, COUNT(i) AS orderCount\n" +
            "    FROM Invoice i\n" +
            "    WHERE i.invoiceStatus IN :statuses\n" +
            "    GROUP BY i.invoiceStatus")
    List<OrderCountByStatus> countOrdersByStatuses(@Param("statuses") List<InvoiceStatus> statuses);

    @Query("select count(i.id) from Invoice i where i.invoiceStatus = 'DANG_CHO_VAN_CHUYEN'")
    Long donHangMoi();

    @Query("select count(i.id) from Invoice i where i.invoiceStatus = ?1")
    Long countByStatus(InvoiceStatus invoiceStatus);

    @Query("select i from Invoice i where i.userId = ?1")
    List<Invoice> findByUserId(Long id);

    @Query("""
    SELECT i.fullName AS fullName,
           i.phone AS phone,
           i.email AS email,
           COUNT(i.id) AS totalInvoice,
           SUM(i.totalAmount) AS totalAmount,
           MAX(i.code) AS lastInvoiceCode,
           MAX(i.id) AS lastInvoiceId,
           MAX(i.createdDate) AS lastInvoiceDate
    FROM Invoice i
    WHERE (:search IS NULL OR LOWER(i.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(i.phone) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(i.email) LIKE LOWER(CONCAT('%', :search, '%')))
    AND (:status IS NULL OR i.invoiceStatus = :status)
    AND (:type IS NULL OR i.invoiceType = :type)
    AND (:from IS NULL OR i.createdDate >= :from)
    AND (:to IS NULL OR i.createdDate <= :to)
    GROUP BY i.fullName, i.phone, i.email
    ORDER BY MAX(i.createdDate) DESC
    """)
    Page<CustomerSummary> findCustomerSummary(String search,InvoiceStatus status,InvoiceType type,LocalDateTime from,LocalDateTime to,Pageable pageable);


    @Query(value = "select sum(i.total_amount) from invoice i where Month(i.created_date) = ?1 and Year(i.created_date) = ?2 and i.invoice_status = ?3", nativeQuery = true)
    public Double calDt(Integer thang, Integer month, InvoiceStatus invoiceStatus);

    @Query(value = """
        SELECT 
            d.id_product_detail AS productId,
            d.product_name AS productName,
            d.product_image AS productImage,
            SUM(d.quantity) AS totalQuantity,
            d.price AS price
        FROM invoice_detail d
        JOIN invoice i ON d.id_invoice = i.id
        WHERE MONTH(i.created_date) = MONTH(CURRENT_DATE())
          AND YEAR(i.created_date) = YEAR(CURRENT_DATE())
        GROUP BY d.id_product_detail, d.product_name, d.product_image, d.price
        ORDER BY totalQuantity DESC
        LIMIT 10
        """,
            nativeQuery = true)
    List<TopProductDTO> findTop10SellingProductsThisMonth();

    List<Invoice> findByCreatedDateBetween(LocalDateTime from, LocalDateTime to);



    // 1. Doanh thu theo ngày trong khoảng thời gian (cho biểu đồ 30 ngày)
    @Query(value = """
        SELECT
            DATE_FORMAT(h.created_date, '%d/%m') AS date,
            COALESCE(SUM(i.total_amount - i.discount), 0) AS revenue,
            COUNT(i.id) AS orderCount
        FROM history_invoice_status h
        JOIN invoice i ON i.id = h.invoice_id
        WHERE h.invoice_status = 'HOAN_THANH'
          AND h.created_date BETWEEN :from AND :to
        GROUP BY DATE(h.created_date)
        ORDER BY DATE(h.created_date) ASC
        """, nativeQuery = true)
    List<Object[]> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    // 2. Doanh thu theo danh mục
    @Query(value = """
        SELECT
            c.id AS categoryId,
            c.name AS categoryName,
            SUM(ivd.quantity * ivd.price) AS totalRevenue,
            SUM(ivd.quantity) AS totalQuantity
        FROM invoice_detail ivd
        JOIN product_detail pd ON pd.id = ivd.id_product_detail
        JOIN product_category pc ON pc.product_id = pd.product_id
        JOIN category c ON c.id = pc.category_id
        JOIN invoice i ON i.id = ivd.id_invoice
        WHERE i.invoice_status = 'HOAN_THANH'
          AND MONTH(i.created_date) = :month
          AND YEAR(i.created_date) = :year
        GROUP BY c.id, c.name
        ORDER BY totalRevenue DESC
        """, nativeQuery = true)
    List<Object[]> revenueByCategoryAndMonth(@Param("month") int month, @Param("year") int year);


    // 3. Đếm đơn hàng mới theo ngày hôm nay
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.createdDate >= :startOfDay AND i.createdDate < :endOfDay")
    Long countOrdersToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // 4. Đếm khách hàng mới hôm nay (userId chưa có đơn nào trước đó)
    @Query(value = """
        SELECT COUNT(DISTINCT i.user_id) FROM invoice i
        WHERE DATE(i.created_date) = CURDATE()
        AND i.user_id IS NOT NULL
        AND i.user_id NOT IN (
            SELECT DISTINCT i2.user_id FROM invoice i2
            WHERE i2.user_id IS NOT NULL
              AND DATE(i2.created_date) < CURDATE()
        )
        """, nativeQuery = true)
    Long countNewCustomersToday();
}
