package com.example.demodatn.dto.nhanvien;
import com.example.demodatn.util.PasswordPolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @jakarta.validation.constraints.Pattern(regexp="[A-Za-z0-9_.-]{3,50}",message="Tên đăng nhập gồm 3–50 chữ, số hoặc dấu . _ -")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @jakarta.validation.constraints.Pattern(regexp=PasswordPolicy.REGEX,message=PasswordPolicy.MESSAGE)
    private String password;
    
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    @NotBlank(message="Nhập thư điện tử")
    @jakarta.validation.constraints.Size(max=100)
    @Email(message = "Thư điện tử không hợp lệ")
    private String email;
    
    @NotBlank(message = "SĐT không được để trống")
    @jakarta.validation.constraints.Pattern(regexp="(?:0[0-9]{9}|\\+84[0-9]{9})",message="SĐT gồm 10 số bắt đầu bằng 0 hoặc +84 và 9 số")
    private String phone;
}
