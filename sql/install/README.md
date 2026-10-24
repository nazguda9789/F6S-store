# Cài mới bằng phpMyAdmin — bản tối thiểu

Thay cho file all-in-one có stored procedure. Bộ này chỉ dùng SQL thường, không `CALL`, `DELIMITER`, `PREPARE` hoặc SELECT trả trạng thái; dùng `FOREIGN_KEY_CHECKS = 0/1`.

## Khi vừa gặp lỗi #2014

1. Dừng import file cũ. Làm mới phpMyAdmin hoặc đăng xuất/đăng nhập lại để dùng kết nối mới. Nếu lỗi còn ngay trên kết nối mới, khởi động lại **Apache** trong XAMPP.
2. Không nhập đè, không xóa database `f6s_shop_demo` đã nhập dở nếu chưa backup. Bộ mới tạo database khác: **`f6s_shop_clean_demo`**.
3. Giải nén ZIP mới. Import từng file theo thứ tự, chờ file trước thành công rồi mới chọn file sau. Không bật tiếp tục bỏ qua lỗi.

| Thứ tự | File | Nội dung |
| --- | --- | --- |
| 1 | `01_schema.sql` | Tạo database mới và toàn bộ bảng; không DROP database |
| 2 | `02_catalog_admin.sql` | Admin local + 25 giày/250 biến thể + thuộc tính/banner/bài viết |
| 3, tùy chọn | `03_demo_optional.sql` | 5 nhân viên, 5 khách, voucher/giỏ/đơn và dữ liệu demo nghiệp vụ |

**Chỉ 01 + 02 là đủ để chạy web với catalog.** File 03 không bắt buộc, các hóa đơn giả của nó sẽ ảnh hưởng báo cáo trong database demo. Không cần nhập bất kỳ SQL cũ hoặc file maintenance nào.

Nếu database `f6s_shop_clean_demo` đã tồn tại, dừng lại. Chọn tên database mới và sửa tên ở CREATE DATABASE/USE trong **cả ba file**; không thêm IF NOT EXISTS để nhập chồng lên dữ liệu. Các file seed chỉ dành cho database trống vừa tạo; không chạy lại một bước đã thành công và không dùng `--force`.

## Chạy web

JDK 17 + MySQL/MariaDB phù hợp + Maven Wrapper. Trong IntelliJ Run Configuration → Environment variables:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/f6s_shop_clean_demo
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=
```

Thay cổng/tài khoản/mật khẩu bằng cấu hình MySQL của máy. Chạy `ShopgiaythethaoApplication` hoặc `mvnw.cmd spring-boot:run`, mở `http://localhost:8080`.

Admin local: `admin@gmail.com` / `123123`. Đổi mật khẩu trước khi công khai. Tài khoản nhân viên/khách trong file 03 đang ngừng hoạt động, chưa có mật khẩu chung; admin đặt mật khẩu và bật từng tài khoản nếu cần.

Giữ nguyên biến môi trường/secret OTP, Google và VNPAY đang có. SQL không tạo secret, OTP hoặc biên lai VNPAY giả. Không đổi database của website thật sang demo nếu muốn tiếp tục sử dụng tài khoản/OTP/hóa đơn thật. Xem `sql/maintenance/` cho trường hợp giữ database hiện tại.

## Về thông báo lỗi

Bộ ba file đã được kiểm tra trên **MySQL 8.4.11** và **MariaDB 10.4.34**, mỗi file chạy bằng một kết nối mới. Kiểm tra đủ bảng/catalog/demo, không có routine, chặn nhập sai thứ tự và chặn chạy lại bước đã hoàn tất. Web Java đã chạy với riêng 01 + 02 trên MySQL và `ddl-auto=validate`; chưa kiểm thử trực tiếp đúng bản phpMyAdmin Windows hoặc Java runtime trên mọi phiên bản MariaDB.

`#2014 Commands out of sync` là lỗi trạng thái kết nối/driver còn kết quả chưa xử lý, không phải bằng chứng rằng tất cả bảng đã import sai. File cũ có CALL trả kết quả; đây là nguồn lỗi có thể xảy ra khi phpMyAdmin xử lý import. MySQL thử nghiệm chấp nhận `SET FOREIGN_KEY_CHECKS=ON`, nhưng parser của phpMyAdmin có thể cảnh báo riêng về ON. File mới dùng số 0/1 và bỏ hoàn toàn routine/result-set trả trạng thái. Chưa tái hiện đúng phiên bản phpMyAdmin trên máy Windows của bạn.
