# Kiến trúc và model hiện hành

Ứng dụng Java 17/Spring Boot, Spring Security, JPA/Hibernate, Thymeleaf, Bootstrap/JavaScript và MySQL. Không có frontend Node riêng cần build khi chạy web.

```text
Browser → Controller / API → Service nghiệp vụ → Repository → MySQL
                      ↘ OTP/OAuth/VNPAY/vận chuyển/upload theo cấu hình
```

## Nhóm dữ liệu

| Nhóm | Model/bảng chính |
| --- | --- |
| Tài khoản/xác thực | users, otp_codes, pending_registration |
| Giày | product, product_detail, product_category |
| Thuộc tính | shoe_attribute (BRAND/SOLE/MATERIAL), category, color, size |
| Ảnh | product_image, product_color_image |
| Bán hàng | carts, order_draft, order_detail_draft, invoice, invoice_detail |
| Lịch sử | history_invoice_status, history_pay, product_view_history |
| Giảm giá | voucher, discount_product |
| Nội dung/hỗ trợ | banner, blog, contact, chatting |
| Cấu hình/địa chỉ | cloud_key, key_telegram, shopee_key, province, wards |
| Tương thích/kiểm toán | parent_category, catalog_variant_merge và các bảng catalog_release_* |

Schema ứng dụng có 33 bảng JPA; installer thêm bảng kỹ thuật cho release/demo. Nhân viên và khách nằm trong `users`, phân biệt ROLE_EMPLOYEE/ROLE_USER; không nhân đôi sang bảng tiếng Việt cũ. Bản cài sạch không nạp lại các bảng nghiệp vụ tiếng Việt hoặc giày nguồn cũ.

Một product là mẫu giày; product_detail là màu × size với SKU/tồn riêng. Mẫu có mã F6S-SP…, biến thể F6S-SKU… do server cấp. Các cột color/size và color_id/size_id tương thích vẫn được điền nhất quán. Không đổi SKU khi chỉ đổi tên giày.

customer_id của draft/invoice độc lập với user_id/created_by nhân viên tạo. Hóa đơn lưu snapshot tên/ảnh/màu/size/giá, không cần mở sản phẩm đang bán để đọc lịch sử. blog.archived tách bài lưu trữ khỏi trang công khai.

cash_received/cash_change lưu DECIMAL nguyên VND; tiền hàng/tổng phần lớn hệ thống vẫn theo model Double cũ. Chưa chuyển toàn bộ tiền sang BigDecimal hoặc có sổ kho/hoàn tiền đầy đủ; không xem đây là mô hình kế toán production.

## Mã nguồn và SQL

`src/main/java/com/web` chứa controller/api/service/repository/entity. `src/main/webapp` chứa template và asset. `sql/install` chỉ dành cài mới, không có routine. `sql/maintenance` dành database đang dùng và có routine nên chạy MySQL CLI.

Nguồn tạo SQL ở `scripts/sql-templates/` và `scripts/build-sports-catalog.cjs`; không lấy dump chứa dữ liệu người dùng hoặc secret để phát hành. Không dùng Hibernate ddl-auto thay cho backup/migration. [Cách chọn SQL](../sql/README.md).
