package com.web.repository;

import com.web.entity.InvoiceDetail;
import com.web.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {

    @Query(value = "SELECT * FROM invoice_detail WHERE id_product_detail = ?1 LIMIT 1", nativeQuery = true)
    InvoiceDetail findByDetail(Long detailId);

    @Query(value = "SELECT ivd.* FROM invoice_detail ivd inner join product_detail p on p.id = ivd.id_product_detail where p.product_id = 2", nativeQuery = true)
    InvoiceDetail findByProduct(Long id);

    @Query("select i from InvoiceDetail i where i.idInvoice = ?1")
    List<InvoiceDetail> findByInvoiceId(Long id);

    @Query("select i from InvoiceDetail i where i.idInvoice in ?1")
    List<InvoiceDetail> findAllByInvoiceId(List<Long> idInvoices);

    @Query(value = "SELECT count(ivd.id) FROM invoice_detail ivd inner join product_detail p on p.id = ivd.id_product_detail where p.product_id = ?1", nativeQuery = true)
    Long countByProduct(Long prorductId);

    List<InvoiceDetail> findByIdInvoice(Long invoiceId);

    @Query(value = "select p.* from invoice_detail iv inner join product_detail pd on pd.id = iv.id_product_detail inner join product p on p.id = pd.product_id where iv.id_invoice = ?1", nativeQuery = true)
    List<Product> findProductByIdInvoice(Long invoiceId);

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
          AND MONTH(i.created_date) = ?1
          AND YEAR(i.created_date) = ?2
        GROUP BY c.id, c.name
        ORDER BY totalRevenue DESC
        """, nativeQuery = true)
    List<Object[]> revenueByCategoryAndMonth(int month, int year);


    @Query(value = """
    SELECT DISTINCT ivd.id_invoice
    FROM invoice_detail ivd
    JOIN product_detail pd ON pd.id = ivd.id_product_detail
    WHERE pd.product_id = ?1
    """, nativeQuery = true)
    Page<Long> findInvoiceIdsByProductId(Long productId, Pageable pageable);

    @Query(value = """
    SELECT pd.product_id AS productId, COUNT(*) AS coCount
    FROM invoice_detail ivd
    JOIN product_detail pd ON pd.id = ivd.id_product_detail
    WHERE ivd.id_invoice IN (:invoiceIds)
      AND pd.product_id <> :excludeProductId
    GROUP BY pd.product_id
    ORDER BY coCount DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findCoOccurringProducts(
            @Param("excludeProductId") Long excludeProductId,
            @Param("invoiceIds") List<Long> invoiceIds,
            @Param("limit") int limit);
}
