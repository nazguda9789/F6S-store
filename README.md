# F6S Store — cập nhật từ ZIP ngày 03/11/2026

**Java 17 · Spring Boot 4.1.1 · Thymeleaf · Spring Security · SQL Server.**

**Bản này không dùng Vue. Không chạy `npm run dev` hay mở `backend/pom.xml` cũ.** Mở `pom.xml` ở gốc, chạy `com.example.demodatn.DemoDatnApplication`, vào **http://localhost:8080/**. Node chỉ phục vụ test trình duyệt.

Source nền: `F6S_Store-main.zip` do người dùng gửi. Source trước vẫn giữ trong lịch sử Git/nhánh dự phòng, không trộn entity/SQL hai dự án.

## Thay đổi

- POS: chọn sản phẩm/biến thể → kiểm tra giỏ → khách hàng & thanh toán → in hóa đơn. Giữ cơ chế hóa đơn chờ và token chống checkout lặp.
- Hiện số biến thể/tổng tồn trên thẻ POS; chọn sản phẩm xem size/chất liệu, giá và tồn riêng. Giỏ có cột Trong kho. Trang chi tiết quản trị/khách hiện tổng biến thể/tồn; danh sách admin có liên kết số biến thể.
- Form biến thể chấp nhận size/chất liệu mới và báo lỗi bỏ trống/trùng.
- Bán hàng online dẫn về trang chủ `/`, không phải admin nhập đơn thay khách. Khách cần tài khoản để checkout, giỏ giữ qua đăng nhập; header hiện tên người đã đăng nhập.
- Có thể gõ số lượng, sửa người nhận/điện thoại/email/địa chỉ. Backend validate và token ngăn gửi lặp tạo hai đơn.
- Nhân viên: sinh mật khẩu tạm, lưu BCrypt, gửi mail, bắt đổi mật khẩu. Thông tin account được điền trước INSERT. SMTP lỗi cảnh báo/có nút gửi lại, không in mật khẩu trên web.
- Style chung, POS đáp ứng desktop/mobile; Bootstrap được lưu trong source, không phụ thuộc CDN cho form/nút/menu.

## 1. Chuẩn bị

JDK **17 đầy đủ** (không chỉ JRE), IntelliJ hoặc Maven Wrapper, SQL Server đã bật TCP/IP. Đăng nhập SSMS được chưa bảo đảm cổng TCP 1433 đã mở. Sao lưu database và thư mục ảnh `uploads/` trước khi thay source.

## 2. SQL — chọn đúng bản

**Không import SQL này vào `f6s_shop` của bản Vue cũ: schema không tương thích.**

### Chưa có database của ZIP

Trong SSMS chạy **`database/init_shopbangiay_thu.sql`** một lần. File tạo **`shopbangiay_thu`**, 21 bảng và dữ liệu mẫu của ZIP. Đã bỏ DROP DATABASE: nếu database đã tồn tại, báo lỗi và bỏ phần còn lại để bảo toàn dữ liệu.

Không chạy thêm `database.sql` hoặc `shopbangiay_thu_full.sql`: chúng là bản khởi tạo thay thế, không phải bước tiếp theo. Không xóa DB để ép import.

### Đã dùng database của chính ZIP này

Giữ dữ liệu. Đợt sửa nghiệp vụ này không thêm bảng/cột. Nếu `SCHEMA_CHECK` báo thiếu mapping, đối chiếu migration được nêu trong lỗi trên bản sao DB trước. **Không dùng SQL 01–06 của bản Vue cũ.** Script/biên bản cũ được giữ để đối chiếu, không phải thứ tự cài đặt bắt buộc.

## 3. Cấu hình và chạy

Sao chép `application-example.properties` thành **`application-local.properties`** ở cạnh `pom.xml` (đã gitignore). URL mẫu trỏ localhost:1433/database `shopbangiay_thu`; sửa máy/cổng nếu khác.

IntelliJ → Run → Edit Configurations → Environment variables, mỗi biến một dòng:

| Name | Value |
|---|---|
| SPRING_DATASOURCE_USERNAME | Tài khoản SQL Server, ví dụ `sa` |
| SPRING_DATASOURCE_PASSWORD | Mật khẩu SQL Server trên máy bạn |

Có thể đặt `SPRING_DATASOURCE_URL` để thay URL. Giữ `ddl-auto=validate`, không tắt schema-check trên DB thật. `trustServerCertificate=true` chỉ dùng local/demo; production cần chứng chỉ tin cậy.

Mở **pom.xml ở gốc** như Maven project, chọn Project SDK/Run JDK 17, chờ thư viện, chạy **DemoDatnApplication**. Hoặc PowerShell tại thư mục đó:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Web: http://localhost:8080/. POS: http://localhost:8080/admin/pos. Tài khoản mẫu của ZIP chỉ dùng local; đổi ngay mật khẩu quản trị mặc định, không công khai cửa hàng với tài khoản demo.

## 4. Email nhân viên qua Mailpit, không cần Docker

Chạy và giữ cửa sổ Mailpit:

```powershell
.\mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
```

Thêm các biến Run sau, rồi Stop/Run lại app:

| Name | Value |
|---|---|
| MAIL_HOST | 127.0.0.1 |
| MAIL_PORT | 1025 |
| MAIL_AUTH | false |
| MAIL_STARTTLS | false |
| MAIL_FROM | demo@f6s.test |
| PUBLIC_BASE_URL | http://localhost:8080 |

Không đặt MAIL_USERNAME/MAIL_PASSWORD khi dùng Mailpit. Đọc mail ở http://127.0.0.1:8025/. Gmail thật dùng smtp.gmail.com:587, bật auth/TLS, Gmail + mật khẩu ứng dụng Google.

**Bản ZIP dùng `MAIL_*`, không phải `SMTP_*`/`OTP_SECRET` của backend Vue cũ.** Không gửi secret lên chat/GitHub.

## 5. Luồng bán hàng

**Online:** Trang chủ → giày/biến thể/số lượng → giỏ → đăng nhập/đăng ký → người nhận → COD/chuyển khoản → đặt đơn → xem Đơn hàng. Giá/khuyến mãi/tồn được server kiểm tra lại.

**Quầy:** Chọn biến thể → kiểm tra số lượng/giá/tồn → khách hoặc khách lẻ → voucher → phương thức → tiền khách đưa/xác nhận nhận chuyển khoản → thanh toán → in. Giỏ/hóa đơn chờ chưa trừ tồn; chỉ hoàn tất sau khi server lưu thành công.

**Online hiện là COD/chuyển khoản thủ công, không phải VNPAY.** Chuyển khoản tạo đơn UNPAID, admin đối chiếu rồi xác nhận, không tự PAID vì bấm Đặt hàng. Cấu hình `shop.bank.name`, `shop.bank.account`, `shop.bank.owner` trước khi nhận tiền thật. ZIP không chứa OTP/VNPAY của backend cũ.

## Kiểm thử

`mvn test`/`mvn package` chạy unit + regression H2 riêng, không đụng DB thật. H2 có mock các query SQL Server-specific, không chứng minh native SQL Server. Bộ native cũ chỉ chạy khi opt-in `-Psqlserver-tests`, trên database thử `shopbangiay_kt_*` đã chuẩn bị.

Sau Maven tests, nếu muốn test browser:

```powershell
npm ci --prefix ui-tests
npx --prefix ui-tests playwright install chromium
npm test --prefix ui-tests
```

Browser dùng HTML render thật từ MockMvc/Thymeleaf; POST bị intercept. Lưu DB/checkout được kiểm tra riêng bằng Maven. Xem [báo cáo đợt này](docs/RELEASE-20261103.md). Các báo cáo cũ trong ZIP chỉ là lịch sử.
