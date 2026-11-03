package com.example.demodatn.controller.auth;
import com.example.demodatn.dto.nhanvien.RegisterRequest;
import com.example.demodatn.service.nhanvien.AuthService;
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
public class AuthController {
    
    private final AuthService authService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/dang-ky";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request, 
                                  BindingResult bindingResult, 
                                  Model model, 
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/dang-ky";
        }
        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("error", "Username, thư điện tử hoặc số điện thoại vừa được sử dụng. Vui lòng kiểm tra lại.");
            return "auth/dang-ky";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/dang-ky";
        }
    }
}
