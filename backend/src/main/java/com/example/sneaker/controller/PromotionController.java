package com.example.sneaker.controller;

import com.example.sneaker.dto.PromotionApi.*;
import com.example.sneaker.service.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
public class PromotionController {
  private final PromotionService service;

  @GetMapping
  public List<View> list() {
    return service.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public View create(@Valid @RequestBody Input input) {
    return service.create(input);
  }

  @PutMapping("/{id}")
  public View update(@PathVariable Long id, @Valid @RequestBody Input input) {
    return service.update(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disable(@PathVariable Long id) {
    service.disable(id);
  }
}
