package com.web.controller.admin;

import com.web.entity.ShoeAttribute;
import com.web.repository.ShoeAttributeRepository;
import com.web.repository.ProductImageRepository;
import com.web.service.ShoeAttributeService;
import com.web.utils.TypeSystem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(TypeSystem.ADMIN_URL)
public class ShoeModuleController {
    private final ShoeAttributeRepository attributes;
    private final ShoeAttributeService service;
    private final ProductImageRepository images;
    public ShoeModuleController(ShoeAttributeRepository attributes, ShoeAttributeService service, ProductImageRepository images) {
        this.attributes = attributes; this.service = service; this.images = images;
    }
    @GetMapping("/shoe-attributes")
    public String attributes(@RequestParam(defaultValue = "BRAND") ShoeAttribute.Kind kind,
                             @RequestParam(defaultValue = "") String search, Model model) {
        model.addAttribute("kind", kind);
        model.addAttribute("label", switch(kind) { case BRAND -> "Thương hiệu"; case SOLE -> "Đế giày"; case MATERIAL -> "Chất liệu"; });
        model.addAttribute("search", search);
        model.addAttribute("items", attributes.findByKindOrderByNameAsc(kind).stream()
                .filter(item -> item.getName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).toList());
        return "admin/shoe-attributes";
    }
    @PostMapping("/shoe-attributes/save")
    public String save(@RequestParam(required = false) Long id, @RequestParam ShoeAttribute.Kind kind,
                       @RequestParam String name, @RequestParam(defaultValue = "false") boolean active,
                       RedirectAttributes flash) {
        try { service.save(id, kind, name, active); flash.addFlashAttribute("notice", "Đã lưu thuộc tính giày."); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("errorMessage", e.getMessage()); }
        catch (DataIntegrityViolationException e) { flash.addFlashAttribute("errorMessage", "Tên đã tồn tại. Vui lòng tải lại danh sách."); }
        return "redirect:" + TypeSystem.ADMIN_URL + "/shoe-attributes?kind=" + kind.name();
    }
    @GetMapping("/image-library")
    public String gallery(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String search, Model model) {
        model.addAttribute("images", images.searchLibrary(search, PageRequest.of(Math.max(0, page), 24, Sort.by("id").descending())));
        model.addAttribute("search", search);
        return "admin/image-library";
    }
}
