package com.web.service;

import com.web.dto.ProductRecommendDTO;
import com.web.dto.ProductView;
import com.web.entity.Product;
import com.web.entity.ProductViewHistory;
import com.web.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý logic GỢI Ý SẢN PHẨM
 *
 * Hỗ trợ 5 chiến lược gợi ý:
 *  1. Cùng danh mục (Content-based)
 *  2. Bán chạy nhất (Popularity-based)
 *  3. Giá tương tự (Price similarity)
 *  4. Mua cùng nhau (Collaborative - dựa trên invoice)
 *  5. Gần đây đã xem (View history)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ProductViewHistoryRepository viewHistoryRepository;
    private final InvoiceRepository invoiceRepository;

    // =================== GỢI Ý CHÍNH ===================

    /**
     * Gợi ý tổng hợp cho trang chi tiết sản phẩm.
     * Kết hợp nhiều chiến lược, loại bỏ trùng lặp, trả về top N.
     *
     * @param productId    sản phẩm đang xem
     * @param userId       null nếu chưa đăng nhập
     * @param sessionId    session id của khách vãng lai
     * @param limit        số lượng gợi ý trả về (mặc định 8)
     */
    /**
     * Gợi ý tổng hợp cho trang chi tiết sản phẩm.
     * Kết hợp nhiều chiến lược, loại bỏ trùng lặp, trả về top N.
     *
     * ƯU TIÊN: Lượt xem > Mua cùng > Cùng danh mục > Giá tương tự > Bán chạy
     *
     * @param productId    sản phẩm đang xem
     * @param userId       null nếu chưa đăng nhập
     * @param sessionId    session id của khách vãng lai
     * @param limit        số lượng gợi ý trả về (mặc định 8)
     */
    @Transactional(readOnly = true)
    public List<ProductRecommendDTO> getRecommendations(Long productId,
                                                        Long userId,
                                                        String sessionId,
                                                        int limit) {
        // scoreMap: id → (dto, tổng điểm tích lũy, danh sách lý do)
        Map<Long, ProductRecommendDTO> scoreMap = new LinkedHashMap<>();
        Map<Long, Set<String>> reasonMap = new LinkedHashMap<>();

        // Lấy thông tin sản phẩm hiện tại
        Product currentProduct = productRepository.findById(productId).orElse(null);
        if (currentProduct == null) return Collections.emptyList();

        // --- Helper functions ---
        java.util.function.BiConsumer<ProductRecommendDTO, Double> addScore = (dto, delta) -> {
            ProductRecommendDTO existing = scoreMap.get(dto.getId());
            if (existing == null) {
                dto.setScore(delta);
                scoreMap.put(dto.getId(), dto);
                reasonMap.put(dto.getId(), new LinkedHashSet<>());
            } else {
                existing.setScore(existing.getScore() + delta);
            }
        };

        java.util.function.BiConsumer<Long, String> addReason = (id, reason) -> {
            reasonMap.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(reason);
        };

        // =================== CHIẾN LƯỢC 1: VIEW HISTORY (ƯU TIÊN CAO NHẤT) ===================
        Map<Long, Double> viewBoostMap = buildViewBoostMap(userId, sessionId);

        if (!viewBoostMap.isEmpty()) {
            log.info("=== VIEW BOOST MAP: {} products ===", viewBoostMap.size());
            viewBoostMap.forEach((id, boost) -> log.debug("Product {}: view boost = {}", id, boost));

            // Lấy tất cả danh mục từ các sản phẩm đã xem
            Set<Long> viewedProductIds = viewBoostMap.keySet();
            Set<Long> viewedCatIds = new HashSet<>();

            for (Long viewedId : viewedProductIds) {
                try {
                    List<Long> cats = productCategoryRepository.findCategoryIdsByProductId(viewedId);
                    viewedCatIds.addAll(cats);
                } catch (Exception e) {
                    log.debug("Error getting categories for product {}: {}", viewedId, e.getMessage());
                }
            }

            if (!viewedCatIds.isEmpty()) {
                // Lấy sản phẩm cùng danh mục với sản phẩm đã xem
                List<ProductView> similarByView = productRepository.findByCategoryIds(
                        new ArrayList<>(viewedCatIds));

                int viewBoostCount = 0;
                for (ProductView product : similarByView) {
                    if (product.getId().equals(productId)) continue;

                    // Tính tổng điểm boost từ tất cả sản phẩm đã xem
                    double totalViewBoost = 0.0;
                    for (Map.Entry<Long, Double> entry : viewBoostMap.entrySet()) {
                        Long viewedId = entry.getKey();
                        Double boostValue = entry.getValue();

                        try {
                            List<Long> candidateCats = productCategoryRepository.findCategoryIdsByProductId(product.getId());
                            List<Long> viewedCats = productCategoryRepository.findCategoryIdsByProductId(viewedId);

                            boolean sameCategory = candidateCats.stream().anyMatch(viewedCats::contains);
                            if (sameCategory) {
                                // Cộng dồn điểm boost nếu cùng danh mục
                                totalViewBoost += boostValue;
                            }
                        } catch (Exception ignored) {}
                    }

                    if (totalViewBoost > 0) {
                        // NHÂN HỆ SỐ 5 ĐỂ ƯU TIÊN VIEW BOOST
                        double finalBoost = totalViewBoost * 5.0;

                        ProductRecommendDTO dto = toRecommendDTOView(product, "Dựa trên lịch sử xem", finalBoost);

                        ProductRecommendDTO existing = scoreMap.get(product.getId());
                        if (existing == null) {
                            dto.setScore(finalBoost);
                            scoreMap.put(product.getId(), dto);
                            reasonMap.put(product.getId(), new LinkedHashSet<>());
                        } else {
                            existing.setScore(existing.getScore() + finalBoost);
                        }
                        addReason.accept(product.getId(), "Phù hợp sở thích (🔥 Ưu tiên cao)");
                        viewBoostCount++;
                    }
                }
                log.info("Added {} products from view history boost", viewBoostCount);
            }
        }

        // =================== CHIẾN LƯỢC 2: THƯỜNG MUA CÙNG NHAU (weight 8 -> giảm còn 6) ===================
        List<ProductRecommendDTO> boughtTogetherProducts = getBoughtTogetherProducts(productId, limit);
        log.info("Bought together products: {}", boughtTogetherProducts.size());

        for (ProductRecommendDTO product : boughtTogetherProducts) {
            addScore.accept(product, 6.0);
            addReason.accept(product.getId(), "Thường mua cùng");
        }

        // =================== CHIẾN LƯỢC 3: CÙNG DANH MỤC (weight 10 -> giảm còn 5) ===================
        List<ProductRecommendDTO> sameCategoryProducts = getSameCategoryProducts(productId, limit * 2);
        log.info("Same category products: {}", sameCategoryProducts.size());

        for (ProductRecommendDTO product : sameCategoryProducts) {
            addScore.accept(product, 5.0);
            addReason.accept(product.getId(), "Cùng danh mục");
        }

        // =================== CHIẾN LƯỢC 4: GIÁ TƯƠNG TỰ (weight 5 -> giảm còn 4) ===================
        if (currentProduct.getPrice() != null) {
            List<ProductRecommendDTO> similarPriceProducts = getSimilarPriceProducts(
                    currentProduct.getPrice(), productId, limit);
            log.info("Similar price products: {}", similarPriceProducts.size());

            for (ProductRecommendDTO product : similarPriceProducts) {
                // Càng gần giá gốc càng điểm cao
                double priceDiff = Math.abs(product.getPrice() - currentProduct.getPrice()) / currentProduct.getPrice();
                double priceBonus = 1.5 * (1 - priceDiff); // [0, 1.5]
                addScore.accept(product, 4.0 + priceBonus);
                addReason.accept(product.getId(), "Giá tương tự");
            }
        }

        // =================== CHIẾN LƯỢC 5: BÁN CHẠY NHẤT (weight 7 -> giảm còn 3) ===================
        List<ProductRecommendDTO> bestSellerProducts = getBestSellerProducts(limit * 2);
        log.info("Best seller products: {}", bestSellerProducts.size());

        for (ProductRecommendDTO product : bestSellerProducts) {
            // Sold count bonus giảm ảnh hưởng
            double soldBonus = product.getSold() != null ? Math.min(product.getSold() / 200.0, 1.0) : 0;
            addScore.accept(product, 3.0 + soldBonus);
            addReason.accept(product.getId(), "Bán chạy");
        }

        // =================== BOOST THÊM CHO SẢN PHẨM CÙNG DANH MỤC VỚI LỊCH SỬ XEM ===================
        // Lần 2: Boost cho các sản phẩm đã có trong scoreMap dựa trên view history
        if (!viewBoostMap.isEmpty()) {
            for (Map.Entry<Long, Double> entry : viewBoostMap.entrySet()) {
                Long viewedProductId = entry.getKey();
                Double boostValue = entry.getValue();

                List<Long> viewedCatIds;
                try {
                    viewedCatIds = productCategoryRepository.findCategoryIdsByProductId(viewedProductId);
                } catch (Exception e) {
                    continue;
                }
                if (viewedCatIds.isEmpty()) continue;

                // Boost cho tất cả sản phẩm trong scoreMap có cùng danh mục
                for (Map.Entry<Long, ProductRecommendDTO> scoreEntry : scoreMap.entrySet()) {
                    Long candidateId = scoreEntry.getKey();
                    ProductRecommendDTO candidate = scoreEntry.getValue();

                    try {
                        List<Long> candidateCatIds = productCategoryRepository.findCategoryIdsByProductId(candidateId);
                        boolean sameCategory = candidateCatIds.stream().anyMatch(viewedCatIds::contains);

                        if (sameCategory) {
                            // Boost thêm 30% giá trị view boost
                            double additionalBoost = boostValue * 0.3;
                            candidate.setScore(candidate.getScore() + additionalBoost);
                            addReason.accept(candidateId, "Liên quan đến sản phẩm đã xem");
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Gán reason đã tổng hợp vào từng DTO
        scoreMap.forEach((id, dto) -> {
            Set<String> reasons = reasonMap.getOrDefault(id, Collections.emptySet());
            dto.setRecommendReason(String.join(" • ", reasons));
        });

        // Lọc, sắp xếp và trả về kết quả
        List<ProductRecommendDTO> results = scoreMap.values().stream()
                .filter(p -> !p.getId().equals(productId))
                .sorted(Comparator.comparingDouble(ProductRecommendDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Log kết quả top 5 để debug
        log.info("=== FINAL RECOMMENDATIONS (Top {}) ===", Math.min(5, results.size()));
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            ProductRecommendDTO dto = results.get(i);
            log.info("{}. Product: {} | Score: {:.2f} | Reason: {}",
                    i + 1, dto.getName(), dto.getScore(), dto.getRecommendReason());
        }

        return results;
    }

    /**
     * Gợi ý cho trang chủ / danh mục - top sản phẩm phổ biến
     */
    /**
     * Gợi ý cho trang chủ / danh mục - ưu tiên dựa trên lịch sử xem
     *
     * Thứ tự ưu tiên:
     * 1. Dựa trên lịch sử xem (view history) - Ưu tiên cao nhất
     * 2. Sản phẩm bán chạy (fallback)
     *
     * @param userId    ID user (null nếu chưa đăng nhập)
     * @param sessionId Session ID cho khách vãng lai
     * @param limit     Số lượng gợi ý trả về
     * @return Danh sách sản phẩm gợi ý
     */
    @Transactional(readOnly = true)
    public List<ProductRecommendDTO> getHomepageRecommendations(Long userId,
                                                                String sessionId,
                                                                int limit) {
        Map<Long, ProductRecommendDTO> scoreMap = new LinkedHashMap<>();
        Map<Long, Set<String>> reasonMap = new LinkedHashMap<>();

        // =================== CHIẾN LƯỢC 1: VIEW HISTORY (ƯU TIÊN CAO NHẤT) ===================
        Map<Long, Double> viewBoostMap = buildViewBoostMap(userId, sessionId);

        if (!viewBoostMap.isEmpty()) {
            log.info("=== HOMEPAGE: View boost map with {} products ===", viewBoostMap.size());

            // Lấy tất cả danh mục từ các sản phẩm đã xem
            Set<Long> viewedProductIds = viewBoostMap.keySet();
            Set<Long> viewedCatIds = new HashSet<>();

            for (Long viewedId : viewedProductIds) {
                try {
                    List<Long> cats = productCategoryRepository.findCategoryIdsByProductId(viewedId);
                    viewedCatIds.addAll(cats);
                } catch (Exception e) {
                    log.debug("Error getting categories for product {}: {}", viewedId, e.getMessage());
                }
            }

            if (!viewedCatIds.isEmpty()) {
                // Lấy sản phẩm cùng danh mục với sản phẩm đã xem
                List<ProductView> similarByView = productRepository.findByCategoryIds(
                        new ArrayList<>(viewedCatIds));

                int viewBoostCount = 0;
                for (ProductView product : similarByView) {
                    // Bỏ qua sản phẩm đã xem rồi
                    if (viewedProductIds.contains(product.getId())) continue;

                    // Tính tổng điểm boost từ tất cả sản phẩm đã xem
                    double totalViewBoost = 0.0;
                    for (Map.Entry<Long, Double> entry : viewBoostMap.entrySet()) {
                        Long viewedId = entry.getKey();
                        Double boostValue = entry.getValue();

                        try {
                            List<Long> candidateCats = productCategoryRepository.findCategoryIdsByProductId(product.getId());
                            List<Long> viewedCats = productCategoryRepository.findCategoryIdsByProductId(viewedId);

                            boolean sameCategory = candidateCats.stream().anyMatch(viewedCats::contains);
                            if (sameCategory) {
                                totalViewBoost += boostValue;
                            }
                        } catch (Exception ignored) {}
                    }

                    if (totalViewBoost > 0) {
                        // NHÂN HỆ SỐ 5 ĐỂ ƯU TIÊN VIEW BOOST (giống getRecommendations)
                        double finalBoost = totalViewBoost * 5.0;

                        ProductRecommendDTO dto = toRecommendDTOView(product, "Dành cho bạn", finalBoost);

                        ProductRecommendDTO existing = scoreMap.get(product.getId());
                        if (existing == null) {
                            dto.setScore(finalBoost);
                            scoreMap.put(product.getId(), dto);
                            reasonMap.put(product.getId(), new LinkedHashSet<>());
                        } else {
                            existing.setScore(existing.getScore() + finalBoost);
                        }
                        reasonMap.computeIfAbsent(product.getId(), k -> new LinkedHashSet<>())
                                .add("Dựa trên lịch sử xem (🔥 Ưu tiên cao)");
                        viewBoostCount++;
                    }
                }
                log.info("HOMEPAGE: Added {} products from view history boost", viewBoostCount);
            }
        }

        // =================== CHIẾN LƯỢC 2: BÁN CHẠY NHẤT (FALLBACK) ===================
        // Chỉ thêm sản phẩm bán chạy nếu chưa đủ limit
        if (scoreMap.size() < limit) {
            int remaining = limit - scoreMap.size();
            List<Product> topSelling = productRepository.findTop10ByOrderBySoldDesc();

            int bestSellerCount = 0;
            for (Product product : topSelling) {
                if (bestSellerCount >= remaining) break;

                // Bỏ qua sản phẩm đã có trong scoreMap
                if (scoreMap.containsKey(product.getId())) continue;

                // Tính sold bonus (giảm ảnh hưởng so với view)
                double soldBonus = product.getSold() != null ? Math.min(product.getSold() / 200.0, 1.0) : 0;
                double score = 3.0 + soldBonus; // Weight thấp hơn view boost nhiều

                ProductRecommendDTO dto = toRecommendDTO(product, "Bán chạy nhất", score);
                scoreMap.put(product.getId(), dto);
                reasonMap.computeIfAbsent(product.getId(), k -> new LinkedHashSet<>())
                        .add("Bán chạy");
                bestSellerCount++;
            }
            log.info("HOMEPAGE: Added {} best seller products as fallback", bestSellerCount);
        }

        // =================== TRƯỜNG HỢP ĐẶC BIỆT: KHÔNG CÓ LỊCH SỬ XEM ===================
        // Nếu không có dữ liệu view history và cũng không đủ sản phẩm bán chạy
        if (scoreMap.isEmpty()) {
            log.info("HOMEPAGE: No view history, getting default products");
            List<Product> defaultProducts = productRepository.findTop10ByOrderBySoldDesc();
            int count = 0;
            for (Product product : defaultProducts) {
                if (count >= limit) break;
                ProductRecommendDTO dto = toRecommendDTO(product, "Gợi ý cho bạn", 5.0);
                scoreMap.put(product.getId(), dto);
                reasonMap.computeIfAbsent(product.getId(), k -> new LinkedHashSet<>())
                        .add("Sản phẩm nổi bật");
                count++;
            }
        }

        // Gán reason đã tổng hợp vào từng DTO
        scoreMap.forEach((id, dto) -> {
            Set<String> reasons = reasonMap.getOrDefault(id, Collections.emptySet());
            dto.setRecommendReason(String.join(" • ", reasons));
        });

        // Sắp xếp theo điểm giảm dần và trả về kết quả
        List<ProductRecommendDTO> results = scoreMap.values().stream()
                .sorted(Comparator.comparingDouble(ProductRecommendDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Log kết quả top 5 để debug
        log.info("=== HOMEPAGE FINAL RECOMMENDATIONS (Top {}) ===", Math.min(5, results.size()));
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            ProductRecommendDTO dto = results.get(i);
            log.info("{}. Product: {} | Score: {:.2f} | Reason: {}",
                    i + 1, dto.getName(), dto.getScore(), dto.getRecommendReason());
        }

        return results;
    }

    // =================== CÁC CHIẾN LƯỢC CỤ THỂ ===================

    /**
     * Chiến lược 1: Cùng danh mục
     */
    private List<ProductRecommendDTO> getSameCategoryProducts(Long productId, int limit) {
        try {
            List<Long> categoryIds = productCategoryRepository.findCategoryIdsByProductId(productId);
            if (categoryIds.isEmpty()) return Collections.emptyList();

            List<ProductView> products = productRepository.findByCategoryIds(categoryIds);
            return products.stream()
                    .filter(p -> !p.getId().equals(productId))
                    .limit(limit)
                    .map(p -> toRecommendDTOView(p, "Cùng danh mục", 0.0))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getSameCategoryProducts error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Chiến lược 2: Bán chạy nhất
     */
    private List<ProductRecommendDTO> getBestSellerProducts(int limit) {
        try {
            return productRepository.findTop10ByOrderBySoldDesc().stream()
                    .limit(limit)
                    .map(p -> toRecommendDTO(p, "Bán chạy", 0.0))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getBestSellerProducts error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Chiến lược 3: Giá tương tự (±30%)
     */
    private List<ProductRecommendDTO> getSimilarPriceProducts(Double price,
                                                              Long excludeId,
                                                              int limit) {
        try {
            double minPrice = price * 0.7;
            double maxPrice = price * 1.3;
            List<Product> products = productRepository.findTop10ByPriceLessThanEqualOrderByPriceDesc(maxPrice);
            return products.stream()
                    .filter(p -> !p.getId().equals(excludeId) && p.getPrice() >= minPrice)
                    .limit(limit)
                    .map(p -> toRecommendDTO(p, "Giá tương tự", 0.0))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getSimilarPriceProducts error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Chiến lược 4: Thường mua cùng nhau
     * Tìm các sản phẩm xuất hiện cùng trong các hóa đơn chứa productId
     */
    private List<ProductRecommendDTO> getBoughtTogetherProducts(Long productId, int limit) {
        try {
            // Tìm invoice chứa sản phẩm này
            List<Long> invoiceIds = invoiceDetailRepository
                    .findInvoiceIdsByProductId(productId, PageRequest.of(0, 50)).getContent();

            if (invoiceIds.isEmpty()) return Collections.emptyList();

            // Tìm sản phẩm khác trong các invoice đó
            List<Object[]> coOccurrences = invoiceDetailRepository
                    .findCoOccurringProducts(productId, invoiceIds, limit);

            List<Long> coProductIds = coOccurrences.stream()
                    .map(row -> ((Number) row[0]).longValue())
                    .collect(Collectors.toList());

            if (coProductIds.isEmpty()) return Collections.emptyList();

            return productRepository.findByIdIn(coProductIds).stream()
                    .map(p -> toRecommendDTO(p, "Thường mua cùng", 0.0))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getBoughtTogetherProducts error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Chiến lược 5: Dựa trên lịch sử xem
     */
    private List<ProductRecommendDTO> getRecentlyViewedSimilar(Long userId,
                                                               String sessionId,
                                                               Long excludeId,
                                                               int limit) {
        try {
            List<Long> viewedIds = userId != null
                    ? viewHistoryRepository.findRecentProductIdsByUser(userId)
                    : viewHistoryRepository.findRecentProductIdsBySession(sessionId);

            if (viewedIds.isEmpty()) return Collections.emptyList();

            // Lấy danh mục từ sản phẩm đã xem
            List<Long> categoryIds = productCategoryRepository.findCategoryIdsByProductIds(
                    viewedIds.stream().limit(5).collect(Collectors.toList()));

            if (categoryIds.isEmpty()) return Collections.emptyList();

            return productRepository.findByCategoryIds(categoryIds).stream()
                    .filter(p -> !p.getId().equals(excludeId) && !viewedIds.contains(p.getId()))
                    .limit(limit)
                    .map(p -> toRecommendDTOView(p, "Dựa trên lịch sử", 0.0))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getRecentlyViewedSimilar error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =================== TRACKING ===================

    /**
     * Ghi lại lượt xem sản phẩm (async, không ảnh hưởng performance)
     */
    @Async
    @Transactional
    public void trackProductView(Long productId, Long userId, String sessionId) {
        try {
            Optional<ProductViewHistory> existing = userId != null
                    ? viewHistoryRepository.findByUserIdAndProductId(userId, productId)
                    : viewHistoryRepository.findBySessionIdAndProductId(sessionId, productId);

            if (existing.isPresent()) {
                ProductViewHistory h = existing.get();
                h.setViewCount(h.getViewCount() + 1);
                h.setLastViewedAt(LocalDateTime.now());
                viewHistoryRepository.save(h);
            } else {
                ProductViewHistory h = new ProductViewHistory();
                h.setProductId(productId);
                h.setUserId(userId);
                h.setSessionId(sessionId);
                h.setLastViewedAt(LocalDateTime.now());
                viewHistoryRepository.save(h);
            }
        } catch (Exception e) {
            log.warn("trackProductView failed silently: {}", e.getMessage());
        }
    }

    // =================== HELPER ===================

    private ProductRecommendDTO toRecommendDTO(Product p, String reason, Double score) {
        ProductRecommendDTO dto = new ProductRecommendDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setSlug(p.getSlug());
        dto.setImage(p.getImage());
        dto.setPrice(p.getPrice());
        dto.setOlePrice(p.getOlePrice());
        dto.setFinalPrice(p.getPrice()); // sẽ được override nếu có discount
        dto.setDiscountAmount(0.0);
        dto.setDiscountPercent(0);
        dto.setSold(p.getSold());
        dto.setRecommendReason(reason);
        dto.setScore(score);
        return dto;
    }

    private ProductRecommendDTO toRecommendDTOView(ProductView p, String reason, Double score) {
        ProductRecommendDTO dto = new ProductRecommendDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setSlug(p.getSlug());
        dto.setImage(p.getImage());
        dto.setPrice(p.getPrice());
        dto.setOlePrice(p.getOlePrice());
        dto.setFinalPrice(p.getPrice()); // sẽ được override nếu có discount
        dto.setDiscountAmount(0.0);
        dto.setDiscountPercent(0);
        dto.setRecommendReason(reason);
        dto.setScore(score);
        return dto;
    }

    /**
     * SỬA LẠI HÀM buildViewBoostMap - đảm bảo điểm boost khác biệt theo viewCount
     */
    private Map<Long, Double> buildViewBoostMap(Long userId, String sessionId) {
        Map<Long, Double> boostMap = new HashMap<>();

        try {
            List<ProductViewHistory> viewHistories;

            if (userId != null) {
                viewHistories = viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId);
            } else if (sessionId != null && !sessionId.isEmpty()) {
                viewHistories = viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId);
            } else {
                return boostMap;
            }

            LocalDateTime now = LocalDateTime.now();

            for (ProductViewHistory history : viewHistories) {
                Long productId = history.getProductId();
                Integer viewCount = history.getViewCount() != null ? history.getViewCount() : 1;
                LocalDateTime lastViewedAt = history.getLastViewedAt();

                // TÍNH ĐIỂM BOOST CHI TIẾT - KHÔNG PHẢI HẰNG SỐ
                double timeDecay = calculateTimeDecay(lastViewedAt, now);

                // TĂNG TRỌNG SỐ VIEW COUNT BOOST
                // viewCount=1 → 2.0, viewCount=3 → 3.16, viewCount=10 → 4.78
                double viewCountBoost = Math.log(viewCount + 1) * 2.0;

                // Tổng boost: timeDecay * viewCountBoost * weight
                double boost = timeDecay * viewCountBoost * 3.0; // weight=3

                // Cộng dồn nếu nhiều lần xem
                boostMap.merge(productId, boost, Double::sum);

                log.debug("Product {}: viewCount={}, timeDecay={:.2f}, boost={:.2f}",
                        productId, viewCount, timeDecay, boost);
            }

        } catch (Exception e) {
            log.warn("buildViewBoostMap error: {}", e.getMessage());
        }

        return boostMap;
    }

    /**
     * Tính hệ số decay theo thời gian
     * - Trong 1 giờ: 1.0 (100%)
     * - 1-24 giờ: 0.8
     * - 24-72 giờ: 0.5
     * - 72+ giờ: 0.2
     */
    private double calculateTimeDecay(LocalDateTime lastViewedAt, LocalDateTime now) {
        if (lastViewedAt == null) return 0.3;

        long hours = java.time.Duration.between(lastViewedAt, now).toHours();

        if (hours <= 1) return 1.0;
        if (hours <= 24) return 0.8;
        if (hours <= 72) return 0.5;
        return 0.2;
    }

    /**
     * Tính hệ số decay theo thời gian
     * Công thức: decay = max(0.3, 1.0 - (hours / 72))
     * - 0 giờ: 1.0 (mới xem)
     * - 24 giờ: 0.67
     * - 48 giờ: 0.33
     * - 72+ giờ: 0.3 (tối thiểu)
     *
     * @param lastViewedAt Thời điểm xem cuối cùng
     * @param now          Thời điểm hiện tại
     * @return Hệ số decay trong khoảng [0.3, 1.0]
     */
    private double calculateDecayFactor(LocalDateTime lastViewedAt, LocalDateTime now) {
        if (lastViewedAt == null) return 0.5;

        long hours = java.time.Duration.between(lastViewedAt, now).toHours();

        // Decay dạng tuyến tính, giới hạn trong [0.3, 1.0]
        double decay = 1.0 - (hours / 72.0);
        return Math.max(0.3, Math.min(1.0, decay));
    }
}