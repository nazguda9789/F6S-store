# Kiểm thử và xử lý lỗi

## Import #2014 / FOREIGN_KEY_CHECKS

MySQL thử nghiệm chấp nhận `SET FOREIGN_KEY_CHECKS=ON`. Cảnh báo parser phpMyAdmin về ON và lỗi driver `#2014 Commands out of sync` không phải cùng một bằng chứng lỗi cú pháp. File gộp cũ có CALL trả result set; kết nối chưa xử lý hết có thể lỗi ở câu lệnh sau.

Bộ mới: [sql/install](../sql/install/README.md), không CALL/routine, chỉ FK_CHECKS=0/1. Làm mới kết nối phpMyAdmin; nếu còn lỗi trên kết nối mới, restart Apache trong XAMPP. Dùng database mới `f6s_shop_clean_demo`, import 01 rồi 02; 03 tùy chọn. Không xóa database cũ có dữ liệu hoặc tiếp tục bỏ qua lỗi import.

## Lỗi chạy thường gặp

| Triệu chứng | Kiểm tra |
| --- | --- |
| Unsupported class version / javac | JDK 17 của terminal, Maven, Project SDK và Run JRE |
| Illegal char ? trong đường dẫn | Chuyển source sang đường dẫn không dấu như C:\projects\F6S-store, mở lại project |
| Unknown database / Access denied | Datasource/cổng/user/password MySQL; không dùng mật khẩu website thay mật khẩu MySQL |
| Unknown column archived/cash_received | Schema chưa ở bản hiện hành; cài mới đúng 01/02 hoặc dùng upgrade giữ dữ liệu |
| Không gửi được OTP | Cấu hình SMTP/App Password; không kết luận Gmail thật hoạt động từ test SMTP giả |
| Google chưa bật/redirect mismatch | Client ID/Secret, test user và redirect URI đúng host/port |
| VNPAY không tìm thấy website/chữ ký sai | TMN/Hash Secret đúng môi trường sandbox, URL cổng/callback và tham số ký |
| Cổng 8080 bận | Dừng tiến trình trùng hoặc đổi SERVER_PORT và callback tương ứng |

## Test tự động

Các test Java trong `src/test/java`; test JS checkout/địa chỉ/presence và các smoke Playwright trong `src/test/js`. 115 test Java chọn lọc và 12 test JS đã đạt trong đợt catalog trước; đây không phải toàn bộ suite hoặc chứng nhận production. Đợt tách installer bổ sung test chạy ba file SQL bằng các kết nối riêng, kiểm tra số lượng và không có routine/result set trả trạng thái.

Đợt tách installer cũng chạy lại 115 test Java/12 test JS; import thành công trên MySQL 8.4.11 và MariaDB 10.4.34. Smoke trình duyệt trên MySQL mở 9 màn quản trị và menu desktop/mobile sau riêng 01 + 02 (25 giày, 1 admin, 0 hóa đơn), xác nhận 03 thật sự tùy chọn. Chưa tái hiện môi trường phpMyAdmin Windows cụ thể của người dùng.

Nhóm JS unit (chỉ người phát triển cần Node):

```bash
node --test src/test/js/checkout.test.cjs src/test/js/local-address.test.cjs src/test/js/online-presence.test.cjs
```

Smoke Playwright cần Node, Playwright và Chromium; không phải dependency để người dùng chạy website. `sports-catalog-browser.cjs`, `product-builder-browser.cjs`, `catalog-quick-create-browser.cjs`, `workspace-browser.cjs` kiểm tra giao diện/catalog; `pos-cash-browser.cjs` **ghi hóa đơn và thay tồn**, chỉ chạy database demo riêng. Đọc header/env trong từng test trước khi chạy.

`minimal-install-sql.cjs` kiểm tra bộ install không routine trên database mới, không đụng database thật. `sports-catalog-sql.cjs` kiểm tra maintenance/rollback bằng fixture cũ lấy từ Git history. Không xóa Git history hoặc trộn test fixture với dữ liệu cửa hàng.

Không xác nhận mail Gmail, thanh toán thật hay mọi phiên bản XAMPP/phpMyAdmin chỉ từ unit test. Kiểm thử tích hợp thực phải dùng thông tin sandbox của chính nhóm và đối soát kết quả.
