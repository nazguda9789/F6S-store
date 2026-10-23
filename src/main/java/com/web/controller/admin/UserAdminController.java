package com.web.controller.admin;

import com.web.dto.Result;
import com.web.entity.Category;
import com.web.entity.User;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.service.CategoryService;
import com.web.service.UserService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL+ "/user")
public class UserAdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public String viewPage(Model model, @RequestParam(required = false) Role role,
                           @RequestParam(required = false) Boolean active,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                           @RequestParam(required = false)
                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(defaultValue = "") String search, Pageable pageable){
        if (size < 1 || size > 100) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Số dòng phải từ 1 đến 100");
        pageable = PageRequest.of(pageable.getPageNumber(), size, Sort.by("id").descending());
        Page<User> page = userService.getAllUser(role, from, to, search, active, pageable);
        model.addAttribute("listUser", page.getContent());
        model.addAttribute("page", page);

        model.addAttribute("role", role);
        model.addAttribute("active", active);
        model.addAttribute("accountTitle", role == Role.ROLE_EMPLOYEE ? "Danh sách nhân viên" : role == Role.ROLE_USER ? "Danh sách khách hàng" : "Danh sách tài khoản");
        model.addAttribute("addAccountLabel", role == Role.ROLE_EMPLOYEE ? "Thêm nhân viên" : role == Role.ROLE_USER ? "Thêm khách hàng" : "Thêm tài khoản");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("search", search);
        return "admin/taikhoan.html";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, @RequestParam(required = false) Long id, @RequestParam(required = false) Role role) {
        User user = (id != null)
                ? userService.findById(id).orElse(null)
                : new User();
        if (id == null && role != null) user.setRole(role);
        if (id == null) user.setActived(true);

        if (id != null && user == null) {
            model.addAttribute("error", "Không tìm thấy user với ID: " + id);
            user = new User();
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", Arrays.asList(Role.values()));
        model.addAttribute("types", Arrays.asList(UserType.values()));
        return "admin/addUser";
    }

    @PostMapping("/create")
    public String createOrUpdateUser(
            @ModelAttribute("user") User user,
            Model model,
            RedirectAttributes redirectAttributes) {

        Result<User> result;

        if (user.getId() == null) {
            // thêm mới
            result = userService.addUser(user);
        } else {
            // cập nhật
            result = userService.updateUser(user.getId(), user);
        }

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, result.getMessage());
            return "redirect:list" + (user.getRole() == null ? "" : "?role=" + user.getRole().name());
        } else {
            // lỗi → trả lại form cũ và giữ lại giá trị nhập
            model.addAttribute("user", user);
            model.addAttribute("roles", Arrays.asList(Role.values()));
            model.addAttribute("types", Arrays.asList(UserType.values()));
            model.addAttribute(TypeSystem.ERROR, result.getMessage());
            return "admin/addUser";
        }
    }

    @GetMapping("/delete")
    public String deleteUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Result<Void> result = userService.deleteUser(id);
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute(TypeSystem.SUCCESS, result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute(TypeSystem.ERROR, result.getMessage());
        }
        return "redirect:list";
    }
}
