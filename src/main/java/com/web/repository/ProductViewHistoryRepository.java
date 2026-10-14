package com.web.repository;

import com.web.entity.ProductViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductViewHistoryRepository extends JpaRepository<ProductViewHistory, Long> {

    // Tìm lịch sử xem theo user + product
    Optional<ProductViewHistory> findByUserIdAndProductId(Long userId, Long productId);

    // Tìm lịch sử xem theo session + product (khách vãng lai)
    Optional<ProductViewHistory> findBySessionIdAndProductId(String sessionId, Long productId);

    // Lấy danh sách sản phẩm đã xem gần đây của user (dùng để gợi ý)
    @Query("SELECT h.productId FROM ProductViewHistory h WHERE h.userId = ?1 ORDER BY h.lastViewedAt DESC")
    List<Long> findRecentProductIdsByUser(Long userId);

    // Lấy danh sách sản phẩm đã xem gần đây theo session (khách vãng lai)
    @Query("SELECT h.productId FROM ProductViewHistory h WHERE h.sessionId = ?1 ORDER BY h.lastViewedAt DESC")
    List<Long> findRecentProductIdsBySession(String sessionId);

    // Top sản phẩm được xem nhiều nhất trong N ngày gần đây
    @Query(value = """
        SELECT h.product_id, COUNT(h.id) AS viewCount
        FROM product_view_history h
        WHERE h.last_viewed_at >= ?1
        GROUP BY h.product_id
        ORDER BY viewCount DESC
        LIMIT ?2
        """, nativeQuery = true)
    List<Object[]> findTopViewedProducts(LocalDateTime since, int limit);

    // Xóa lịch sử cũ (giữ DB gọn nhẹ)
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductViewHistory h WHERE h.lastViewedAt < ?1")
    void deleteOlderThan(LocalDateTime cutoff);

    /**
     * Lấy tất cả lịch sử xem của user, sắp xếp theo thời gian giảm dần
     */
    @Query("SELECT h FROM ProductViewHistory h WHERE h.userId = ?1 ORDER BY h.lastViewedAt DESC")
    List<ProductViewHistory> findByUserIdOrderByLastViewedAtDesc(Long userId);

    /**
     * Lấy tất cả lịch sử xem theo session, sắp xếp theo thời gian giảm dần
     */
    @Query("SELECT h FROM ProductViewHistory h WHERE h.sessionId = ?1 ORDER BY h.lastViewedAt DESC")
    List<ProductViewHistory> findBySessionIdOrderByLastViewedAtDesc(String sessionId);
}