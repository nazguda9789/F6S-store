package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
  private final CatalogService catalog;
  private final com.example.sneaker.service.VariantMatrixService matrix;

  @GetMapping("/session")
  public java.util.Map<String, String> session(java.security.Principal user) {
    return java.util.Map.of("username", user.getName());
  }

  @PostMapping("/products/matrix/preview")
  public java.util.List<VariantInput> preview(
      @Valid @RequestBody com.example.sneaker.dto.MatrixApi.Selection input) {
    return matrix.preview(input);
  }

  @PostMapping("/products/matrix")
  @ResponseStatus(HttpStatus.CREATED)
  public ProductView matrix(@Valid @RequestBody com.example.sneaker.dto.MatrixApi.Create input) {
    return matrix.create(input);
  }

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public ProductView create(@Valid @RequestBody ProductInput input) {
    return catalog.create(input);
  }

  @PutMapping("/products/{id}")
  public ProductView update(@PathVariable Long id, @Valid @RequestBody ProductInput input) {
    return catalog.update(id, input);
  }

  @DeleteMapping("/products/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    catalog.delete(id);
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryView category(@Valid @RequestBody CategoryInput input) {
    return catalog.createCategory(input);
  }

  @PutMapping("/categories/{id}")
  public CategoryView categoryUpdate(
      @PathVariable Long id, @Valid @RequestBody CategoryInput input) {
    return catalog.updateCategory(id, input);
  }

  @DeleteMapping("/categories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void categoryDelete(@PathVariable Long id) {
    catalog.deleteCategory(id);
  }
}
