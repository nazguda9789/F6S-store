package com.web.controller.admin;

import com.web.entity.Color;
import com.web.entity.Size;
import com.web.exception.MessageException;
import com.web.service.ColorService;
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
@RequestMapping(TypeSystem.ADMIN_URL+ "/color")
public class ColorAdminController {

    @Autowired
    private ColorService colorService;

    @GetMapping("/list")
    public String viewPage(Model model){
        List<Color> list = colorService.findAllList();
        model.addAttribute("listcolor", list);
        model.addAttribute("color", new Color());
        return "admin/color.html";
    }

    @PostMapping("/save")
    public String save(Color color, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            colorService.saveOrUpdate(color);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu màu sắc thành công");
        } catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
            return "redirect:list";
        }
        return "redirect:list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes){
        try {
            colorService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
