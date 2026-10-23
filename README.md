# F6S — Website quản lý bán giày thể thao

F6S là ứng dụng Java Spring Boot phục vụ khách mua giày online và nhân viên bán hàng tại quầy, kèm khu vực quản trị sản phẩm, đơn hàng và doanh thu. Giao diện dùng Thymeleaf, Bootstrap và JavaScript; dữ liệu lưu bằng MySQL/MariaDB.

> Các mục chức năng dưới đây được ghi nhận từ mã nguồn hiện tại, không có nghĩa tất cả đã được kiểm thử hoàn chỉnh. Xem phần trạng thái kiểm chứng ở cuối tài liệu.

## Bộ hướng dẫn đầy đủ

**Giao dịch tương đồng Bee:** thêm tạo hóa đơn chờ nhanh, picker khách hàng, kiểm tra số lượng/tồn kho và bộ lọc loại giao dịch kết hợp từ khóa/trạng thái/ngày. [Phạm vi và cách dùng](docs/11-GIAO-DICH-TUONG-DONG-BEE.md). Đợt giao dịch này không cần SQL hay secret mới.

**SQL đã gộp module mới (23/10):** cài mới chỉ import [f6s_shop.sql](f6s_shop.sql), đã có bảng OTP và thuộc tính thương hiệu/đế giày/chất liệu. Máy đang có database: sao lưu rồi chạy [sql/nang_cap_f6s.sql](sql/nang_cap_f6s.sql), không import lại dump đầy đủ. [Chọn đúng file SQL](sql/README.md) · [Phạm vi module](docs/10-DOI-CHIEU-VIDEO-BEE.md). Không cần secret mới.

| Mục | Nội dung |
|---|---|
| [Mục lục tài liệu](docs/README.md) | Chọn hướng dẫn theo người chạy demo, người nhận code hoặc người viết báo cáo |
| [01 — Cài đặt và chạy web](docs/01-CAI-DAT-VA-CHAY.md) | Cần tải gì, IntelliJ + XAMPP, import SQL, admin, chạy và cập nhật |
| [02 — Kiến trúc và model](docs/02-KIEN-TRUC-VA-MODEL.md) | MVC, cấu trúc source, entity/DTO, bảng và sơ đồ quan hệ |
| [03 — Chức năng và phân quyền](docs/03-CHUC-NANG-VA-PHAN-QUYEN.md) | Module, màn hình, vai trò, đường dẫn, API và trạng thái hoàn thiện |
| [04 — Luồng nghiệp vụ](docs/04-LUONG-NGHIEP-VU.md) | Giỏ, COD, VNPay, địa chỉ, tại quầy, trạng thái, tài khoản và thống kê |
| [05 — Cấu hình và tích hợp](docs/05-CAU-HINH-VA-TICH-HOP.md) | Biến môi trường, email, upload, OAuth, GHN, VNPay/ngrok và bot |
| [06 — Kiểm thử và xử lý lỗi](docs/06-KIEM-THU-VA-XU-LY-LOI.md) | Lệnh test, checklist, lỗi thường gặp và giới hạn trước production |
| [07 — Sửa email và cập nhật nội dung](docs/07-CAP-NHAT-EMAIL-VA-NOI-DUNG.md) | Cấu hình gửi mã đăng ký, 5 banner từ source cũ, 12 danh mục đủ ít nhất 3 sản phẩm |
| [08 — Đăng nhập Google và xác thực](docs/08-DANG-NHAP-GOOGLE-VA-XAC-THUC.md) | Sửa Google, chỉ tạo tài khoản sau OTP, điều hướng an toàn và xử lý trang 404 |

Phần dưới là hướng dẫn nhanh; bộ tài liệu trên giải thích chi tiết theo mã nguồn hiện tại. Không cần bật GitHub Wiki hay cài thêm phần mềm để đọc.

Cập nhật đăng nhập 19/10: đăng ký email mới chỉ lưu yêu cầu chờ trong `pending_registration`, chưa thêm vào `users` trước OTP. Google yêu cầu email đã xác minh, lấy đúng ID/quyền tài khoản và không đưa người dùng về URL cache sai. Cấu hình **cả Client ID và Client Secret** của Google Web application; xem [hướng dẫn Google/OTP và schema cập nhật](docs/08-DANG-NHAP-GOOGLE-VA-XAC-THUC.md). Không tự xóa hoặc kích hoạt hàng loạt tài khoản chờ cũ.

Cập nhật 19/10/2026: sửa đăng ký báo thành công dù email gửi thất bại; SMTP thiếu/sai cấu hình sẽ báo lỗi và rollback đăng ký. Bạn vẫn cần đặt `MAIL_USERNAME`/`MAIL_PASSWORD` hợp lệ trên máy chạy (không có khóa gửi mail chung trong Git). Đã khôi phục 3 slider + 2 banner phụ từ ảnh source cũ và bổ sung 4 liên kết để cả 12 danh mục trong seed có ít nhất 3 sản phẩm đang bật; không tạo sản phẩm giả. Database cũ chỉ cần import [script cập nhật nội dung](sql/cap_nhat_noi_dung_20261019.sql), không import lại SQL đầy đủ. [Chi tiết cách cấu hình và danh sách thay đổi](docs/07-CAP-NHAT-EMAIL-VA-NOI-DUNG.md).

Đợt sửa lỗi và làm mới giao diện ngày 18/10/2026: xem [chi tiết những nội dung đã thêm/thay](CAP_NHAT_UI_VA_LOI.md). Không cần import lại SQL. Mặc định `SHIPPING_PROVIDER=local`: chọn/tìm Tỉnh/Thành phố → Phường/Xã → tự điền Quận/Huyện, hỗ trợ nhập địa chỉ mới ngoài danh mục cũ trước 07/2025; cửa hàng tự sắp xếp vận chuyển. Phí mặc định 30.000đ, miễn phí từ 500.000đ. Chỉ đặt `SHIPPING_PROVIDER=ghn` khi có tài khoản GHN hợp lệ; chế độ GHN vẫn chặn xác nhận khi không tải được địa chỉ/phí vận chuyển.

## 1. Website hiện có những gì?

| Khu vực | Chức năng có trong mã nguồn |
|---|---|
| Khách hàng | Trang chủ, danh mục, tìm/lọc sản phẩm, chi tiết giày theo size/màu, giỏ hàng, đặt hàng và thanh toán, tài khoản cá nhân, tra cứu/theo dõi đơn, bài viết, giới thiệu và liên hệ |
| Bán hàng tại quầy | Màn tạo hóa đơn cho admin/nhân viên, chọn sản phẩm và khách hàng, xử lý hóa đơn/thanh toán |
| Nhân viên | Quản lý sản phẩm, hóa đơn/chi tiết đơn, khách hàng, bài viết, liên hệ và chat |
| Admin | Dashboard; quản lý sản phẩm, danh mục, size, màu, banner, khách hàng và tài khoản; hóa đơn; voucher và đợt giảm giá; bài viết; cấu hình Cloudinary |
| Thống kê | Có màn doanh thu, số đơn hoàn tất, doanh thu ngày/tháng và sản phẩm bán chạy; cần kiểm thử số liệu với dữ liệu giao dịch thực tế |
| Xác thực/phân quyền | Đăng nhập bằng email/mật khẩu, đăng xuất, BCrypt, phân quyền admin/nhân viên/khách hàng, khóa đăng nhập sai; có tích hợp Google/Facebook |
| Dịch vụ ngoài | Email OTP/quên mật khẩu, Cloudinary, VNPay sandbox, GHN, Telegram, chatbot Gemini/Groq; chỉ dùng được khi cấu hình dịch vụ phù hợp |

Mã nguồn chưa được xác nhận đáp ứng toàn bộ yêu cầu báo cáo theo phương thức thanh toán, kênh online/tại quầy hoặc cảnh báo sắp hết hàng. Nhóm cần kiểm tra và hoàn thiện các nghiệp vụ đó, cùng việc đồng bộ tồn kho giữa hai kênh bán.

## 2. Cần tải và cài những gì?

Hướng dẫn chính bên dưới dành cho Windows.

| Phần mềm | Mục đích | Link tải |
|---|---|---|
| JDK 17 | Biên dịch/chạy Java; chọn bản 64-bit phù hợp máy | [Amazon Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html) |
| IntelliJ IDEA | Mở và chạy project; bản miễn phí hỗ trợ Java/Maven | [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) |
| XAMPP | Cung cấp MariaDB/MySQL và phpMyAdmin | [XAMPP](https://www.apachefriends.org/download.html) |
| Mã nguồn F6S | Bao gồm Maven Wrapper và SQL | [Download ZIP](https://github.com/nazguda9789/F6S-store/archive/refs/heads/main.zip) |
| Git — tùy chọn | Clone/pull bản mới, không cần nếu dùng ZIP | [Git](https://git-scm.com/downloads) |

Repository private yêu cầu đăng nhập GitHub bằng tài khoản có quyền truy cập.

Không cần cài Node.js/npm cho cấu trúc project hiện tại. Maven đã có sẵn thông qua `mvnw.cmd`; lần chạy đầu cần Internet để tải thư viện. Apache trong XAMPP phục vụ phpMyAdmin, còn Spring Boot phục vụ website ở cổng 8080.

Ảnh mẫu dùng URL Cloudinary bên ngoài nên cũng cần Internet. Tài khoản/khóa dịch vụ ngoài được mô tả ở mục 7.

## 3. Tải project và mở bằng IntelliJ

1. Tải ZIP từ link trên và giải nén vào đường dẫn không dấu, ví dụ `C:\Projects\F6S-store`.
2. Kiểm tra thư mục mở có trực tiếp `pom.xml`, `mvnw.cmd` và thư mục `src`. Tránh mở nhầm thư mục cha sau khi giải nén.
3. IntelliJ → **File → Open**, chọn thư mục chứa `pom.xml`, cho phép mở dưới dạng Maven project.
4. **File → Project Structure → Project → SDK**: chọn JDK 17. Có thể dùng **Download JDK** nếu chưa cài.
5. Trong cửa sổ Maven, chọn **Reload All Maven Projects** và đợi tải thư viện; tắt Offline mode nếu đang bật.

## 4. Chuẩn bị database bằng XAMPP

1. Mở XAMPP Control Panel, Start **MySQL** và **Apache**.
2. Truy cập `http://localhost/phpmyadmin`.
3. Chọn đúng phương án import:

| Tình huống | File cần import |
|---|---|
| Cài mới, chấp nhận tạo lại database | `f6s_shop.sql` |
| Đã có database, chỉ nâng cấu trúc OTP / module giày | Chọn database đang dùng, import `sql/nang_cap_f6s.sql` |
| Đã có database, cần thêm dữ liệu nguồn | Chọn database `f6s_shop`, import `sql/bo_sung_du_lieu.sql` |

**File đầy đủ có `DROP DATABASE IF EXISTS f6s_shop`, sẽ xóa database cùng tên. Sao lưu trước khi import nếu đã có dữ liệu.** Không import cả hai file chỉ để cài mới; file đầy đủ đã bao gồm dữ liệu bổ sung.

Dữ liệu nguồn gồm 50 sản phẩm, 13 bài viết, 1 admin và 2 nhân viên, kèm danh mục, size, màu và ảnh. Giữ 2 sản phẩm mẫu trong bảng tiếng Việt. Xem danh sách chi tiết tại [DANH_SACH_BO_SUNG.md](DANH_SACH_BO_SUNG.md).

Ứng dụng Java dùng các bảng `users`, `product`, `product_detail`, `blog`, `invoice`… SQL còn giữ các bảng tiếng Việt như `tai_khoan`, `san_pham`, `bai_viet` để bảo toàn dữ liệu hiện có. **Hai nhóm bảng không tự đồng bộ sau import**; thay mật khẩu hoặc sản phẩm chỉ trong bảng tiếng Việt sẽ không thay đổi dữ liệu mà Java đọc.

## 5. Cấu hình database và tài khoản admin

Profile mặc định là `dev`, đọc `src/main/resources/application-dev.properties`. Kết nối mặc định:

- Database: `f6s_shop`
- Host/cổng: `localhost:3306`
- User: `root`
- Mật khẩu: trống theo mặc định XAMPP

Nếu máy dùng cổng hoặc mật khẩu khác, đặt biến trong IntelliJ → **Run → Edit Configurations → Environment variables** (có thể cần chọn **Modify options → Environment variables**):

| Biến | Giá trị |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/f6s_shop` — sửa cổng nếu cần |
| `SPRING_DATASOURCE_USERNAME` | Username MySQL của bạn |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu MySQL của bạn |

Tạo cấu hình Run bằng cách mở `src/main/java/com/web/ShopgiaythethaoApplication.java`, chọn Run cạnh hàm `main`, sau đó Stop để chỉnh Environment variables.

SQL chứa các email đăng nhập sau. Admin demo đã đặt mật khẩu `123123` (lưu BCrypt trong database); nhân viên vẫn giữ hash gốc, chưa biết mật khẩu rõ:

| Vai trò | Email |
|---|---|
| Admin | `admin@gmail.com` |
| Nhân viên | `nhanvien@gmail.com` |
| Nhân viên | `employee@gmail.com` |

**Admin demo: `admin@gmail.com` / `123123`. Chỉ dùng demo, đổi trước khi công khai web.** Cài mới bằng SQL mới có thể dùng ngay. Nếu đã import database trước đó, chọn đúng database trong phpMyAdmin rồi import [sql/dat_lai_admin_demo.sql](sql/dat_lai_admin_demo.sql) để đổi riêng admin có sẵn; không import lại file SQL đầy đủ vì sẽ xóa database. File reset không nâng quyền nhân viên/khách hàng và không tạo admin nếu chưa tồn tại. Git pull không tự đổi mật khẩu database đang chạy.

### Đặt mật khẩu mới cho admin

Thêm vào Environment variables của cấu hình Run:

| Biến | Giá trị |
|---|---|
| `F6S_ADMIN_EMAIL` | `admin@gmail.com` |
| `F6S_ADMIN_PASSWORD` | Mật khẩu mới bạn tự chọn |
| `F6S_ADMIN_RESET_PASSWORD` | `true` |

Chạy ứng dụng để lưu mật khẩu BCrypt và mở khóa admin trong bảng `users`. Sau khi đăng nhập thành công, bỏ biến reset và mật khẩu khỏi cấu hình Run, rồi chạy lại. Không cần import lại SQL.

Để tạo admin mới, dùng email chưa tồn tại và đặt `F6S_ADMIN_EMAIL`/`F6S_ADMIN_PASSWORD`; không cần cờ reset. Cơ chế này không nâng quyền hoặc đặt lại mật khẩu cho tài khoản nhân viên/khách hàng có sẵn.

Biến đặt trong Run Configuration chỉ áp dụng khi chạy bằng cấu hình đó; lệnh Terminal không tự nhận chúng. Project không tự nạp file `.env`.

## 6. Chạy và kiểm tra website

Trong IntelliJ, nhấn Run ở `ShopgiaythethaoApplication.main`. Hoặc mở PowerShell tại thư mục chứa `pom.xml`:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Nếu cần đặt biến khi chạy Terminal, khai báo trong chính phiên PowerShell trước khi chạy, ví dụ:

```powershell
$env:F6S_ADMIN_EMAIL = "admin@gmail.com"
$env:F6S_ADMIN_PASSWORD = "<mat-khau-moi-cua-ban>"
$env:F6S_ADMIN_RESET_PASSWORD = "true"
.\mvnw.cmd spring-boot:run
```

Sau khi đặt lại mật khẩu, dừng ứng dụng bằng Ctrl+C và xóa biến khỏi phiên:

```powershell
Remove-Item Env:F6S_ADMIN_PASSWORD
Remove-Item Env:F6S_ADMIN_RESET_PASSWORD
```

Khi console có `Started ShopgiaythethaoApplication`, kiểm tra:

| Trang | URL |
|---|---|
| Trang chủ | http://localhost:8080/ |
| Đăng nhập | http://localhost:8080/login |
| Dashboard admin | http://localhost:8080/adm-prj/qtht-001/dashbroad/view |
| Hóa đơn admin | http://localhost:8080/adm-prj/qtht-001/invoice/list |
| Tạo hóa đơn tại quầy — admin | http://localhost:8080/adm-prj/qtht-001/invoice/create |
| Hóa đơn nhân viên | http://localhost:8080/emp-prj/qtht-001/invoice/list |
| Thống kê doanh thu | http://localhost:8080/adm-prj/qtht-001/thong-ke/doanh-thu |

Các trang nội bộ yêu cầu vai trò tương ứng. Kiểm tra trang chủ có sản phẩm, mở chi tiết size/màu, đăng nhập admin, mở quản lý hóa đơn. Thông báo Started chưa chứng minh các nghiệp vụ thanh toán, email hoặc vận chuyển đã hoạt động.

Dừng bằng nút Stop trong IntelliJ hoặc Ctrl+C trong Terminal.

## 7. Cấu hình để dùng các tích hợp

Cài phần mềm và import SQL giúp chuẩn bị chạy local. Để dùng các chức năng tích hợp, cần tài khoản dịch vụ của chính bạn; không chỉ tải thêm phần mềm.

| Tích hợp | Cần chuẩn bị và cấu hình |
|---|---|
| Email OTP, quên mật khẩu, email hóa đơn | Gmail và App Password hợp lệ; đặt `MAIL_USERNAME`, `MAIL_PASSWORD`. Code hiện dùng SMTP Gmail, cổng 587, STARTTLS. Kiểm tra gửi email thật trước khi thử đăng ký/quên mật khẩu |
| Upload ảnh Cloudinary | Tạo tài khoản Cloudinary, đăng nhập admin → quản lý Cloudinary, thêm cloud name/API key/API secret và chọn cấu hình chính. Dữ liệu lưu bảng `cloud_key`; biến `CLOUDINARY_URL` không được service hiện tại sử dụng |
| Google/Facebook login | Tạo ứng dụng OAuth của bạn, thay client ID trong profile dev và đặt `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` hoặc `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_SECRET`. Cấu hình redirect URI tương ứng `http://localhost:8080/login/oauth2/code/google` hoặc `/facebook` trên trang nhà cung cấp |
| VNPay | Tài khoản sandbox, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`; xem [hướng dẫn IntelliJ/ngrok](VNPAY_SANDBOX.md). Code mặc định gọi VNPay sandbox, không phải môi trường thu tiền thật |
| GHN — tùy chọn | Mặc định giao hàng nội bộ không cần GHN. Để dùng GHN: đặt `SHIPPING_PROVIDER=ghn`, `GHN_API_KEY` (hoặc `GHN_TOKEN`), `GHN_SHOP_ID`, chỉnh `ghn.base-url` đúng dev/production và địa chỉ kho `GHN_FROM_DISTRICT_ID`/`GHN_FROM_WARD_CODE`. Chế độ GHN cần cấu hình hợp lệ để xác nhận đơn |
| Telegram | Nếu cần bot, cấu hình `TELEGRAM_BOT_TOKEN` và `telegram.bot.username`. Khi thiếu token hoặc username, phần đăng ký bot thủ công được bỏ qua |
| Chatbot AI | Đặt `GEMINI_API_KEY`/`GROQ_API_KEY` cho nhà cung cấp được sử dụng; phụ thuộc Internet, model và hạn mức API |
| SMS | Có mã tích hợp SpeedSMS; cần kiểm tra luồng thực tế, `speedsms.api.key`/`speedsms.api.url` và client `SpeedSMSAPI` dùng `SPEEDSMS_TOKEN`. Chưa có một cấu hình thống nhất được xác minh cho mọi luồng SMS |

Không đưa mật khẩu hoặc khóa thật vào README/Git. Các tích hợp hiện chưa được kiểm thử đầy đủ; cần kiểm tra từng luồng với tài khoản dịch vụ hợp lệ. Việc thiếu khóa có thể khiến chức năng tương ứng lỗi khi sử dụng dù web đã khởi động.

## 8. Xử lý lỗi thường gặp

| Lỗi | Cách kiểm tra/xử lý |
|---|---|
| `InvalidPathException`, đường dẫn bị đổi thành dấu ? | Giải nén/mở project ở đường dẫn không dấu, ví dụ `C:\Projects\F6S-store` |
| `ClassNotFoundException: com.web.ShopgiaythethaoApplication` | Mở đúng thư mục có pom.xml, chọn JDK 17, reload Maven, chạy clean compile; xử lý lỗi biên dịch đầu tiên nếu có |
| `Communications link failure` | Bật MySQL XAMPP, kiểm tra host/cổng trong datasource |
| `Unknown database` | Kiểm tra đã import đúng `f6s_shop` |
| `Access denied for user` | Sửa username/password MySQL trong Environment variables |
| `tokenUri cannot be empty` | Dùng bản code mới; kiểm tra không có biến môi trường token URI được đặt rỗng ghi đè địa chỉ mặc định |
| `Bot token and username can't be empty` | Dùng bản mới có kiểm tra thiếu cấu hình; chỉ khai báo Telegram khi dùng bot |
| `Error resolving template [user/index]` | Reload Maven rồi clean compile; pom.xml phải copy views vào templates và profile dev dùng `classpath:/templates/` |
| `danhMucSp.slug` trên null | Cập nhật bản mới có kiểm tra dữ liệu rỗng; import dữ liệu bổ sung nếu database chưa có danh mục |
| `Port 8080 was already in use` | Dừng tiến trình cũ hoặc đặt `SERVER_PORT=8081`; cập nhật URL truy cập và callback tích hợp tương ứng |
| Không thấy ảnh/tải ảnh lỗi | Kiểm tra Internet và URL ảnh; upload cần cấu hình Cloudinary chính |
| Maven không tải thư viện | Kiểm tra Internet/proxy, tắt Offline mode, reload Maven; không bỏ qua lỗi build |

## 9. Cấu trúc project và trạng thái kiểm chứng

```text
pom.xml                         Maven, Java 17, Spring Boot 3.4.3
mvnw / mvnw.cmd                 Maven Wrapper
src/main/java/com/web/          Controller, service, entity, repository, security
src/main/resources/             Cấu hình dev/prod
src/main/webapp/views/          Template Thymeleaf
src/main/webapp/css, js, lib/   Tài nguyên giao diện
src/test/                       Kiểm thử
f6s_shop.sql                    Database đầy đủ, có lệnh tạo lại
sql/bo_sung_du_lieu.sql         Bổ sung dữ liệu vào database đang dùng
DANH_SACH_BO_SUNG.md            Danh sách sản phẩm, bài viết, tài khoản đã nhập
```

Đã kiểm tra bản SQL đầy đủ import được trên MySQL 8, bản bổ sung chạy lại không tăng số dòng và các liên kết sản phẩm–danh mục/size/màu không bị thiếu. Đã bổ sung kiểm thử cho cơ chế đặt lại mật khẩu admin.

Ngày 18/10/2026 đã build bằng JDK 17; các lượt kiểm chứng gần nhất đạt 22 unit test Java chọn lọc và 9 test JavaScript, khởi động Spring Boot với MySQL 8. Trình duyệt đã kiểm tra các trang chính, tạo thành công đơn COD bằng địa chỉ local, xác nhận địa chỉ/phí ship lưu đúng và đăng nhập admin kiểm tra header sáng trên nền navy. Đã kiểm tra tìm/chọn địa chỉ, tự điền quận, nhập địa chỉ mới và tình huống không tải được danh mục. Chưa kiểm chứng MariaDB/XAMPP, thanh toán thật, API dịch vụ bằng khóa thật hoặc toàn bộ nghiệp vụ từ đầu đến cuối. Xem [kiểm thử và giới hạn](docs/06-KIEM-THU-VA-XU-LY-LOI.md) và [chi tiết cập nhật](CAP_NHAT_UI_VA_LOI.md).
