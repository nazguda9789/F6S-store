package com.web.controller.user;

import com.web.dto.ProductRecommendDTO;
import com.web.dto.ProductView;
import com.web.entity.Category;
import com.web.service.BlogService;
import com.web.service.ProductService;
import com.web.service.RecommendationService;
import com.web.service.VoucherService;
import com.web.utils.UserUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BlogService blogService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserUtils userUtils;

    @GetMapping({"/","/index"})
    public String homeView(Model model, HttpSession session){
        Page<ProductView> productViewPage = productService.getSanPhamBanChay();

        List<Category> parentCategory = (List<Category>) model.getAttribute("categories");

        if(parentCategory.size() > 0){
            Long id = parentCategory.get(0).getId();
            Page<ProductView> productByCategory = productService.getProductByCategory(id);
            model.addAttribute("sanPhamDanhMuc", productByCategory.getContent());
            model.addAttribute("danhMucSp", parentCategory.get(0));
        }
        else{
            model.addAttribute("sanPhamDanhMuc", new ArrayList<ProductView>());
        }

        model.addAttribute("sanPhamBanChay", productViewPage.getContent());
        model.addAttribute("vouchers", voucherService.available());
        model.addAttribute("lastBlog", blogService.lastBlog());
        model.addAttribute("listBlog", blogService.listBlogIndex());

        Long userId = userUtils.getIdUserLogged();
        String sessionId = session.getId();
        List<ProductRecommendDTO> list = recommendationService.getHomepageRecommendations(userId, sessionId, 5);
        model.addAttribute("listRecommend", list);

        return "user/index";
    }

}
