package com.web.controller.admin;

import com.web.entity.Contact;
import com.web.entity.User;
import com.web.enums.Role;
import com.web.service.ContactService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/contact")
public class ContactAdminController {

    @Autowired
    private ContactService contactService;

    @GetMapping("/list")
    public String viewPage(Model model,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(defaultValue = "") String search, Pageable pageable){
        pageable = PageRequest.of(pageable.getPageNumber(), size, Sort.by("id").descending());
        Page<Contact> page = contactService.findAllByAdmin(from, to, search, pageable);
        model.addAttribute("listContact", page.getContent());
        model.addAttribute("page", page);

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("search", search);
        return "admin/contact.html";
    }

    @GetMapping(value = {"/delete"})
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        contactService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Xóa liên hệ thành công!");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
