package com.web.controller.admin;

import com.web.entity.Blog;
import com.web.entity.Voucher;
import com.web.exception.MessageException;
import com.web.repository.CategoryRepository;
import com.web.service.DiscountProductService;
import com.web.service.VoucherService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/discount-product")
public class DiscountProductController {

    @Autowired
    private DiscountProductService discountProductService;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/list")
    public String viewPage(Model model){
        return "admin/discount-product.html";
    }

    @GetMapping("/save")
    public String viewPageAdd(Model model){
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/add-discount-product.html";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes){
        try {
            discountProductService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
