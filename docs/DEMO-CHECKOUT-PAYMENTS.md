# Demo online, tại quầy, email OTP và VNPAY

**Cập nhật giao hàng:** database bản này đã có 03/04 thì nâng thêm 05. Xem [hướng dẫn hủy từ khách, nhận hoàn, hàng chờ đối soát và ảnh](FULFILLMENT-AND-MEDIA.md). VNPAY chưa trả có thể xác nhận/chuẩn bị nhưng không được bàn giao vận chuyển; đơn quá hạn có hàng chờ để admin đối soát trước khi hủy.

## 1. Nâng cấp dữ liệu

Sao lưu `f6s_shop` và dừng backend. Dùng **SSMS / SQL Server**, không dùng phpMyAdmin.

- Database trống: `00_create_database.sql` nếu chưa có database → `01_schema.sql` mới nhất → `02_real_products.sql`.
- Database bản Bee đã có dữ liệu: chạy `04_upgrade_integrations.sql`.
- Database bản Vue cũ chưa nâng module Bee: chạy `03_upgrade_bee_modules.sql` → `04_upgrade_integrations.sql`.

Không xóa database hoặc chạy lại schema 01 trên dữ liệu có sẵn. Hiện có 24 bảng ứng dụng, thêm một bảng kỹ thuật khi nhập catalog. 04 chỉ thêm cấu trúc; không tự đánh dấu đơn cũ đã thu/hoàn tiền, không chèn OTP hay secret.

SQL xuất bằng Hibernate SQLServerDialect và có kiểm thử H2 MSSQL mode; vẫn cần nghiệm thu trên SQL Server thật. Trước production dùng migration được duyệt và `DDL_AUTO=validate`, không tự update schema trên dữ liệu kinh doanh.

## 2. Chạy cơ bản, chưa cần dịch vụ ngoài

Backend: mở `backend/pom.xml`, JDK 17, đặt `DB_PASSWORD`, `ADMIN_PASSWORD` (ít nhất 12 ký tự), các biến DB nếu khác mặc định. Chạy `SneakerApplication`; API mặc định cổng 8081.

Frontend (terminal khác):

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:5173`; quản trị `/admin/login`. Biến môi trường phải nằm trong **Run Configuration IntelliJ** hoặc chính terminal chạy Maven. Spring Boot không tự đọc file `.env` ở thư mục gốc.

Trang đăng nhập chung là `/account` (cũng có thể mở `/login` hoặc `/register`). Chọn **Khách hàng** để dùng email OTP; chọn **Nhân viên / Quản trị** để dùng tài khoản/mật khẩu được cấp. `/admin/login` cũ tự chuyển tới lựa chọn nhân viên/quản trị. Backend tự xác định quyền: admin tới tổng quan, nhân viên tới POS. Không tạo thêm quyền chỉ bằng việc chọn loại đăng nhập. Nhân viên quên mật khẩu liên hệ quản trị; khách đăng nhập lại bằng OTP mới. Phiên giữ trong bộ nhớ, không tự lưu mật khẩu vào localStorage.

`SHIPPING_FEE=30000` để demo phí giao hàng, `SHIPPING_FREE_ABOVE=2000000` để miễn phí từ 2 triệu; không cấu hình phí thì mặc định 0. Phí tính trên tiền hàng sau khuyến mãi, được chụp vào đơn. POS không cộng phí giao hàng.

## 3. Kịch bản trình diễn

### Online COD

1. Chọn giày/màu/size còn hàng → thêm giỏ → thanh toán COD. Có thể đặt vãng lai không đăng nhập.
2. Quản trị → Giao dịch → Danh sách hóa đơn → tìm mã đơn.
3. Xác nhận đơn → Chuẩn bị xong → mở Chi tiết / In.
4. Nhập đơn vị `Giao hàng demo`, mã `DEMO-001`, lưu vận đơn.
5. Giao vận chuyển → Xác nhận đã giao / thu tiền. COD lúc này mới được ghi là PAID.

Đây là vận chuyển thủ công phục vụ demo, **không gọi GHN/GHTK và không tạo vận đơn thực tế**. Hủy trước khi giao trả tồn/lượt mã đúng một lần.

### Tại quầy

Quản trị → Bán hàng tại quầy → tạo hóa đơn tạm (tối đa 5/người bán), chọn/quét QR, chọn khách hoặc khách lẻ, chọn khuyến mãi, nhập tiền khách đưa, thanh toán và in. Tiền mặt tại quầy hoàn tất ngay, không cần SMTP/VNPAY.

### Trả hàng / Hoàn toàn bộ tiền

Đơn COMPLETED đã thu tiền, hoặc đơn CANCELLED nhưng VNPAY thu tiền về sau: mở chi tiết → nêu lý do → Mở yêu cầu hoàn.

**Thực hiện hoàn tiền ngoài hệ thống trước**, rồi nhập mã chứng từ và xác nhận trong quản trị. Đơn đã giao phải xác nhận đã nhận/kiểm tra toàn bộ hàng trả; hệ thống cộng lại tồn đúng một lần. Đơn đã hủy đã trả tồn từ trước, không cộng lại. Có thể từ chối yêu cầu; hiện mỗi đơn có một hồ sơ hoàn toàn bộ, không hỗ trợ trả một phần hoặc mở lại hồ sơ bị từ chối.

Nút xác nhận không gọi ngân hàng/VNPAY refund API và không tự chuyển tiền. Không nhập chứng từ giả cho giao dịch thật. Báo cáo trừ tiền hoàn ở kỳ của đơn gốc; đây chưa phải sổ kế toán đối soát hoàn tiền theo ngày chi. Bán chạy bỏ đơn đã hoàn toàn bộ.

## 4. Demo đăng ký/đăng nhập OTP không cần email thật

Cần Docker Desktop. Từ thư mục gốc:

```powershell
docker compose -f docker-compose.mail-demo.yml up -d
```

Đặt vào **môi trường chạy backend**:

```text
OTP_ENABLED=true
OTP_SECRET=<chuỗi bí mật ngẫu nhiên ít nhất 32 ký tự>
SMTP_HOST=127.0.0.1
SMTP_PORT=1025
SMTP_STARTTLS=false
SMTP_FROM=demo@f6s.test
```

Để trống SMTP_USERNAME/SMTP_PASSWORD cho Mailpit. Có thể tạo secret bằng PowerShell:

```powershell
$otpBytes = New-Object byte[] 32
$otpRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$otpRng.GetBytes($otpBytes)
$otpRng.Dispose()
$env:OTP_SECRET = [Convert]::ToBase64String($otpBytes)
```

Khởi động lại backend. Vào `/account`, nhập email demo như `khach@demo.test`, gửi OTP. Mở **http://localhost:8025** để đọc thư và nhập mã. Mailpit chỉ giữ thư cục bộ, **không gửi ra Internet**. Lần đầu xác minh tạo tài khoản; lần sau đăng nhập cùng tài khoản, không có mật khẩu cố định/bypass.

Sau đăng nhập lưu hồ sơ; checkout điền sẵn thông tin, email phải khớp tài khoản đã xác minh. `/account` hiển thị tối đa 50 đơn tạo khi đăng nhập, không tự nhận đơn vãng lai cũ chỉ vì trùng email. Phiên nằm trong bộ nhớ trình duyệt, tải lại trang cần OTP mới.

Quản trị → Tài khoản → Tài khoản online để tìm và ngừng/bật tài khoản đã đăng ký; ngừng sẽ thu hồi các phiên hiện có. Hồ sơ online và hồ sơ khách tại quầy được giữ riêng, không tự hợp nhất bằng số điện thoại/email để tránh lộ lịch sử của người khác.

OTP hết hạn sau 5 phút, tối đa 5 lần nhập; giới hạn gửi lại 60 giây, 5 lần/email/giờ và 200 lần/toàn hệ thống/giờ trên một instance. Mã và token không lưu dạng thô trong database. Phiên khách 24 giờ, logout thu hồi token. Trước production cần rate limit ở reverse proxy, kiểm tra nhiều instance, chính sách lưu/xóa PII và giám sát email.

Email thật: dùng SMTP hợp lệ, bật STARTTLS (mặc định true), host/port/username/password/from của nhà cung cấp. Chế độ không TLS chỉ được phép với localhost phục vụ Mailpit. Không commit secret hay công khai cổng inbox demo.

## 5. VNPAY Sandbox

Cần tài khoản merchant sandbox hợp lệ. Không thể tự tạo TMN code/hash secret thay cho VNPAY.

```text
VNPAY_ENABLED=true
VNPAY_TMN_CODE=<merchant sandbox>
VNPAY_HASH_SECRET=<hash secret của merchant>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment/return
```

Đăng ký IPN URL có HTTPS và truy cập được từ VNPAY:

```text
https://<public-backend-domain>/api/payments/vnpay/ipn
```

Backend local cần tunnel HTTPS do bạn kiểm soát hoặc server test; `localhost` không nhận được IPN từ máy chủ VNPAY. IPN phải đi đến backend, không phải Vite. Chưa có cấu hình thì lựa chọn VNPAY bị vô hiệu hóa; không có thanh toán giả thành công để thay thế.

Chọn VNPAY ở checkout → hệ thống giữ tồn, tạo giao dịch 15 phút, chuyển sang cổng. Backend kiểm tra chữ ký HMAC-SHA512, merchant, số tiền, mã giao dịch và xử lý IPN lặp. Trang return chỉ đọc kết quả; thông số trên trình duyệt không tự đánh dấu PAID. Chưa có IPN thì hiển thị đang chờ, không xử lý giao hàng cho VNPAY chưa thu tiền.

Nếu hết hạn link hoặc giao dịch FAILED: quản trị đối soát rồi hủy đơn, sau đó khách tạo đơn mới; không tự tạo giao dịch thứ hai trên cùng đơn. Nếu đã hủy mà IPN báo tiền về sau: vẫn lưu PAID và cảnh báo cần hoàn tiền, không mở lại đơn/khấu trừ tồn lần nữa. Hoàn VNPAY qua portal của merchant rồi ghi chứng từ như mục 3.

Chưa nghiệm thu với merchant sandbox thật hoặc SQL Server thật trong môi trường này. Không bật production bằng credential demo; cần HTTPS, callback thật, đối soát với nhà cung cấp, backup và kiểm thử race trên SQL Server.

## 6. Ảnh

Thư viện quản trị đã hỗ trợ JPEG/PNG và gắn ảnh đúng từng màu sản phẩm. Các nguồn ảnh hãng/Commons bị chặn HTTP 403 tại môi trường triển khai, nên chưa tải được bộ ảnh thật đầy đủ. Placeholder vẫn ghi rõ chưa có ảnh; không gắn ảnh khác model để giả là sản phẩm thật. Cần ảnh có quyền sử dụng hoặc nguồn tải được truy cập để hoàn tất phần này.

Kiểm thử tự động SMTP dùng Mailpit cục bộ; kiểm thử VNPAY ký thông điệp với secret riêng cho test, không gọi cổng thanh toán hoặc di chuyển tiền thật.
