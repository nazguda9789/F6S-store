# 06 — Kiểm thử, xử lý lỗi và giới hạn

[Mục lục](README.md) · [Cài đặt](01-CAI-DAT-VA-CHAY.md) · [Luồng nghiệp vụ](04-LUONG-NGHIEP-VU.md)

## 1. Các kiểm thử đã có

| Nhóm | Nội dung | Số test đã chạy đạt gần nhất |
|---|---|---|
| `DataInitializerTests` | Tạo/reset admin và bảo vệ role | 4 |
| `GhnClientTests` | Cấu hình, phản hồi và lỗi nhà cung cấp | 5 |
| `CloudinaryServiceTests` | Lựa chọn cấu hình và xử lý upload | 4 |
| `ChatRealtimeApiTests` | Kiểm tra API hội thoại liên quan quyền | 2 |
| `ShippingPolicyTests` | Phí local/GHN, ngưỡng miễn phí, dữ liệu sai | 5 |
| `VNPayServiceTests` | Thiếu cấu hình, URL/chữ ký, tiền, múi giờ, thời hạn | 2 |
| JavaScript checkout | Danh sách địa chỉ rỗng, lỗi provider, phí và miễn phí | 4 |
| JavaScript địa chỉ local | Phường trùng tên, tự điền/reset, tìm không dấu, nhập mới, lỗi dữ liệu, tính toàn vẹn danh mục | 5 |

Tổng lượt kiểm chứng gần nhất: **22 test Java chọn lọc, 9 test JavaScript**. Đây không phải coverage toàn bộ ứng dụng. Test VNPay dùng khóa giả, test/mock dịch vụ không chứng minh nhà cung cấp thật đã chấp nhận giao dịch.

## 2. Lệnh chạy lại

PowerShell tại thư mục có `pom.xml`, JDK 17:

```powershell
.\mvnw.cmd "-Dtest=DataInitializerTests,GhnClientTests,CloudinaryServiceTests,ChatRealtimeApiTests,ShippingPolicyTests,VNPayServiceTests" test
```

JavaScript cần Node.js nhưng không cần `npm install` vì các test hiện dùng thư viện chuẩn:

```powershell
node --test src/test/js/checkout.test.cjs src/test/js/local-address.test.cjs
```

Chạy toàn bộ test bằng `.\mvnw.cmd test`; test context như `ShopgiaythethaoApplicationTests` có thể yêu cầu cấu hình/database. Chạy trên database thử, không dùng reset admin hoặc test tích hợp không rõ tác động với dữ liệu thật. Đọc `target/surefire-reports`: phân biệt tests run, failures, errors, skipped; build thành công với 0 test không chứng minh chức năng đúng.

Build: `.\mvnw.cmd clean compile`; đóng gói: `.\mvnw.cmd package` (mặc định có chạy test). Không dùng bỏ qua test để mô tả rằng bản phát hành đã được kiểm chứng.

## 3. Kiểm tra thủ công theo luồng

| Luồng | Tình huống tối thiểu | Kết quả mong đợi để xác minh |
|---|---|---|
| Cài mới | Import DB thử, kết nối, trang chủ | Có dữ liệu, không lỗi template/SQL |
| Đăng nhập | Đúng/sai mật khẩu, sai nhiều lần, role khác | Redirect/khóa/quyền phù hợp; không truy cập nội bộ trái quyền |
| Sản phẩm | Size/màu, giá riêng, hết tồn, ảnh lỗi | Biến thể/giá/ảnh phù hợp; không mua vượt tồn |
| Giỏ | Khách vãng lai và user; số lượng 0/âm/lớn | Validate; tổng và dữ liệu đúng nhánh lưu trữ |
| Địa chỉ local | Đổi tỉnh, phường trùng tên, gõ không dấu, địa chỉ mới, dữ liệu tải lỗi | Không giữ quận/phường cũ; vẫn nhập tay được |
| COD | Địa chỉ thiếu/đủ, điện thoại sai, voucher, ngưỡng 500.000đ | Lỗi rõ ràng hoặc tạo mã đơn; DB lưu địa chỉ/phí/tồn chính xác |
| Tồn đồng thời | Hai khách mua số cuối | Không âm tồn, không tạo đơn dở dang; kiểm tra riêng COD/quầy/VNPay |
| Tại quầy | Đơn chờ rỗng, thêm/sửa/xóa dòng, chốt lặp | Không chốt sai/nhân đôi; tổng, tồn và trạng thái đúng |
| VNPay | Thành công/hủy/sai chữ ký/lặp callback/mất session/hết tồn sau trả tiền | Không ghi đơn/thanh toán sai; ghi nhận hạn chế chưa xử lý và đối soát |
| Trạng thái | Từng cạnh hợp lệ/không hợp lệ, đổi sau hạn, vòng đổi–hoàn thành | Đúng map, lịch sử, tồn, số bán và báo cáo |
| Báo cáo | Tập đơn biết trước ngày, kênh, payment, voucher, ship, đổi/hủy | Đối soát từng truy vấn với tập dữ liệu kỳ vọng |
| Tích hợp | Timeout, sai khóa, quota, phản hồi không hợp lệ | Không rò secret, không giả thành công |
| UI | Desktop/mobile, bàn phím, form lỗi, màu chữ | Dễ đọc và thao tác; không nút bị che/khóa vô lý |

Các dòng trên là checklist thiết kế, **không phải báo cáo tất cả đã thực thi** hoặc bộ 250 testcase. Nếu làm tài liệu kiểm thử, tách Test Plan/Test Design/Test Case/Test Report; chỉ ghi pass sau khi chạy và có bằng chứng.

## 4. Bằng chứng đã kiểm chứng trong môi trường phát triển

- JDK 17, Spring Boot chạy với MySQL 8; SQL đầy đủ import được, dữ liệu bổ sung được kiểm tra tính lặp và liên kết chính.
- Trình duyệt mở trang chính, thêm giỏ, đặt COD local, mở trang xác nhận; đã kiểm tra lưu địa chỉ/phí, đăng nhập admin và tương phản header.
- Địa chỉ tìm kiếm: đã kiểm tra chọn danh mục, tên gửi về form, tự điền quận, đổi tỉnh xóa giá trị cũ, nhập địa chỉ mới, đặt COD; giả lập tải danh mục trả 503 vẫn nhập được và không có lỗi JavaScript không được bắt trong lượt đó.
- Chưa kiểm chứng đầy đủ XAMPP/MariaDB trên Windows, mọi CRUD, giao dịch merchant VNPay, GHN/SMTP/OAuth/Cloudinary bằng credential thật hoặc tải/đồng thời lớn.

Không suy ra mọi chức năng đã hoạt động chỉ từ log Hibernate SELECT hoặc console `Started...`. Kiểm chứng trên cloud không thay thế kiểm thử lại trên máy nhóm.

## 5. Bảng xử lý lỗi

| Dấu hiệu | Kiểm tra / hành động |
|---|---|
| `InvalidPathException`, tên thư mục có `?` | Chuyển project sang đường dẫn không dấu, ví dụ `C:\Projects\F6S-store`, đóng/mở lại đúng thư mục |
| Không thấy main/ClassNotFound | Mở thư mục có pom.xml, chọn JDK 17, reload Maven, clean compile; sửa lỗi build đầu tiên |
| Maven không tải được | Kiểm tra Internet/proxy, Offline mode; không tắt TLS/chứng thực để bỏ qua lỗi |
| `Communications link failure` | Bật MySQL, kiểm tra host/cổng thực tế và datasource |
| `Access denied for user` | Sửa user/password DB, không sửa mật khẩu website để giải quyết |
| `Unknown database/table/column` | Đúng database/schema và file SQL chưa? Java đọc bảng tiếng Anh; không import lại file phá DB nếu chưa backup |
| Port 8080 bị chiếm | Stop app cũ hoặc đổi `SERVER_PORT`; cập nhật URL/callback/ngrok theo cổng mới |
| Template `user/index` không tìm thấy | Reload Maven, clean compile; kiểm tra resources trong pom và prefix classpath templates |
| Không chọn được địa chỉ | Xác định local hay GHN; local cần `/data/addresses-legacy.json`/JS mới, GHN cần khóa/shop; Ctrl+F5 sau cập nhật |
| Không thấy địa chỉ mới trong danh mục | Dữ liệu 3 cấp là địa chỉ cũ; gõ tên mới → “Dùng địa chỉ”, để trống quận/huyện nếu không có |
| VNPay báo thiếu cấu hình | Đặt đúng tên biến, đúng Run Configuration, restart; không viết `getenv` bằng giá trị secret |
| VNPay “Không tìm thấy website” | Kiểm tra TmnCode/môi trường/kích hoạt merchant; liên hệ VNPay bằng mã tra cứu, không gửi secret |
| VNPay về web nhưng mất đơn | Cùng trình duyệt/domain/phiên chưa? App có restart không? Đọc giới hạn session/IPN ở tài liệu luồng |
| `tokenUri cannot be empty` | Không để biến endpoint OAuth rỗng ghi đè giá trị mặc định |
| Telegram báo token/username rỗng | Dùng bản có kiểm tra thiếu cấu hình; chỉ bật bot khi có dữ liệu hợp lệ |
| OTP/mail không tới | Kiểm tra SMTP/App Password, spam, quota và log; không mặc định đăng ký đã hoàn tất |
| Upload/ảnh lỗi | Phân biệt URL ảnh ngoài với upload Cloudinary; cấu hình chính, Internet, file và quota |
| `Disconnected, online: 0` | Thường là phiên WebSocket rời; cần log lỗi khác mới kết luận nguyên nhân lỗi mua/thanh toán |
| Hibernate in nhiều SELECT | Do show-sql; riêng SELECT không phải exception; xem stack trace và kết quả HTTP/nghiệp vụ |

Khi nhờ hỗ trợ: gửi thao tác tái hiện, URL/path, vai trò, thời gian, lỗi đầu tiên và phần `Caused by` liên quan; che mật khẩu/token/secret, email/điện thoại thật và chuỗi kết nối có credential. Không chỉ gửi phần cuối log đã cắt mất nguyên nhân.

## 6. Các hạng mục cần rà soát trước dùng thật

1. Quyền API ngoài prefix bảo vệ, quyền trên từng tài nguyên, tra cứu đơn/chat, CSRF và session.
2. VNPay: IPN, đối chiếu amount/merchant/order, idempotency, log đối soát, rollback và tồn kho khi callback tới muộn.
3. Hoàn/hủy/đổi: hoàn tồn, hoàn tiền, số lượng bán và vòng chuyển trạng thái tránh đếm lặp.
4. Quầy: chống chốt lại đơn chờ, phân biệt payment tiền mặt/chuyển khoản và ngày hoàn thành.
5. Báo cáo: thống nhất ngày ghi nhận, tổng tiền/ship/giảm, kênh bán và payment; cảnh báo tồn thấp, xuất báo cáo nếu đề yêu cầu.
6. Bảo mật credential, HTTPS/proxy đáng tin, migration/backup, logging dữ liệu nhạy cảm, giám sát và kiểm thử tải.

Đây là danh sách giới hạn/rủi ro đọc từ source, không phải các lỗi đã được sửa trong lần cập nhật tài liệu này. Khi phát hiện sai lệch, tạo issue kèm điều kiện tái hiện và tiêu chí chấp nhận trước khi giao sửa.
