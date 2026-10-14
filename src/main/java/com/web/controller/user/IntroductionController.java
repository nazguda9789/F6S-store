package com.web.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IntroductionController {

    @GetMapping({"/gioi-thieu"})
    public String homeView(Model model){

        return "user/gioithieu";
    }
}
