package com.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về cho API gợi ý sản phẩm
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRecommendDTO {

    private Long id;
    private String name;
    private String slug;
    private String image;
    private Double price;
    private Double olePrice;
    private Double finalPrice;
    private Double discountAmount;
    private Integer discountPercent;
    private Long sold;
    private String recommendReason; // Lý do gợi ý: "Cùng danh mục", "Bán chạy", "Giá tương tự"...
    private Double score;           // Điểm gợi ý (dùng để sort nội bộ)
}