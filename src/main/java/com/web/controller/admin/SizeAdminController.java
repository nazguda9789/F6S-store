package com.web.controller.admin;

import com.web.entity.Category;
import com.web.entity.Size;
import com.web.exception.MessageException;
import com.web.service.CategoryService;
import com.web.service.SizeService;
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
@RequestMapping(TypeSystem.ADMIN_URL+ "/size")
public class SizeAdminController {

    @Autowired
    private SizeService sizeService;

    @GetMapping("/list")
    public String viewPage(Model model){
        List<Size> list = sizeService.findAllList();
        model.addAttribute("listsize", list);
        model.addAttribute("size", new Size());
        return "admin/size.html";
    }

    @PostMapping("/save")
    public String saveCategory(Size size, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            sizeService.saveOrUpdate(size);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu size thành công");
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
            sizeService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
