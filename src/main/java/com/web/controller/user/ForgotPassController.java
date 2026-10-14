package com.web.controller.user;

import com.web.dto.Result;
import com.web.dto.UserRegis;
import com.web.exception.MessageException;
import com.web.service.UserService;
import com.web.utils.TypeSystem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPassController {

    @Autowired
    private UserService userService;

    @GetMapping("/forgot")
    public String registerForm(Model model) {
        return "user/forgot";
    }

    @PostMapping("/forgot")
    public String forgotForm(@RequestParam String email, Model model, RedirectAttributes redirectAttributes) {
        Result<Void> result = userService.sendMailForgotPass(email);
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS,result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, result.getMessage());
        }
        return "redirect:/forgot";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String resetPass(@RequestParam String email, @RequestParam String key,Model model) {
        Boolean check = userService.checkResetPass(email, key);
        if(check == false){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "");
        }
        return "user/resetpassword";
    }


    @PostMapping("/dat-lai-mat-khau")
    public String resetPassPost(@RequestParam String email, @RequestParam String key,
                                @RequestParam String password, @RequestParam String repassword, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            userService.resetPassword(email, key, password, repassword);
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS,"Đặt lại mật khẩu thành công");
        }
        catch (MessageException e) {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, e.getDefaultMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + referer;
        }
        return "redirect:/login";
    }
}
