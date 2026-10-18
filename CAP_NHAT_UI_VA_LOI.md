# Cập nhật giao diện và xử lý lỗi F6S — 18/10/2026

## Nội dung đã thêm/thay

| Khu vực | Thay đổi |
|---|---|
| Nhận diện | Wordmark F6S/Sport Store thay logo mobile bị thiếu; hệ màu navy–cam, font hệ thống, khoảng cách thống nhất |
| Trang chủ | Hero giới thiệu có ảnh thể thao, CTA đến danh sách sản phẩm và giới thiệu; trạng thái rỗng cho sản phẩm bán chạy |
| Sản phẩm | Thẻ sản phẩm bo góc, nền ảnh, phân cấp tên/giá, hover nhẹ; ảnh lỗi thử HTTPS rồi dùng placeholder F6S có ghi rõ ảnh đang cập nhật |
| Điều hướng | Menu mobile dùng button có nhãn; không chặn link danh mục không có submenu; nút tìm kiếm/giỏ có tên truy cập |
| Footer | Link thật tới giới thiệu, bài viết, chính sách, liên hệ, đơn hàng; bỏ mạng xã hội/link trống, số điện thoại giả và cam kết bảo hành không nhất quán |
| Đăng nhập | Tiêu đề và hướng dẫn mới, label/email/autocomplete; không hiển thị nút Google khi chưa cấu hình client secret |
| Admin/nhân viên | Stylesheet riêng làm mới màu sidebar, bảng dữ liệu, card, form và nút; giữ cấu trúc điều hướng/nghiệp vụ |
| Responsive | Bố cục hero/card trên desktop và mobile, focus bàn phím, hỗ trợ reduced motion, bỏ viewport khóa zoom |
| Tin tức | Thay ngày cố định 20/10/2023 bằng ngày đăng thực tế |

## Lỗi đã xử lý

- GHN dùng chung `ghn.base-url` thay địa chỉ production hard-code; sửa endpoint lấy xã/phường; `GHN_SHOP_ID` thay shop ID cố định. Token nhận từ `GHN_API_KEY` hoặc `GHN_TOKEN`.
- Timeout kết nối/đọc cho RestTemplate dùng chung, kiểm tra tham số và response GHN; lỗi cấu hình/provider trả 400/503 có kiểm soát.
- Checkout không sập khi GHN thiếu khóa hoặc không phản hồi: hiện thông báo và khóa cả hai nút đặt hàng. Không tự giả lập vận chuyển thành công hoặc coi API lỗi là miễn phí ship.
- JavaScript địa chỉ xử lý danh sách rỗng, lỗi HTTP và phản hồi cũ đến chậm; cập nhật phí ship ở nội dung hiển thị, input và tổng tiền; giữ quy tắc miễn phí từ 500K hiện có.
- Chọn COD/VNPay ở sự kiện submit của form để hoạt động trên cả desktop/mobile; MoMo chưa hỗ trợ được chặn submit.
- `cart.js` không gắn listener lên nút không tồn tại; hỗ trợ nhiều nút thanh toán.
- Cloudinary kiểm tra cấu hình chính/file rỗng, dùng tên file tạm an toàn và dọn trong finally; dùng HTTPS; upload nhiều file giữ thứ tự và không âm thầm trả thiếu ảnh. Nếu file sau lỗi, ảnh trước đã lên Cloudinary không tự rollback.
- SMS có giá trị cấu hình mặc định rỗng để không lỗi placeholder lúc khởi tạo; báo chưa cấu hình khi gọi dịch vụ. Chưa xác minh gửi SMS thực tế.
- Chat lịch sử cho khách chưa đăng nhập trả 401 thay NullPointerException; frontend không gọi lịch sử chat của khách ẩn danh; giới hạn API đọc/trả lời chat nhân viên cho ADMIN/EMPLOYEE.
- Dùng bản local của Bootstrap, Font Awesome, SockJS, STOMP, OwlCarousel và Select2 tại các trang khách hàng được sửa; bỏ script Font Awesome kit mẫu. Giữ nguyên SRI của OwlCarousel, đã đối chiếu SHA-512.
- Tắt SMTP debug mặc định; không thêm khóa dịch vụ thật vào mã nguồn.

## Cập nhật và cấu hình

1. Pull/tải mã nguồn mới, giữ cấu hình Run và biến môi trường của bạn.
2. Reload Maven, chạy `mvnw.cmd clean compile`, sau đó Run ứng dụng. Không cần import lại SQL cho đợt sửa này.
3. GHN: cần `GHN_API_KEY` (hoặc `GHN_TOKEN`), `GHN_SHOP_ID`, `ghn.base-url` đúng môi trường và địa chỉ kho `GHN_FROM_DISTRICT_ID`/`GHN_FROM_WARD_CODE`.
4. Cloudinary: thêm cấu hình chính bằng trang admin trước khi upload. Email/OAuth/VNPay/SMS/chatbot cần tài khoản dịch vụ hợp lệ của bạn.
5. Ảnh thay thế chỉ là trạng thái hiển thị khi nguồn ảnh không truy cập được, không phải ảnh thật của sản phẩm và không sửa URL trong SQL.

## Phạm vi và lưu ý

- Không xóa/sửa dữ liệu sản phẩm, bài viết, tài khoản hoặc import lại SQL.
- Không tự đồng bộ hai nhóm bảng tiếng Việt/Java. Ứng dụng tiếp tục dùng bảng Java `product`, `users`, `invoice`…; không sửa bảng tiếng Việt để mong thay đổi trên web.
- Không coi đây là chứng nhận toàn bộ website đã hết lỗi. Thanh toán thật, GHN thật, email/SMS, upload bằng khóa thật, luồng hoàn tiền và tranh chấp tồn kho cần kiểm thử nghiệp vụ riêng.
- Các thư viện khác ở trang admin có thể vẫn cần Internet; việc đưa thư viện khách hàng về local không biến toàn bộ hệ thống thành ứng dụng offline.

## Kiểm chứng

- Biên dịch bằng JDK 17 trong container Maven.
- 15 unit test Java: admin initializer (4), GHN (5), Cloudinary (4), chat (2); đều đạt, không bỏ qua.
- 4 kiểm thử JavaScript checkout bằng Node test runner; đều đạt.
- Trình duyệt Chromium: trang chủ, đăng nhập, giỏ, bài viết, giới thiệu và chi tiết sản phẩm trả HTTP 200. Trang chủ ở 1440px và 390px, cùng login/cart/blog/product ở 390px không tràn ngang. Không có lỗi JavaScript không được bắt trong lượt kiểm tra cuối.
- Thêm một sản phẩm vào giỏ session thành công; mở checkout thiếu GHN trả HTTP 200, hiện cảnh báo và khóa cả hai nút đặt hàng. API lịch sử chat ẩn danh trả 401.
- Ảnh giao diện thật chụp trên database kiểm thử: [desktop](docs/images/f6s-desktop.png), [mobile](docs/images/f6s-mobile.png). Placeholder hiển thị vì URL ảnh nguồn không tải được trong môi trường kiểm thử.

Lệnh chạy các unit test Java:

```powershell
.\mvnw.cmd "-Dtest=DataInitializerTests,GhnClientTests,CloudinaryServiceTests,ChatRealtimeApiTests" test
```

Kiểm thử JavaScript là tùy chọn cho người phát triển có Node.js:

```text
node --test src/test/js/checkout.test.cjs
```
