package com.web.controller.admin;

import com.web.dto.Result;
import com.web.entity.ShopeeKey;
import com.web.service.ShopeeKeyService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/shopee")
public class ShopeeAdminController {

    @Autowired
    private ShopeeKeyService shopeeKeyService;

    @GetMapping("/key")
    public String listCustomers(Model model){
        model.addAttribute("listShopeeKey", shopeeKeyService.findAll());
        model.addAttribute("shopeeKey", new ShopeeKey()); // cho form add
        return "admin/shopee-key";
    }

    @PostMapping("/add")
    public String addShopeeKey(@ModelAttribute ShopeeKey shopeeKey, RedirectAttributes redirectAttributes) {
        Result<?> result = shopeeKeyService.save(shopeeKey);
        if(result.isSuccess() == true){
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, result.getMessage());
        }
        return "redirect:key";
    }

    @GetMapping("/delete/{id}")
    public String deleteShopeeKey(@PathVariable Long id) {
        shopeeKeyService.delete(id);
        return "redirect:key";
    }
}
