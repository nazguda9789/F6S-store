package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.Api.PageView;
import com.example.sneaker.dto.PeopleApi.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeopleService {
  private final StaffRepository staff;
  private final CustomerRepository customers;
  private final CustomerAddressRepository addresses;
  private final PosOperatorRepository operators;

  @Value("${app.admin-username}")
  private String adminUsername;

  private String normalized(String value) {
    return value == null ? null : value.trim();
  }

  private <T> Specification<T> filter(String q, Boolean active, String... fields) {
    return (root, query, cb) -> {
      var list = new ArrayList<jakarta.persistence.criteria.Predicate>();
      if (active != null) list.add(cb.equal(root.get("active"), active));
      if (q != null && !q.isBlank()) {
        String value =
            "%"
                + q.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                    .replace("[", "\\[")
                + "%";
        var terms =
            Arrays.stream(fields)
                .map(f -> cb.like(cb.lower(root.get(f)), value, '\\'))
                .toArray(jakarta.persistence.criteria.Predicate[]::new);
        list.add(cb.or(terms));
      }
      return cb.and(list.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  public PageView<StaffView> staff(String q, Boolean active, int page) {
    var result =
        staff.findAll(
            filter(q, active, "fullName", "username", "email", "phone", "code"),
            PageRequest.of(page, 20, Sort.by("id").descending()));
    return new PageView<>(
        result.getContent().stream().map(this::staffView).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  public StaffView staffDetail(Long id) {
    return staffView(staff.findById(id).orElseThrow(() -> missing("Không tìm thấy nhân viên")));
  }

  private String hash(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw bad("Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8");
    return "{bcrypt}" + new BCryptPasswordEncoder().encode(password);
  }

  @Transactional
  public StaffView createStaff(StaffCreate input) {
    String username = input.username().trim().toLowerCase(Locale.ROOT);
    if (username.equalsIgnoreCase(adminUsername))
      throw bad("Tên đăng nhập dành riêng cho quản trị hệ thống");
    var s = new StaffAccount();
    s.setCode("NV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
    s.setUsername(username);
    s.setPasswordHash(hash(input.password()));
    apply(s, input.profile(), input.address());
    staff.saveAndFlush(s);
    if (!operators.existsById(username)) {
      var operator = new PosOperator();
      operator.setUsername(username);
      operators.save(operator);
    }
    return staffView(s);
  }

  @Transactional
  public StaffView updateStaff(Long id, StaffUpdate input) {
    var s = staff.findById(id).orElseThrow(() -> missing("Không tìm thấy nhân viên"));
    apply(s, input.profile(), input.address());
    staff.flush();
    return staffView(s);
  }

  @Transactional
  public void password(Long id, Password input) {
    staff
        .findById(id)
        .orElseThrow(() -> missing("Không tìm thấy nhân viên"))
        .setPasswordHash(hash(input.password()));
  }

  @Transactional
  public void disableStaff(Long id) {
    staff.findById(id).orElseThrow(() -> missing("Không tìm thấy nhân viên")).setActive(false);
  }

  private void apply(StaffAccount s, Profile p, String address) {
    if (p.email() == null || p.email().isBlank()) throw bad("Nhân viên cần email để liên hệ");
    s.setFullName(p.fullName().trim());
    s.setEmail(p.email().trim().toLowerCase(Locale.ROOT));
    s.setPhone(p.phone());
    s.setGender(p.gender());
    s.setBirthDate(p.birthDate());
    s.setActive(p.active());
    s.setAddress(normalized(address));
  }

  private StaffView staffView(StaffAccount s) {
    return new StaffView(
        s.getId(),
        s.getCode(),
        s.getUsername(),
        new Profile(
            s.getFullName(),
            s.getEmail(),
            s.getPhone(),
            s.getGender(),
            s.getBirthDate(),
            s.isActive()),
        s.getAddress(),
        s.getCreatedAt());
  }

  public PageView<CustomerView> customers(String q, Boolean active, int page) {
    var result =
        customers.findAll(
            filter(q, active, "fullName", "email", "phone", "code"),
            PageRequest.of(page, 20, Sort.by("id").descending()));
    return new PageView<>(
        result.getContent().stream().map(this::customerView).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  public CustomerView customer(Long id) {
    return customerView(
        customers.findById(id).orElseThrow(() -> missing("Không tìm thấy khách hàng")));
  }

  @Transactional
  public CustomerView createCustomer(Profile input) {
    var c = new Customer();
    c.setCode("KH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
    apply(c, input);
    customers.saveAndFlush(c);
    return customerView(c);
  }

  @Transactional
  public CustomerView updateCustomer(Long id, Profile input) {
    var c = customers.lock(id).orElseThrow(() -> missing("Không tìm thấy khách hàng"));
    apply(c, input);
    customers.flush();
    return customerView(c);
  }

  @Transactional
  public void disableCustomer(Long id) {
    customers.lock(id).orElseThrow(() -> missing("Không tìm thấy khách hàng")).setActive(false);
  }

  private void apply(Customer c, Profile p) {
    c.setFullName(p.fullName().trim());
    c.setEmail(
        p.email() == null || p.email().isBlank()
            ? null
            : p.email().trim().toLowerCase(Locale.ROOT));
    c.setPhone(p.phone());
    c.setGender(p.gender());
    c.setBirthDate(p.birthDate());
    c.setActive(p.active());
  }

  @Transactional
  public CustomerView saveAddress(Long customerId, Long addressId, AddressInput input) {
    var c = customers.lock(customerId).orElseThrow(() -> missing("Không tìm thấy khách hàng"));
    if (!c.isActive()) throw bad("Khách hàng đang ngừng hoạt động");
    var existing =
        addresses.findByCustomerIdAndActiveTrueOrderByDefaultAddressDescIdAsc(customerId);
    if (addressId == null && existing.size() >= 10)
      throw bad("Tối đa 10 địa chỉ đang dùng cho một khách");
    var address =
        addressId == null
            ? new CustomerAddress()
            : existing.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> missing("Địa chỉ không thuộc khách hàng này"));
    address.setCustomer(c);
    address.setRecipientName(input.recipientName().trim());
    address.setPhone(input.phone());
    address.setProvince(input.province().trim());
    address.setDistrict(input.district().trim());
    address.setWard(input.ward().trim());
    address.setDetail(input.detail().trim());
    boolean makeDefault =
        input.defaultAddress() || existing.isEmpty() || address.isDefaultAddress();
    if (makeDefault) existing.forEach(a -> a.setDefaultAddress(false));
    address.setDefaultAddress(makeDefault);
    addresses.saveAndFlush(address);
    return customerView(c);
  }

  @Transactional
  public CustomerView deleteAddress(Long customerId, Long addressId) {
    var c = customers.lock(customerId).orElseThrow(() -> missing("Không tìm thấy khách hàng"));
    var existing =
        addresses.findByCustomerIdAndActiveTrueOrderByDefaultAddressDescIdAsc(customerId);
    var address =
        existing.stream()
            .filter(a -> a.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> missing("Địa chỉ không thuộc khách hàng"));
    boolean wasDefault = address.isDefaultAddress();
    address.setActive(false);
    address.setDefaultAddress(false);
    if (wasDefault)
      existing.stream()
          .filter(a -> !a.getId().equals(addressId))
          .findFirst()
          .ifPresent(a -> a.setDefaultAddress(true));
    addresses.flush();
    return customerView(c);
  }

  private CustomerView customerView(Customer c) {
    return new CustomerView(
        c.getId(),
        c.getCode(),
        new Profile(
            c.getFullName(),
            c.getEmail(),
            c.getPhone(),
            c.getGender(),
            c.getBirthDate(),
            c.isActive()),
        c.getCreatedAt(),
        addresses.findByCustomerIdAndActiveTrueOrderByDefaultAddressDescIdAsc(c.getId()).stream()
            .map(
                a ->
                    new AddressView(
                        a.getId(),
                        a.getRecipientName(),
                        a.getPhone(),
                        a.getProvince(),
                        a.getDistrict(),
                        a.getWard(),
                        a.getDetail(),
                        a.isDefaultAddress(),
                        a.fullAddress()))
            .toList());
  }
}
