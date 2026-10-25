package com.example.sneaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class PeopleApi {
  private PeopleApi() {}

  public record Profile(
      @NotBlank @Size(max = 120) String fullName,
      @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "0[35789][0-9]{8}") String phone,
      @Pattern(regexp = "MALE|FEMALE|OTHER") String gender,
      @Past LocalDate birthDate,
      boolean active) {}

  public record StaffCreate(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String username,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull @Valid Profile profile,
      @Size(max = 500) String address) {}

  public record StaffUpdate(@NotNull @Valid Profile profile, @Size(max = 500) String address) {}

  public record Password(@NotBlank @Size(min = 12, max = 72) String password) {}

  public record StaffView(
      Long id, String code, String username, Profile profile, String address, Instant createdAt) {}

  public record AddressInput(
      @NotBlank @Size(max = 120) String recipientName,
      @NotBlank @Pattern(regexp = "0[35789][0-9]{8}") String phone,
      @NotBlank @Size(max = 100) String province,
      @NotBlank @Size(max = 100) String district,
      @NotBlank @Size(max = 100) String ward,
      @NotBlank @Size(max = 250) String detail,
      boolean defaultAddress) {}

  public record AddressView(
      Long id,
      String recipientName,
      String phone,
      String province,
      String district,
      String ward,
      String detail,
      boolean defaultAddress,
      String fullAddress) {}

  public record CustomerView(
      Long id, String code, Profile profile, Instant createdAt, List<AddressView> addresses) {}
}
