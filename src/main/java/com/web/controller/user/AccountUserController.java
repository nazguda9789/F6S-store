package com.web.controller.user;

import com.web.dto.InvoiceUserResponse;
import com.web.dto.Result;
import com.web.entity.Invoice;
import com.web.entity.User;
import com.web.repository.UserRepository;
import com.web.service.InvoiceService;
import com.web.service.UserService;
import com.web.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AccountUserController {

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private UserService userService;

    @GetMapping({"/tai-khoan"})
    public String view(Model model){
        User user = userUtils.getUserWithAuthority();
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("phone", user.getPhone());
        model.addAttribute("email", user.getEmail());
        List<InvoiceUserResponse> invoices = invoiceService.findInvoiceByUserId(user.getId());
        model.addAttribute("InvoiceUserResponse", invoices);
        return "user/account";
    }


    @PostMapping("/update-infor")
    public String updateAccount(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            RedirectAttributes redirectAttributes) {
        Result<?> result = userService.changeInfor(fullName, phone);
        if(result.isSuccess() == true){
            redirectAttributes.addFlashAttribute("successChange", "Cập nhật thông tin thành công!");
        }
        else{
            redirectAttributes.addFlashAttribute("errorChange", result.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @PostMapping("/update-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        Result<?> result = userService.changePassword(oldPassword, newPassword, confirmPassword);
        if(result.isSuccess() == true){
            redirectAttributes.addFlashAttribute("successChange", result.getMessage());
        }
        else{
            redirectAttributes.addFlashAttribute("errorChange", result.getMessage());
        }
        return "redirect:/tai-khoan";
    }

}
