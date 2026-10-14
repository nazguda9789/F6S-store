package com.web.controller.user;

import com.web.entity.Contact;
import com.web.service.ContactService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    // Trang form liên hệ
    @GetMapping("/contact")
    public String showContactForm(Model model) {
        model.addAttribute("contact", new Contact());
        return "user/contact";
    }

    // Xử lý submit form
    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contact") Contact contact, BindingResult result, HttpServletRequest request,RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "user/contact";
        }
        contact.setIpAddress(request.getRemoteAddr());
        contactService.save(contact);
        redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, "Gửi liên hệ thành công! Chúng tôi sẽ sớm phản hồi lại cho bạn");
        return "redirect:/contact";
    }

}
