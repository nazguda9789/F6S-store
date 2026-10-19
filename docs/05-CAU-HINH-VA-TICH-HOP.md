# 05 — Cấu hình và dịch vụ tích hợp

[Mục lục](README.md) · [Cài đặt](01-CAI-DAT-VA-CHAY.md) · [Luồng nghiệp vụ](04-LUONG-NGHIEP-VU.md)

## 1. Nguyên tắc cấu hình

Cấu hình chung ở [application.properties](../src/main/resources/application.properties); profile local mặc định ở [application-dev.properties](../src/main/resources/application-dev.properties). `application-prod.properties` là cấu hình khác, không tự chọn `prod` chỉ vì đưa web ra ngrok. Profile `prod` không chứng minh ứng dụng đã an toàn/sẵn sàng vận hành thật.

Ưu tiên đặt biến ở IntelliJ → Run → Edit Configurations → Environment variables, hoặc trong phiên Terminal chạy app. Không đặt tên biến bằng **giá trị** merchant/token: `System.getenv("VNPAY_TMN_CODE")` đọc biến có tên `VNPAY_TMN_CODE`, không đọc biến có tên là mã merchant.

Không commit mật khẩu, API secret, file export cấu hình Run chứa khóa hoặc database dump thật chứa credential. Nếu khóa đã bị chia sẻ trong chat/log/Git, đổi hoặc thu hồi khóa ở nhà cung cấp; chỉ xóa khỏi màn hình không làm khóa cũ an toàn lại.

## 2. Chạy tối thiểu và chạy từng tích hợp

| Mức | Cần có | Kiểm tra |
|---|---|---|
| Xem trang/COD local | Java, Maven dependency, database/schema, dữ liệu sản phẩm; `SHIPPING_PROVIDER=local` | Xem sản phẩm và tạo đơn thử |
| Quản trị | Tài khoản đúng role và mật khẩu đã biết | Đăng nhập, mở danh sách đơn/sản phẩm |
| Đăng ký/quên mật khẩu | SMTP hoạt động | Nhận OTP/link reset bằng hộp thư thử |
| Upload ảnh | Cloudinary chính hợp lệ | Upload ảnh thử, xác nhận URL và giao diện |
| VNPay demo | Merchant Sandbox + secret + URL truy cập/return phù hợp | Thử thành công, thất bại, hủy, hết phiên |
| GHN | Token/shop/kho cùng môi trường | Tải địa chỉ và server báo giá; tạo vận đơn phải kiểm tra riêng |
| OAuth/bot/AI/SMS | Tài khoản ứng dụng, khóa, quyền và hạn mức tương ứng | Kiểm thử chức năng cụ thể |

Thiếu dịch vụ tùy chọn không có nghĩa phải cài tất cả để xem trang. Ngược lại, app khởi động không bảo đảm nhánh gọi dịch vụ đó sẽ thành công.

## 3. Database, cổng và admin

| Biến | Mục đích |
|---|---|
| `SPRING_DATASOURCE_URL` | Ví dụ `jdbc:mysql://localhost:3306/f6s_shop` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Tài khoản DB, không phải tài khoản admin website |
| `SERVER_PORT` | Cổng HTTP ứng dụng, mặc định 8080 |
| `SHIPPING_PROVIDER` | `local` hoặc `ghn` |
| `F6S_ADMIN_EMAIL`, `F6S_ADMIN_PASSWORD` | Khởi tạo admin hoặc xác định admin cần reset |
| `F6S_ADMIN_RESET_PASSWORD` | Chỉ đặt `true` khi chủ động reset admin có sẵn, bỏ sau khi dùng |

Các property `shipping.cost`, `amount.freeShipping` cấu hình phí local và ngưỡng miễn phí. Cấu hình admin initializer không tự đổi mật khẩu nhân viên/user có sẵn. Không nhập mật khẩu thuần trực tiếp vào cột `users.password`.

## 4. Email

- Đợt sửa 19/10: đăng ký và gửi lại mã dùng gửi đồng bộ, báo lỗi/rollback khi SMTP không chấp nhận thư; xem [hướng dẫn mới](07-CAP-NHAT-EMAIL-VA-NOI-DUNG.md). Không có tài khoản SMTP hoặc App Password dùng chung trong Git.
- Biến: `MAIL_USERNAME`, `MAIL_PASSWORD`; mật khẩu dùng App Password phù hợp tài khoản SMTP, không ghi vào repository.
- Cấu hình hiện tại dùng SMTP Gmail cổng 587 và STARTTLS. Tài khoản cần hỗ trợ/phê duyệt cơ chế đăng nhập đó.
- Kiểm tra `support.url`, `datlaimatkhau.url` nếu host/cổng thay đổi để link gửi email trỏ đúng web đang chạy.
- Test riêng OTP đăng ký, gửi lại OTP, quên mật khẩu, link sai/hết hiệu lực và mail đơn hàng. Kiểm tra cả spam/quota và log lỗi, không công bố OTP/key thật.

## 5. Cloudinary

Admin → màn cấu hình Cloudinary `/adm-prj/qtht-001/cloudkey/list` → thêm cloud name, API key/secret của bạn → chọn cấu hình chính → upload ảnh thử. Service hiện lấy dữ liệu `cloud_key`; không mặc định chỉ đặt `CLOUDINARY_URL` là đủ.

Không xuất bản screenshot chứa secret. Ảnh mẫu từ URL bên ngoài có thể xem được dù chưa cấu hình upload; đó là hai khả năng khác nhau.

## 6. VNPay Sandbox và ngrok

Hướng dẫn từng bước: [VNPAY_SANDBOX.md](../VNPAY_SANDBOX.md).

1. Được cấp merchant **Sandbox** đang hoạt động, không trộn TmnCode/secret production với endpoint Sandbox.
2. Đặt `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` bằng thông tin hiện hành của merchant. Không gửi secret qua chat.
3. Khởi động lại app. Chạy `ngrok http 8080` nếu app ở cổng 8080.
4. Mở website qua `https://<domain-ngrok-hiện-tại>` ngay từ đầu phiên mua; giữ ngrok/app hoạt động đến khi trở về.
5. Return URL ứng dụng: `https://<domain-ngrok-hiện-tại>/check-vnpay`. Khai báo tên miền/URL theo yêu cầu merchant.
6. Dùng tài khoản/thẻ thử chính thức của VNPay Sandbox, không dùng thẻ thật.

Code mặc định gọi `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`. `server.forward-headers-strategy` mặc định `framework` để lấy đúng scheme/host qua proxy như ngrok; khi triển khai thật chỉ tin forwarded headers từ proxy được kiểm soát.

Ngrok không thay thế merchant, không sửa lỗi TmnCode chưa kích hoạt và không phải URL cổng thanh toán. “Không tìm thấy website” trên cổng cần kiểm tra merchant/môi trường và gửi mã tra cứu cho VNPay nếu cần. Không giải quyết bằng cách thay endpoint Sandbox thành tên miền ngrok.

Return theo phiên không thay thế IPN server-to-server, chống lặp, đối soát tiền và hoàn tiền production. Chưa có bằng chứng giao dịch thật/Sandbox với merchant của người dùng thành công trong lượt kiểm thử bằng khóa giả.

## 7. GHN

| Cấu hình | Vai trò |
|---|---|
| `SHIPPING_PROVIDER=ghn` | Bật chế độ GHN thay local |
| `GHN_API_KEY` hoặc `GHN_TOKEN` | Khóa theo các binding hiện có |
| `GHN_SHOP_ID` | Shop cùng môi trường token |
| `ghn.base-url` | Endpoint dev/production đúng tài khoản |
| `GHN_FROM_DISTRICT_ID`, `GHN_FROM_WARD_CODE` | Kho gửi theo mã GHN |

Tránh đặt hai biến token với giá trị mâu thuẫn. Kiểm tra [GhnClient](../src/main/java/com/web/ghn/GhnClient.java) và properties nếu cập nhật cấu hình. Khối lượng sản phẩm được đổi kg → gram khi báo phí; kiểm tra dữ liệu trọng lượng hợp lý.

Thử tải tỉnh/huyện/phường rồi báo phí. Lỗi cấu hình/API không được coi là ship miễn phí. Danh mục JSON local không dùng làm mã GHN. Có báo phí hoặc có controller tạo vận đơn chưa chứng minh đơn online tự tạo vận đơn thành công; kiểm tra riêng hợp đồng request và kết quả nhà cung cấp.

## 8. Google/Facebook OAuth

Hướng dẫn Google mới: [đăng ký/đăng nhập và cấu hình đủ cặp Client ID/Secret](08-DANG-NHAP-GOOGLE-VA-XAC-THUC.md). Tài khoản Google chỉ hoàn tất khi provider trả email đã xác minh; không cần OTP cửa hàng cho bước Google hợp lệ.
Tạo ứng dụng nhà cung cấp của chính bạn; thay client ID cấu hình bằng ID ứng dụng đó. Đặt secret qua:

```text
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_SECRET
```

Redirect URI khi dùng local: `http://localhost:8080/login/oauth2/code/google` và `/login/oauth2/code/facebook`. Khi dùng domain khác, khai báo đầy đủ URI mới trên nhà cung cấp. Không để biến token URI rỗng ghi đè endpoint hợp lệ. Kiểm tra phạm vi email/profile, quyền ứng dụng, tài khoản thử và thông tin user sau đăng nhập.

## 9. Telegram, chatbot, SMS và cấu hình khác

| Tích hợp | Thiết lập / giới hạn |
|---|---|
| Telegram | `TELEGRAM_BOT_TOKEN` và property `telegram.bot.username`; thiếu token/username thì bỏ qua đăng ký bot thủ công. Token hợp lệ chưa bảo đảm quyền/chat nhận đã đúng |
| Chatbot AI | `GEMINI_API_KEY`, `GROQ_API_KEY` theo provider code sử dụng; kiểm tra model, quota và lỗi API |
| Chat người dùng–nhân viên | Không phải chatbot AI; cần session/phân quyền, API và kết nối WebSocket phù hợp |
| SMS | Có property `speedsms.api.key`, `speedsms.api.url` và client dùng `SPEEDSMS_TOKEN`; chưa có một cấu hình thống nhất đã được kiểm chứng cho mọi luồng |
| Shopee | Có entity và màn khóa; không khẳng định có luồng đồng bộ thương mại điện tử hoàn chỉnh |
| `ProvinceService` | Có phương thức đồng bộ dữ liệu địa chỉ ngoài vào bảng `province`/`wards`; checkout local hiện dùng JSON đóng gói, không yêu cầu tự chạy đồng bộ này |

## 10. Checklist trước khi public demo

- Database thử đã sao lưu; không chứa dữ liệu người dùng thật/khóa thật để chia sẻ.
- Đã đổi mật khẩu admin, bỏ cờ reset và mật khẩu bootstrap khỏi cấu hình chạy.
- Chỉ mở những tích hợp cần trình diễn; không dùng thẻ thanh toán thật.
- Đã kiểm tra quyền backend cho API ngoài prefix admin/employee, giới hạn upload, tra cứu đơn và chat.
- Biết ai có thể truy cập ngrok; đóng tunnel sau buổi demo.
- Không gọi đây là triển khai production chỉ vì đã có HTTPS ngrok.
