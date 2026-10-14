package com.web.controller.employee;

import com.web.utils.TypeSystem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(TypeSystem.EMPLOYEE_URL+ "/chat")
public class ChatEmployeeController {

    @GetMapping("/list")
    public String viewPage(Model model){
        return "employee/chat.html";
    }
}
