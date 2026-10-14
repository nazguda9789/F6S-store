package com.web.controller.admin;

import com.web.entity.Category;
import com.web.entity.Voucher;
import com.web.exception.MessageException;
import com.web.service.CategoryService;
import com.web.service.VoucherService;
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

import java.sql.Date;
import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/voucher")
public class VoucherAdminController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/list")
    public String viewPage(Model model){
        List<Voucher> list = voucherService.findAll();
        model.addAttribute("listvoucher", list);
        return "admin/voucher.html";
    }

    @GetMapping("/save")
    public String viewPageAdd(Model model, @RequestParam(required = false) Long id){
        if(id == null){
            model.addAttribute("voucher", new Voucher());
        }
        else{
            try {
                Voucher voucher = voucherService.findById(id).get();
                model.addAttribute("voucher", voucher);
            }catch (MessageException e){
                return "redirect:list";
            }
        }
        return "admin/addvoucher.html";
    }

    @PostMapping("/save")
    public String saveCategory(Voucher voucher, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            voucherService.saveOrUpdate(voucher);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Lưu khuyến mại thành công");
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
            voucherService.delete(id);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Xóa thành công");
        }
        catch (Exception e){
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, "Xóa thất bại");
        }
        return "redirect:list";
    }
}
