package com.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web.dto.ActiveDiscountResponse;
import com.web.dto.ProductView;
import com.web.entity.Category;
import com.web.entity.Product;
import com.web.repository.ProductImageRepository;
import com.web.repository.VoucherRepository;
import com.web.service.*;
import com.web.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class ProductUserController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DiscountProductService discountProductService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserUtils userUtils;

    @Value("brand.name")
    String brandName;

    private Integer size = 16;


    @GetMapping({"/product/{slug}"})
    public String productView(@PathVariable String slug, Model model, HttpServletRequest request, HttpSession session){
        Product product = productService.findBySlug(slug);
        if(product == null || !Boolean.TRUE.equals(product.getActive())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        ActiveDiscountResponse discountResponse = discountProductService.getActiveDiscount(product.getId(), product.getPrice());
        if(discountResponse != null){
            model.addAttribute("discountProduct", discountResponse);
            product.setOlePrice(product.getPrice());
            product.setPrice(discountResponse.getFinalPrice());
        }
        List<ProductView> productSamePrice = productService.productSamePrice(product.getPrice());
        model.addAttribute("product", product);
        model.addAttribute("productSamePrice", productSamePrice);
        model.addAttribute("listImage", productImageRepository.findByProduct(product.getId()));
        model.addAttribute("listVoucher", voucherRepository.availableLimit3());
        String jsonLdString = productService.genJsonByProduct(product,request);
        model.addAttribute("jsonLd", jsonLdString);

        Long userId = userUtils.getIdUserLogged();
        String sessionId = session.getId();
        recommendationService.trackProductView(product.getId(), userId, sessionId);
        return "user/product";
    }

    @GetMapping({"/category/{slug}"})
    public String productByCategory(@PathVariable String slug, Model model, Pageable pageable, HttpServletRequest request){
        Category category = categoryService.findBySlug(slug);
        if(category == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        model.addAttribute("category", category);
        model.addAttribute("vouchers", voucherService.available());

        pageable = PageRequest.of(pageable.getPageNumber(), size, Sort.by("price").ascending());
        Page<ProductView> page = productService.findByCategory(category.getId(), pageable);
        model.addAttribute("productList", page.getContent());
        model.addAttribute("tongSoTrang", page.getTotalPages());
        model.addAttribute("pageable", pageable);
        model.addAttribute("trangHienTai", page.getNumber());
        model.addAttribute("currentUri", request.getRequestURI());
        String jsonLdString = productService.genJsonByCategory(category,page.getContent(),request);
        model.addAttribute("jsonLd", jsonLdString);
        return "user/productfilter";
    }
}
