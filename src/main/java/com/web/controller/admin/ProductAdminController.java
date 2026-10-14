package com.web.controller.admin;

import com.web.entity.Product;
import com.web.repository.ProductColorImageRepository;
import com.web.repository.ProductDetailRepository;
import com.web.repository.ProductImageRepository;
import com.web.service.CategoryService;
import com.web.service.ColorService;
import com.web.service.ProductService;
import com.web.service.SizeService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/product")
public class ProductAdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SizeService sizeService;

    @Autowired
    private ColorService colorService;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductColorImageRepository productColorImageRepository;

    @GetMapping("/list")
    public String viewPage(Model model){
        model.addAttribute("listCategory", categoryService.findAllList());
        model.addAttribute("totalProduct", productService.totalProduct());
        return "admin/product.html";
    }

    @GetMapping("/save")
    public String viewSavePage(Model model, @RequestParam(required = false) Long id){
        model.addAttribute("listCategory", categoryService.findAllList());
        model.addAttribute("listSize", sizeService.findAllList());
        model.addAttribute("listColor", colorService.findAllList());
        if(id != null){
            model.addAttribute("product", productService.findById(id));
            model.addAttribute("images", productImageRepository.findByProduct(id));
            model.addAttribute("details", productDetailRepository.findByProductId(id));
            model.addAttribute("productColor", productColorImageRepository.findByProduct(id));
        }
        else{
            model.addAttribute("product", new Product());
        }
        return "admin/addproduct.html";
    }


}
