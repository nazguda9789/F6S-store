package com.example.demodatn.controller;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(basePackages = "com.example.demodatn")
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    
    private final CategoryRepository categoryRepository;

    @ModelAttribute("storeAccountName")
    public String accountName(java.security.Principal principal) {
        return principal == null ? null : principal.getName();
    }

    @ModelAttribute("storeAdmin")
    public boolean storeAdmin(org.springframework.security.core.Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    
    @ModelAttribute("globalCategories")
    public Object getGlobalCategories() {
        return categoryRepository.findAll();
    }

    /** Tong so luong san pham trong gio (cung cong thuc voi JSON totalQuantity cua /cart/add-ajax). */
    @ModelAttribute("cartCount")
    public int getCartCount(HttpSession session) {
        Object cart = session.getAttribute("cart");
        if (!(cart instanceof List<?> items)) return 0;
        int total = 0;
        for (Object o : items) {
            if (o instanceof CartItemDto item && item.getQuantity() != null) total += item.getQuantity();
        }
        return total;
    }
}
