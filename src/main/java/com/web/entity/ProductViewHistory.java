package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử xem sản phẩm của người dùng
 * Dùng để tính điểm gợi ý theo hành vi
 */
@Entity
@Table(name = "product_view_history",
        indexes = {
                @Index(name = "idx_pvh_user", columnList = "user_id"),
                @Index(name = "idx_pvh_product", columnList = "product_id"),
                @Index(name = "idx_pvh_session", columnList = "session_id")
        })
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ProductViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null nếu chưa đăng nhập
    private Long userId;

    // dùng cho khách vãng lai
    private String sessionId;

    @Column(nullable = false)
    private Long productId;

    // Số lần xem sản phẩm này trong session
    private Integer viewCount = 1;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastViewedAt;
}