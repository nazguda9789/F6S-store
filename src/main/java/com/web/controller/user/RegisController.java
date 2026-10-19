package com.web.controller.user;

import com.web.dto.UserRegis;
import com.web.entity.Category;
import com.web.exception.MessageException;
import com.web.repository.UserRepository;
import com.web.service.UserService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("userRegis")) {
            model.addAttribute("userRegis", new UserRegis());
        }
        return "user/regis";
    }

    @PostMapping("/register")
    public String register(UserRegis userRegis, RedirectAttributes redirectAttributes, HttpServletRequest request, Model model) {
        try {
            userService.registerUser(userRegis.getEmail(), userRegis.getPassword(), userRegis.getFullName(), userRegis.getPhone(), userRegis.getRePassword());
            return verificationRedirect(userRegis.getEmail());
        } catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
            redirectAttributes.addFlashAttribute("userRegis", userRegis);
            userRegis.setPassword(null);
            userRegis.setRePassword(null);
            return "redirect:/register";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyView() {
        return "user/verifyOtp";
    }

    @PostMapping("/verify-otp")
    public String verifyAction(@RequestParam String otp, String email, RedirectAttributes redirectAttributes, HttpServletRequest request, Model model) {
        try {
            userService.verifyOtp(email, otp);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS,"Xác thực tài khoản thành công");
            return "redirect:/login";
        }
        catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
            return verificationRedirect(email);
        }
    }

    @PostMapping("/new-otp-verify")
    public String sendNewOtp(@RequestParam String email, RedirectAttributes redirectAttributes, HttpServletRequest request, Model model) {
        try {
            userService.sendNewOtp(email);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS,"Đã gửi mã xác thực mới cho bạn");
        }
        catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
        }
        return verificationRedirect(email);
    }

    private String verificationRedirect(String email) {
        return "redirect:/verify-otp?email=" + java.net.URLEncoder.encode(email == null ? "" : email.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

}
