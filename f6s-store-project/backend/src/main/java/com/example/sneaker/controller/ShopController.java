package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ShopController {
  private final CatalogService catalog;
  private final CheckoutService checkout;

  @GetMapping("/products")
  public PageView<ProductView> products(
      @RequestParam(required = false) Long category,
      @RequestParam(required = false) String brand,
      @RequestParam(required = false) @DecimalMin("0") BigDecimal min,
      @RequestParam(required = false) @DecimalMin("0") BigDecimal max,
      @RequestParam(required = false) String size,
      @RequestParam(required = false) String color,
      @RequestParam(required = false) @Size(max = 180) String q,
      @RequestParam(required = false) Boolean featured,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "12") @Min(1) @Max(48) int limit) {
    return catalog.search(category, brand, min, max, size, color, q, featured, sort, page, limit);
  }

  @GetMapping("/products/{id}")
  public ProductView detail(@PathVariable Long id) {
    return catalog.detail(id);
  }

  @GetMapping("/categories")
  public List<CategoryView> categories() {
    return catalog.categoryList();
  }

  @GetMapping("/facets")
  public Map<String, List<String>> facets() {
    return catalog.facets();
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public OrderView order(@Valid @RequestBody Checkout input) {
    return checkout.checkout(input);
  }
}
