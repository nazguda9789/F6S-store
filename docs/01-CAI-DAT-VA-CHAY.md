# Cài đặt và chạy

## Phần mềm tối thiểu

- JDK **17**, kiểm tra `java -version` và Project SDK/Run JRE trong IntelliJ.
- MySQL 8; nếu dùng XAMPP, kiểm tra phiên bản MariaDB/MySQL thực tế của máy.
- Git hoặc tải ZIP source, Maven Wrapper đã có sẵn. Không cần Node/npm/Docker để chạy web.
- Internet cho Maven tải dependency lần đầu và các dịch vụ ngoài/CDN khi sử dụng.

XAMPP chỉ dùng cho MySQL/phpMyAdmin. Web này chạy bằng Java trên cổng 8080, không chép source vào htdocs để chạy như PHP.

## Database cài mới

Đọc [hướng dẫn phpMyAdmin](../sql/install/README.md). Import `01_schema.sql` rồi `02_catalog_admin.sql`; chỉ thêm `03_demo_optional.sql` nếu muốn dữ liệu nghiệp vụ mẫu. Không import all-in-one hoặc SQL maintenance cùng bộ cài mới.

Database mặc định của bộ này là `f6s_shop_clean_demo`, khác database nhập dở từ file cũ. Nếu tên này đã tồn tại, dừng và chọn tên mới trong cả ba file; không bỏ qua lỗi CREATE DATABASE để tiếp tục.

## Datasource và khởi động

IntelliJ → Run Configuration → Environment variables:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/f6s_shop_clean_demo
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=
```

Mật khẩu/cổng MySQL phải đúng máy. Không thay tài khoản MySQL bằng tài khoản admin website. Chạy class `com.web.ShopgiaythethaoApplication`.

PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/f6s_shop_clean_demo'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD=''
.\mvnw.cmd spring-boot:run
```

Linux/macOS: đặt cùng biến môi trường, chạy `./mvnw spring-boot:run`. Nếu cổng 8080 đã dùng, dừng ứng dụng trùng hoặc đổi `SERVER_PORT` và cập nhật URL callback tương ứng khi dùng OAuth/VNPAY.

Mở `http://localhost:8080`. Admin demo `admin@gmail.com` / `123123`, phải đổi trước công khai. Nhân viên/khách mẫu của file 03 không có mật khẩu chung và đang ngừng hoạt động; admin chuẩn bị từng tài khoản nếu cần kiểm thử.

## Những gì SQL không tự làm

Mail OTP cần SMTP và App Password. Google cần OAuth client/callback. VNPAY cần thông tin sandbox/return URL hợp lệ. Upload ảnh/vận chuyển cần cấu hình dịch vụ. Không yêu cầu các secret này chỉ để mở web và thử catalog/POS local; tính năng tích hợp chưa cấu hình có thể bị vô hiệu hóa hoặc báo lỗi cấu hình. Xem [tài liệu tích hợp](05-CAU-HINH-VA-TICH-HOP.md).

## Đang có dữ liệu thật

Không import installer để đổi giao diện hoặc đặt lại mật khẩu. Backup và xem [SQL maintenance](../sql/README.md). Giữ biến môi trường, tài khoản và cấu hình OTP/VNPAY hiện tại. Các thao tác import mẫu chỉ được thử trên database riêng.
