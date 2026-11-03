package com.example.demodatn.dto.donhang;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CheckoutRequest {
    private Integer shippingAddressId;
    @NotBlank(message = "Vui lòng nhập họ tên người nhận")
    @Size(max=100, message="Tên người nhận tối đa 100 ký tự.")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp="(?:0[0-9]{9}|\\+84[0-9]{9})", message="Số điện thoại gồm 10 số bắt đầu bằng 0 hoặc +84 và 9 số.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập thư điện tử")
    @Email(message="Email không hợp lệ.") @Size(max=100)
    private String email;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    @Size(min=10,max=255,message="Địa chỉ giao hàng từ 10 đến 255 ký tự.")
    private String shippingAddress;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp="COD|BANK_TRANSFER",message="Chọn COD hoặc chuyển khoản ngân hàng.")
    private String paymentMethod;

    @Size(max=50) private String voucherCode;
    private String checkoutToken;
}
