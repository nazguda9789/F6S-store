package com.web.controller.admin;

import com.web.entity.Banner;
import com.web.exception.MessageException;
import com.web.service.BannerService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/banner")
public class BannerAdminController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public String listBanners(Model model) {
        model.addAttribute("banners", bannerService.getAllBanners());
        return "admin/banner.html";
    }

    // Admin: form tạo banner mới
    @GetMapping("/save")
    public String createForm(@RequestParam(required = false) Long id,Model model) {
        if(id != null){
            Optional<Banner> banner = bannerService.getBannerById(id);
            if(banner.isPresent()){
                model.addAttribute("banner", banner.get());
            }
        }
        else{
            model.addAttribute("banner", new Banner());
        }
        return "admin/addbanner.html";
    }

    @PostMapping("/save")
    public String saveBanner(@ModelAttribute Banner banner, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            bannerService.saveBanner(banner);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu banner thành công");
        } catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + referer;
        }
        return "redirect:list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes){
        try {
            bannerService.deleteBanner(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}

