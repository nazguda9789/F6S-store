package com.example.demodatn.controller.user;
import com.example.demodatn.dto.sanpham.ProductDto;
import com.example.demodatn.entity.sanpham.Category;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.service.sanpham.ProductFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final int SECTION_SIZE = 8;

    private final ProductFilterService productFilterService;
    private final CategoryRepository categoryRepository;

    /** Trang chu: 100% du lieu tu DB, gia qua PricingService (ben trong ProductFilterService). */
    @GetMapping("/")
    public String home(Model model) {
        List<Category> brands = categoryRepository.findAll().stream()
                .filter(c -> !Boolean.FALSE.equals(c.getStatus()))
                .collect(Collectors.toList());

        // Bán chạy dùng cùng điều kiện doanh thu; chỉ lấy sản phẩm có lượng bán hợp lệ.
        List<ProductDto> bestSellers = productFilterService
                .filterProducts(null, null, null, null, null, null, "best_seller", 0, SECTION_SIZE)
                .getContent().stream().filter(p -> p.getTotalSold() > 0).collect(Collectors.toList());
        List<ProductDto> newest = productFilterService
                .filterProducts(null, null, null, null, null, null, "newest", 0, SECTION_SIZE).getContent();
        List<ProductDto> promotions = productFilterService
                .filterProducts(null, null, null, null, null, true, "newest", 0, SECTION_SIZE).getContent();

        // Hero: anh san pham that tu DB (uu tien san pham ban chay)
        ProductDto heroProduct = bestSellers.stream()
                .filter(p -> p.getImageUrl() != null && p.getImageUrl().startsWith("http"))
                .findFirst()
                .orElse(bestSellers.isEmpty() ? null : bestSellers.get(0));

        model.addAttribute("brands", brands);
        model.addAttribute("bestSellers", bestSellers);
        model.addAttribute("promotions", promotions);
        model.addAttribute("newest", newest);
        model.addAttribute("heroProduct", heroProduct);
        return "user/trang-chu";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/dang-nhap";
    }
}
