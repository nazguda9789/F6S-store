package com.web.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LoginController {

    @GetMapping({"/login"})
    public String productView(HttpSession session, Model model){
        String error = (String) session.getAttribute("loginError");
        if(error != null) {
            model.addAttribute("errorMessage", error);
            session.removeAttribute("loginError");
        }
        return "user/login";
    }
}
