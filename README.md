# Website quản lý bán giày thể thao F6S

Database hiện dùng là `f6s_shop`. Cài mới bằng `f6s_shop.sql` (có DROP DATABASE, cần sao lưu). Database đang dùng: chọn database và chỉ import `sql/bo_sung_du_lieu.sql` để bổ sung mà không xóa dữ liệu cũ. Xem danh sách đầy đủ tại [DANH_SACH_BO_SUNG.md](DANH_SACH_BO_SUNG.md).

## Chạy bằng IntelliJ IDEA và XAMPP trên Windows

1. Cài JDK 17, IntelliJ IDEA và XAMPP.
2. Trong XAMPP, bật Apache và MySQL. Mở http://localhost/phpmyadmin.
3. Cài mới: import `f6s_shop.sql`. Bản SQL có 50 sản phẩm nguồn, 13 bài viết, 1 admin và 2 nhân viên bên cạnh dữ liệu mẫu cũ; giữ hash mật khẩu gốc. Không nhập khách hàng, giao dịch hoặc khóa dịch vụ từ SQL nguồn. Các bảng `product`, `blog`, `users` phục vụ Java cùng tồn tại với các bảng tiếng Việt; hai nhóm bảng không tự đồng bộ khi sửa dữ liệu sau này.
4. Trong IntelliJ, chọn File → Open và mở thư mục chứa `pom.xml`. Chọn Project SDK 17 và Reload All Maven Projects.
5. MySQL mặc định là `localhost:3306`, username `root`, password trống. Nếu khác, đặt các biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` trong Run Configuration → Environment variables.
6. Để tạo tài khoản quản trị riêng, đặt `F6S_ADMIN_EMAIL` và `F6S_ADMIN_PASSWORD` trong Environment variables trước khi chạy. Không lưu mật khẩu vào Git.
7. Mở `src/main/java/com/web/ShopgiaythethaoApplication.java` và nhấn Run cạnh hàm main. Hoặc chạy trong Terminal Windows: `.\mvnw.cmd spring-boot:run`.
8. Khi console báo Started, mở http://localhost:8080. Nhấn Stop trong IntelliJ để dừng ứng dụng.

Maven cần Internet để tải thư viện. Các chức năng email, OAuth, vận chuyển, SMS và thanh toán cần cấu hình dịch vụ của bạn. Khóa dịch vụ được đọc từ biến môi trường; xem placeholder trong `application*.properties`. Email dùng `MAIL_USERNAME`/`MAIL_PASSWORD`; VNPay dùng `VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET`.

Trạng thái kiểm chứng: bản SQL đầy đủ đã import thành công vào MySQL 8; chạy lại bản bổ sung không tăng số dòng. Đã kiểm tra liên kết sản phẩm–danh mục và sản phẩm–size–màu không bị thiếu. Chưa xác minh chạy Spring Boot hoặc import trên MariaDB/XAMPP.
