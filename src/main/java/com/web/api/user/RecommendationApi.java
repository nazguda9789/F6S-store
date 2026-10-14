package com.web.api.user;

import com.web.dto.ProductRecommendDTO;
import com.web.service.RecommendationService;
import com.web.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API cho module GỢI Ý SẢN PHẨM
 *
 * Endpoints:
 *  GET /api/recommendations/product/{id}     → Gợi ý trên trang chi tiết sản phẩm
 *  GET /api/recommendations/homepage         → Gợi ý cho trang chủ
 *  POST /api/recommendations/track/{id}      → Ghi lại lượt xem sản phẩm
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationApi {

    private final RecommendationService recommendationService;

    private final UserUtils userUtils;

    /**
     * Gợi ý sản phẩm liên quan trên trang chi tiết
     * Gọi từ JS: fetch('/api/recommendations/product/123?limit=8')
     */
    @GetMapping("/product/{productId}")
    public List<ProductRecommendDTO> getProductRecommendations(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "8") int limit,
            HttpSession session) {

        Long userId = userUtils.getIdUserLogged();
        String sessionId = session.getId();
        return recommendationService.getRecommendations(productId, userId, sessionId, limit);
    }

    /**
     * Gợi ý sản phẩm cho trang chủ (dựa trên lịch sử)
     */
    @GetMapping("/homepage")
    public List<ProductRecommendDTO> getHomepageRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            HttpSession session) {

        Long userId = userUtils.getIdUserLogged();
        String sessionId = session.getId();
        return recommendationService.getHomepageRecommendations(userId, sessionId, limit);
    }

    /**
     * Track lượt xem sản phẩm (gọi khi user vào trang chi tiết)
     * Async - không ảnh hưởng đến tốc độ tải trang
     */
    @PostMapping("/track/{productId}")
    public void trackView(
            @PathVariable Long productId,
            HttpSession session) {

        Long userId = userUtils.getIdUserLogged();
        String sessionId = session.getId();
        recommendationService.trackProductView(productId, userId, sessionId);
    }

}