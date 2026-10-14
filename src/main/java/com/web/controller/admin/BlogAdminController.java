package com.web.controller.admin;

import com.web.entity.Blog;
import com.web.entity.Category;
import com.web.exception.MessageException;
import com.web.service.BlogService;
import com.web.service.CategoryService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/blog")
public class BlogAdminController {

    @Autowired
    private BlogService blogService;

    @GetMapping("/list")
    public String viewPage(Model model){
        model.addAttribute("totalBlog", blogService.totalBlog());
        return "admin/blog.html";
    }

    @GetMapping("/save")
    public String viewPageAdd(Model model, @RequestParam(required = false) Long id){
        if(id == null){
            model.addAttribute("blog", new Blog());
        }
        else{
            try {
                Blog blog = blogService.findById(id);
                model.addAttribute("blog", blog);
            }catch (MessageException e){
                return "redirect:list";
            }
        }
        return "admin/addblog.html";
    }

    @PostMapping("/save")
    public String saveCategory(Blog blog, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            blogService.saveOrUpdate(blog);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu bài viết thành công");
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
            blogService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
