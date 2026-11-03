package com.example.demodatn.dto.nhanvien;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AccountProfileForm {
    @NotBlank(message="Nhập họ tên.") @Size(max=100, message="Họ tên tối đa 100 ký tự.")
    private String fullName;
    @NotBlank(message="Nhập thư điện tử.") @Email(message="Thư điện tử không đúng định dạng.") @Size(max=254)
    private String email;
    @NotBlank(message="Nhập số điện thoại.")
    @Pattern(regexp="(?:0[0-9]{9}|\\+84[0-9]{9})", message="SĐT gồm 10 chữ số bắt đầu bằng 0 hoặc +84 và 9 chữ số.")
    private String phone;
}
