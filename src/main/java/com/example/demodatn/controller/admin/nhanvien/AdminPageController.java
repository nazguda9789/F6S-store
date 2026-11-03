package com.example.demodatn.controller.admin.nhanvien;
import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.dto.nhanvien.AccountProfileForm;
import com.example.demodatn.service.nhanvien.AdminAccountService;
import java.security.Principal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminPageController {
    private final AdminAccountService accounts;
    private void populate(Principal principal, Model model) {
        var account=accounts.user(principal.getName());
        model.addAttribute("account",account);
        model.addAttribute("accountProfile",accounts.profile(account));
        if(!model.containsAttribute("profileForm"))model.addAttribute("profileForm",accounts.profile(account));
        if(!model.containsAttribute("passwordForm"))model.addAttribute("passwordForm",new AccountPasswordForm());
    }
    @GetMapping("/admin/account")
    public String account(Principal principal, Model model) {
        populate(principal,model);return "admin/tai-khoan/tai-khoan";
    }
    @PostMapping("/admin/account/profile")
    public String profile(@Valid @ModelAttribute("profileForm") AccountProfileForm form, BindingResult errors,
            Principal principal,Model model,RedirectAttributes flash) {
        accounts.update(principal.getName(),form,errors);
        if(errors.hasErrors()){populate(principal,model);return "admin/tai-khoan/tai-khoan";}
        flash.addFlashAttribute("success","Đã cập nhật thông tin tài khoản.");return "redirect:/admin/account";
    }
    @PostMapping("/admin/account/password")
    public String password(@Valid @ModelAttribute("passwordForm") AccountPasswordForm form, BindingResult errors,
            Principal principal,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors())accounts.password(principal.getName(),form,errors);
        if(errors.hasErrors()) {populate(principal,model);return "admin/tai-khoan/tai-khoan";}
        flash.addFlashAttribute("success","Đổi mật khẩu thành công.");return "redirect:/admin/account";
    }
}
