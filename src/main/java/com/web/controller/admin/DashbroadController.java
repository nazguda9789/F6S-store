package com.web.controller.admin;

import com.web.entity.Category;
import com.web.utils.TypeSystem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/dashbroad")
public class DashbroadController {

    @GetMapping("/view")
    public String viewPage(Model model){
        return "admin/index.html";
    }
}
