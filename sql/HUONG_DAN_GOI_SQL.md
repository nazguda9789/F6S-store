# Gói SQL F6S — 23/10/2026

Gói tổng hợp theo bản code 94fcde0. Chọn MỘT trong hai file, không import cả hai theo thứ tự.

## Máy đang có code và database cũ

1. Dừng web trong IntelliJ, bật MySQL của XAMPP.
2. Mở http://localhost/phpmyadmin, chọn database đang dùng → Export để sao lưu.
3. Vẫn chọn đúng database đó → Import → chọn `nang_cap_f6s.sql` → Go.
4. Nếu có lỗi, giữ nguyên dữ liệu và gửi thông báo lỗi; không xóa database.
5. Chạy bản code mới bằng JDK 17, giữ cấu hình database và secret của máy.

File nâng cấp gộp bảng OTP, thuộc tính giày, mã sản phẩm/SKU, giữ SKU cũ, audit gộp biến thể và customer_id cho đơn chờ/hóa đơn. Không reset mật khẩu. Bước gộp 245 vào 244 chỉ áp dụng khi đúng dữ liệu và không còn giỏ/đơn chờ tham chiếu nguồn: APPLIED = đã gộp; ALREADY_APPLIED = đã gộp trước; SKIPPED_REVIEW_CART_DRAFT_OR_VARIANT_DATA = chưa gộp, cần kiểm tra, không ép xóa tham chiếu.

## Cài mới hoàn toàn

Chỉ import `f6s_shop.sql` trên máy không có dữ liệu cần giữ. CẢNH BÁO: file này có DROP DATABASE f6s_shop, xóa database cùng tên trước khi tạo lại. Không dùng để cập nhật database cũ.

Bản mẫu gồm 50 sản phẩm, 13 bài viết, 1 admin và 2 nhân viên nguồn. Admin demo: admin@gmail.com / 123123; đổi mật khẩu trước khi public. Biến thể 244/245 vẫn giữ hai dòng lịch sử, 245 ngừng bán sau gộp đủ điều kiện. Không phải bộ dữ liệu lấy từ Bee hay bản hoàn tất mọi chức năng Bee.

## Lưu ý

- Không cần chạy từng patch SQL cũ sau hai file tổng hợp này.
- Gói không chứa MAIL_PASSWORD, Google Client Secret, VNPay HashSecret hoặc token khác. Giữ/cấu hình riêng trên từng máy; SQL không cấu hình được các dịch vụ đó.
- Đã kiểm tra MySQL 8; chưa kiểm chứng toàn bộ trên XAMPP/MariaDB Windows. Nếu import lỗi, không tiếp tục chạy web hoặc xóa database để thử.
