package com.example.demodatn.dto.sanpham;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InventoryForm {
    @NotNull(message="Chọn biến thể.") private Integer variantId;
    @NotNull(message="Chọn cách điều chỉnh.") @Pattern(regexp="ADD|SET",message="Cách điều chỉnh không hợp lệ.") private String mode = "ADD";
    @NotNull(message="Nhập số lượng.") @Min(value=0,message="Số lượng không được âm.") private Integer quantity;
    @NotBlank(message="Nhập lý do điều chỉnh.") @jakarta.validation.constraints.Size(max=500,message="Lý do tối đa 500 ký tự.") private String reason;
    @NotNull(message="Tải lại trang để lấy tồn kho mới nhất.") private Integer expectedQuantity;
}

