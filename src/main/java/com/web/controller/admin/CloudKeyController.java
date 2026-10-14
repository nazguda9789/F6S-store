package com.web.controller.admin;

import com.web.entity.Category;
import com.web.entity.CloudKey;
import com.web.exception.MessageException;
import com.web.service.CategoryService;
import com.web.service.CloudKeyService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/cloudkey")
public class CloudKeyController {

    @Autowired
    private CloudKeyService cloudKeyService;

    @GetMapping("/list")
    public String viewPage(Model model){
        List<CloudKey> list = cloudKeyService.findAllList();
        model.addAttribute("listcloud", list);
        model.addAttribute("cloudKey", new CloudKey());
        return "admin/cloudkey.html";
    }


    @PostMapping("/save")
    public String saveCategory(CloudKey cloudKey, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            cloudKeyService.saveOrUpdate(cloudKey);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu cloud key thành công");
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
            cloudKeyService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
