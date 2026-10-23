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
                             @RequestParam(defaultValue = "") String search, @RequestParam(required=false) Boolean active,
                             @RequestParam(defaultValue="0") int page, Model model) {
        if (page < 0 || search.length() > 100) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Bộ lọc không hợp lệ");
        model.addAttribute("kind", kind);
        model.addAttribute("label", switch(kind) { case BRAND -> "Thương hiệu"; case SOLE -> "Đế giày"; case MATERIAL -> "Chất liệu"; });
        model.addAttribute("search", search);
        org.springframework.data.jpa.domain.Specification<ShoeAttribute> spec = (root,query,cb) -> {
            var filter=cb.and(cb.equal(root.get("kind"),kind),cb.or(cb.like(cb.lower(root.get("name")),"%"+search.strip().toLowerCase(java.util.Locale.ROOT)+"%"),cb.like(cb.lower(root.get("code")),"%"+search.strip().toLowerCase(java.util.Locale.ROOT)+"%")));
            return active == null ? filter : cb.and(filter,cb.equal(root.get("active"),active));
        };
        var results=attributes.findAll(spec,PageRequest.of(page,10,Sort.by("name").ascending()));
        model.addAttribute("items",results.getContent()); model.addAttribute("resultPage",results);model.addAttribute("active",active);
        return "admin/shoe-attributes";
    }
    @PostMapping("/shoe-attributes/save")
    public String save(@RequestParam(required = false) Long id, @RequestParam ShoeAttribute.Kind kind,
                       @RequestParam String name, @RequestParam(defaultValue = "false") boolean active,
                       @RequestParam(required=false) String description,
                       RedirectAttributes flash) {
        try { service.save(id, kind, name, active, description); flash.addFlashAttribute("notice", "Đã lưu thuộc tính giày."); }
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
