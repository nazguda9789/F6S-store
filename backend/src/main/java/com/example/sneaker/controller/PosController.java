package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.OrderView;
import com.example.sneaker.dto.PosApi.*;
import com.example.sneaker.service.PosService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pos/drafts")
@RequiredArgsConstructor
public class PosController {
  private final PosService pos;

  @PutMapping("/{id}/customer")
  public Draft customer(
      @PathVariable Long id, Principal user, @Valid @RequestBody CustomerSelection input) {
    return pos.customer(id, user.getName(), input.customerId());
  }

  @GetMapping
  public List<Draft> list(Principal user) {
    return pos.list(user.getName());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Draft create(Principal user, @Valid @RequestBody NewDraft input) {
    return pos.create(user.getName(), input);
  }

  @PostMapping("/{id}/scan")
  public Draft scan(@PathVariable Long id, Principal user, @Valid @RequestBody Scan input) {
    return pos.scan(id, user.getName(), input);
  }

  @PutMapping("/{id}/lines/{variantId}")
  public Draft quantity(
      @PathVariable Long id,
      @PathVariable Long variantId,
      Principal user,
      @Valid @RequestBody Quantity input) {
    return pos.quantity(id, variantId, user.getName(), input);
  }

  @DeleteMapping("/{id}/lines/{variantId}")
  public Draft remove(@PathVariable Long id, @PathVariable Long variantId, Principal user) {
    return pos.remove(id, variantId, user.getName());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long id, Principal user) {
    pos.cancel(id, user.getName());
  }

  @PostMapping("/{id}/quote")
  public Quote quote(@PathVariable Long id, Principal user, @Valid @RequestBody QuoteInput input) {
    return pos.quote(id, user.getName(), input.promotionCode());
  }

  @PostMapping("/{id}/checkout")
  public OrderView checkout(
      @PathVariable Long id, Principal user, @Valid @RequestBody Payment input) {
    return pos.pay(id, user.getName(), input);
  }

  @GetMapping("/{id}/receipt")
  public OrderView receipt(@PathVariable Long id, Principal user) {
    return pos.receipt(id, user.getName());
  }
}
