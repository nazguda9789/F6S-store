package com.web.controller.user;

import com.web.dto.CartResponse;
import com.web.dto.ProductView;
import com.web.service.CartService;
import com.web.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping({"/cart"})
    public String cartView(Model model, HttpServletRequest request, HttpSession session){
        CartResponse cartResponse = cartService.myCart(session);
        model.addAttribute("cartResponse",cartResponse);
        Page<ProductView> productViewPage = productService.getSanPhamBanChay();
        model.addAttribute("sanPhamBanChay", productViewPage.getContent());
        return "user/cart";
    }
}
