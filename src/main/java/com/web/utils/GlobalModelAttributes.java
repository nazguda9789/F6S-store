package com.web.utils;

import com.web.dto.CustomUserDetails;
import com.web.entity.Banner;
import com.web.entity.Category;
import com.web.enums.BannerType;
import com.web.enums.InvoiceStatus;
import com.web.enums.Role;
import com.web.repository.InvoiceRepository;
import com.web.service.BannerService;
import com.web.service.CartService;
import com.web.service.CategoryService;
import com.web.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private final WebSocketSessionListener sessionListener;

    public GlobalModelAttributes(WebSocketSessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request, @AuthenticationPrincipal Object principal, HttpSession session) {
        model.addAttribute("googleLoginAvailable", com.web.config.OAuthAvailabilityFilter.available(environment,"google"));
        model.addAttribute("facebookLoginAvailable", com.web.config.OAuthAvailabilityFilter.available(environment,"facebook"));
        List<Category> listParents = categoryService.getParentCategoriesUser();
        model.addAttribute("categories", listParents);
        String currentUrl = request.getRequestURI();

        if(!currentUrl.contains(TypeSystem.ADMIN_URL)){
            List<Banner> bannerSlider = bannerService.findByPageNameAndType(currentUrl, BannerType.SLIDER);
            model.addAttribute("bannerSlider", bannerSlider);
            List<Banner> bannerNormal = bannerService.findByPageNameAndType(currentUrl, BannerType.NORMAL);
            model.addAttribute("bannerNormal", bannerNormal);
        }
        if (principal instanceof OAuth2User oauthUser) {
            model.addAttribute("fullNameheader", oauthUser.getAttribute("name"));
        }
        else if (principal instanceof CustomUserDetails userDetails) {
            model.addAttribute("fullNameheader", userDetails.getFullName());
        }

        if(userUtils.getRoleLogged() != null && userUtils.getRoleLogged().equals(Role.ROLE_ADMIN.toString())){
            model.addAttribute("cartQuantity", 0);
        }
        else {
            model.addAttribute("cartQuantity", cartService.getQuantityCart(session));
        }

        if(currentUrl.contains(TypeSystem.ADMIN_URL) || currentUrl.contains(TypeSystem.EMPLOYEE_URL)){
            model.addAttribute("numberNewContact", ContactService.newCounter);
            model.addAttribute("onlineUsers", sessionListener.getOnlineUsers());
            model.addAttribute("donHangMoi", invoiceRepository.donHangMoi());
        }
        model.addAttribute("userLogged", userUtils.getUserWithAuthority());
    }
}
