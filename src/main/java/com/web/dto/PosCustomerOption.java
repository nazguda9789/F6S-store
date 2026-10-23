package com.web.dto;

/** Minimal staff-facing contact data; never serialize the User entity for the picker. */
public record PosCustomerOption(Long id, String fullName, String email, String phone, String address) {}
