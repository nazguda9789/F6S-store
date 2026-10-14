# Website quản lý bán giày thể thao F6S

## Chạy bằng IntelliJ IDEA và XAMPP trên Windows

1. Cài JDK 17, IntelliJ IDEA và XAMPP.
2. Trong XAMPP, bật Apache và MySQL. Mở http://localhost/phpmyadmin.
3. Tạo database `shopgiaydabong`, chọn database này và import `shopgiaythethao.sql`. Bản SQL đã dùng collation tương thích MariaDB của XAMPP; dữ liệu tài khoản, giao dịch và khóa dịch vụ cũ đã được bỏ.
4. Trong IntelliJ, chọn File → Open và mở thư mục chứa `pom.xml`. Chọn Project SDK 17 và Reload All Maven Projects.
5. MySQL mặc định là `localhost:3306`, username `root`, password trống. Nếu khác, đặt các biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` trong Run Configuration → Environment variables.
6. Để tạo tài khoản quản trị riêng, đặt `F6S_ADMIN_EMAIL` và `F6S_ADMIN_PASSWORD` trong Environment variables trước khi chạy. Không lưu mật khẩu vào Git.
7. Mở `src/main/java/com/web/ShopgiaythethaoApplication.java` và nhấn Run cạnh hàm main. Hoặc chạy trong Terminal Windows: `.\mvnw.cmd spring-boot:run`.
8. Khi console báo Started, mở http://localhost:8080. Nhấn Stop trong IntelliJ để dừng ứng dụng.

Maven cần Internet để tải thư viện. Các chức năng email, OAuth, vận chuyển, SMS và thanh toán cần cấu hình dịch vụ của bạn. Khóa dịch vụ được đọc từ biến môi trường; xem placeholder trong `application*.properties`. Email dùng `MAIL_USERNAME`/`MAIL_PASSWORD`; VNPay dùng `VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET`.

Trạng thái kiểm chứng: SQL người dùng cung cấp đã import thành công vào MySQL 8 (30 bảng). Chưa xác minh build và chạy Spring Boot do Maven Central không phân giải DNS trong môi trường chuẩn bị; bản SQL tương thích XAMPP chưa được chạy trên MariaDB.
