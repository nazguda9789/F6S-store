package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.PageView;
import com.example.sneaker.dto.PeopleApi.*;
import com.example.sneaker.service.PeopleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class PeopleController {
  private final PeopleService service;

  @GetMapping("/staff")
  public PageView<StaffView> staff(
      @RequestParam(required = false) @Size(max = 120) String q,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return service.staff(q, active, page);
  }

  @GetMapping("/staff/{id}")
  public StaffView staff(@PathVariable Long id) {
    return service.staffDetail(id);
  }

  @PostMapping("/staff")
  @ResponseStatus(HttpStatus.CREATED)
  public StaffView create(@Valid @RequestBody StaffCreate input) {
    return service.createStaff(input);
  }

  @PutMapping("/staff/{id}")
  public StaffView staff(@PathVariable Long id, @Valid @RequestBody StaffUpdate input) {
    return service.updateStaff(id, input);
  }

  @PutMapping("/staff/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(@PathVariable Long id, @Valid @RequestBody Password input) {
    service.password(id, input);
  }

  @DeleteMapping("/staff/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disableStaff(@PathVariable Long id) {
    service.disableStaff(id);
  }

  @GetMapping("/customers")
  public PageView<CustomerView> customers(
      @RequestParam(required = false) @Size(max = 120) String q,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return service.customers(q, active, page);
  }

  @GetMapping("/customers/{id}")
  public CustomerView customer(@PathVariable Long id) {
    return service.customer(id);
  }

  @PostMapping("/customers")
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerView customer(@Valid @RequestBody Profile input) {
    return service.createCustomer(input);
  }

  @PutMapping("/customers/{id}")
  public CustomerView customer(@PathVariable Long id, @Valid @RequestBody Profile input) {
    return service.updateCustomer(id, input);
  }

  @DeleteMapping("/customers/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disableCustomer(@PathVariable Long id) {
    service.disableCustomer(id);
  }

  @PostMapping("/customers/{id}/addresses")
  public CustomerView address(@PathVariable Long id, @Valid @RequestBody AddressInput input) {
    return service.saveAddress(id, null, input);
  }

  @PutMapping("/customers/{id}/addresses/{addressId}")
  public CustomerView address(
      @PathVariable Long id, @PathVariable Long addressId, @Valid @RequestBody AddressInput input) {
    return service.saveAddress(id, addressId, input);
  }

  @DeleteMapping("/customers/{id}/addresses/{addressId}")
  public CustomerView address(@PathVariable Long id, @PathVariable Long addressId) {
    return service.deleteAddress(id, addressId);
  }
}
