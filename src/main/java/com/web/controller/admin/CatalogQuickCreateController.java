package com.web.controller.admin;

import com.web.exception.MessageException;
import com.web.service.CatalogQuickCreateService;
import com.web.utils.TypeSystem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Non-/api admin path intentionally retains administrator-only URL authorization. */
@RestController
@RequestMapping(TypeSystem.ADMIN_URL + "/shoe-attributes")
public class CatalogQuickCreateController {
    private final CatalogQuickCreateService service;
    public CatalogQuickCreateController(CatalogQuickCreateService service) { this.service=service; }

    @PostMapping("/quick-create")
    public ResponseEntity<CatalogQuickCreateService.Option> create(@RequestBody CatalogQuickCreateService.Request request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @ExceptionHandler({IllegalArgumentException.class,MessageException.class,DataIntegrityViolationException.class})
    public ResponseEntity<Map<String,String>> invalid(RuntimeException error) {
        String message=error instanceof MessageException value ? value.getDefaultMessage() : error.getMessage();
        if(error instanceof DataIntegrityViolationException) message="Tên hoặc mã đã tồn tại. Tải lại danh sách và kiểm tra trước khi thêm.";
        return ResponseEntity.badRequest().body(Map.of("message",message==null ? "Dữ liệu thuộc tính không hợp lệ." : message));
    }
}
