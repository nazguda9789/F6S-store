# 01 — Cài đặt và chạy website

[Mục lục](README.md) · Tiếp: [Kiến trúc và model](02-KIEN-TRUC-VA-MODEL.md)

## 1. Cần tải những gì?

| Thành phần | Bắt buộc? | Vai trò |
|---|---|---|
| [JDK 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html) | Có | Biên dịch và chạy Java |
| [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) | Theo hướng dẫn này | Mở Maven project và chạy hàm main; có thể thay bằng Terminal |
| [XAMPP](https://www.apachefriends.org/download.html) | Theo hướng dẫn này | MariaDB/MySQL và phpMyAdmin; có thể dùng MySQL riêng |
| [Mã nguồn ZIP](https://github.com/nazguda9789/F6S-store/archive/refs/heads/main.zip) | Có | Project, Maven Wrapper, SQL |
| [Git](https://git-scm.com/downloads) | Không | Clone, pull và quản lý phiên bản |
| [ngrok](https://ngrok.com/download) | Chỉ khi cần callback công khai | Đưa web local ra HTTPS để thử Sandbox |
| Node.js | Không để chạy web | Chỉ cần nếu chạy các test JavaScript |

Repository private yêu cầu tài khoản GitHub có quyền. Không cần cài PHP riêng, Node/npm, Tomcat riêng hoặc chép source vào `htdocs`. Spring Boot có máy chủ nhúng, chạy ở cổng 8080. Apache XAMPP phục vụ phpMyAdmin, không chạy ứng dụng Java.

Lần đầu cần Internet để tải Maven/dependency; ảnh ngoài, OAuth và API cũng cần Internet. XAMPP có thể cung cấp MariaDB, dù URL JDBC hiện dùng tiền tố `jdbc:mysql:`; lượt kiểm chứng trong cloud dùng MySQL 8, chưa xác nhận mọi phiên bản MariaDB.

## 2. Lấy mã nguồn

ZIP: GitHub → Code → Download ZIP → giải nén vào `C:\Projects\F6S-store` (đường dẫn không dấu).

Hoặc dùng Git:

```powershell
git clone https://github.com/nazguda9789/F6S-store.git
cd F6S-store
```

Thư mục đúng phải chứa trực tiếp `pom.xml`, `mvnw.cmd`, `src` và `f6s_shop.sql`. Không chạy trong thư mục cha hoặc ngay trong file ZIP. Nếu clone repo private, dùng cơ chế đăng nhập GitHub của Git, không dán token vào lệnh hoặc gửi token qua chat.

## 3. Chuẩn bị database

1. Mở XAMPP Control Panel, Start MySQL. Start Apache nếu dùng phpMyAdmin.
2. Vào `http://localhost/phpmyadmin` (nếu Apache dùng cổng khác, sửa URL theo cổng đó).
3. **Sao lưu database cũ trước khi import**: chọn database → Export → SQL.
4. Cài mới: chỉ import [f6s_shop.sql](../f6s_shop.sql), đã gộp bảng OTP và thuộc tính giày mới. File có `DROP DATABASE IF EXISTS f6s_shop`, sẽ xóa database cùng tên.
5. Đã có database và chỉ cần bổ sung dữ liệu: đọc [danh sách bổ sung](../DANH_SACH_BO_SUNG.md), chọn database `f6s_shop` rồi import [sql/bo_sung_du_lieu.sql](../sql/bo_sung_du_lieu.sql). Không chạy file đầy đủ để cập nhật giao diện.
6. Kiểm tra có các bảng `users`, `product`, `product_detail`, `invoice`… Không import cả hai file chỉ vì cài mới; bản đầy đủ đã gồm dữ liệu bổ sung.

**Database đang chạy:** dừng web, sao lưu, chọn đúng schema rồi chạy [sql/nang_cap_f6s.sql](../sql/nang_cap_f6s.sql). File nâng cấu trúc và đổi SKU hiện hành sang mã do server cấp, lưu mã cũ ở `legacy_sku`; không xóa dữ liệu, không reset tài khoản, không nhập lại sản phẩm mẫu và có thể chạy lại. Xem [mục lục SQL](../sql/README.md). Không cần đặt lại secret.

Nếu đã có dữ liệu và chỉ cập nhật banner/danh mục ngày 19/10, dùng [sql/cap_nhat_noi_dung_20261019.sql](../sql/cap_nhat_noi_dung_20261019.sql), không import lại SQL đầy đủ. Bản cài mới đã có 5 banner, 50 sản phẩm, 13 bài viết; 12 danh mục có ít nhất 3 sản phẩm đang bật. Đăng ký gửi mail cần [cấu hình SMTP/App Password](07-CAP-NHAT-EMAIL-VA-NOI-DUNG.md), không tự hoạt động chỉ nhờ import SQL.

Java đọc nhóm bảng tiếng Anh. Các bảng tiếng Việt được giữ lại trong SQL không tự đồng bộ với nhóm này; sửa `tai_khoan` không đổi đăng nhập trong `users`.

## 4. Mở bằng IntelliJ

1. File → Open → chọn thư mục có `pom.xml`, mở dưới dạng Maven project.
2. File → Project Structure → Project SDK: JDK 17. Kiểm tra Maven runner/importer cũng dùng JDK phù hợp.
3. Maven → Reload All Maven Projects. Tắt Offline mode trong lần tải thư viện đầu.
4. Mở [ShopgiaythethaoApplication.java](../src/main/java/com/web/ShopgiaythethaoApplication.java), Run hàm `main`; Stop để chỉnh cấu hình nếu cần.
5. Run → Edit Configurations → cấu hình ứng dụng → Modify options → Environment variables.

## 5. Thiết lập tối thiểu

Profile mặc định là `dev`. Các giá trị mặc định phục vụ XAMPP local:

| Biến môi trường | Giá trị demo/ý nghĩa |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/f6s_shop` |
| `SPRING_DATASOURCE_USERNAME` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Để trống chỉ khi MySQL thực sự không đặt mật khẩu |
| `SHIPPING_PROVIDER` | `local` — giao hàng do cửa hàng sắp xếp |
| `SERVER_PORT` | `8080`, đổi khi cổng bị chiếm |

Nếu MySQL chạy cổng 3307 thì sửa URL datasource sang 3307, không sửa cổng Apache để giải quyết lỗi kết nối database. Không dùng tài khoản root/mật khẩu trống khi triển khai công khai.

Admin demo trong SQL mới: **`admin@gmail.com` / `123123`** (database lưu BCrypt). Chỉ dùng cho demo, đổi trước khi public. Nếu database đã được import từ bản cũ, chọn đúng database trong phpMyAdmin rồi import [dat_lai_admin_demo.sql](../sql/dat_lai_admin_demo.sql); file chỉ reset admin này nếu đã tồn tại, không tạo mới/nâng quyền user và không xóa sản phẩm/đơn. Pull Git không tự cập nhật database. Không import lại `f6s_shop.sql` để chỉ đổi mật khẩu.

Nếu muốn tạo admin khác hoặc dùng mật khẩu riêng, thêm tạm:

```text
F6S_ADMIN_EMAIL=admin@gmail.com
F6S_ADMIN_PASSWORD=<mật-khẩu-mới-do-bạn-chọn>
F6S_ADMIN_RESET_PASSWORD=true
```

Thay phần trong `< >` bằng mật khẩu của bạn. Chạy một lần, đăng nhập thành công, bỏ `F6S_ADMIN_PASSWORD` và `F6S_ADMIN_RESET_PASSWORD` rồi chạy lại. Với email chưa có, cơ chế khởi tạo có thể tạo admin mới; không nâng quyền hoặc reset tài khoản nhân viên/khách hàng đã tồn tại. Các email nhân viên nguồn là `nhanvien@gmail.com`, `employee@gmail.com`; không biết mật khẩu gốc chỉ từ hash SQL.

Project không tự đọc `.env`. Biến trong cấu hình Run không tự áp dụng cho Terminal hoặc một cấu hình Run khác. Đổi biến cần dừng và chạy lại tiến trình.

## 6. Chạy ứng dụng

Trong IntelliJ: Run `ShopgiaythethaoApplication.main`.

Hoặc PowerShell, đứng tại thư mục chứa `pom.xml` (JDK 17 cần có trong PATH/JAVA_HOME):

```powershell
java -version
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Nếu dùng Terminal, đặt các biến trong chính phiên đó trước khi chạy, ví dụ:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/f6s_shop"
$env:SHIPPING_PROVIDER = "local"
.\mvnw.cmd spring-boot:run
```

macOS/Linux có thể dùng `bash ./mvnw spring-boot:run` với Java và MySQL tương ứng; hướng dẫn XAMPP/IntelliJ chi tiết ở đây tập trung Windows. Dừng bằng Stop hoặc Ctrl+C.

## 7. Kiểm tra sau khi chạy

Chờ console có `Started ShopgiaythethaoApplication`, sau đó:

| Trang | Đường dẫn trên `http://localhost:8080` |
|---|---|
| Trang chủ | `/` |
| Đăng nhập | `/login` |
| Giỏ hàng | `/cart` |
| Tra cứu đơn | `/tra-cuu-don-hang` |
| Hóa đơn admin | `/adm-prj/qtht-001/invoice/list` |
| Tạo hóa đơn admin | `/adm-prj/qtht-001/invoice/create` |
| Hóa đơn nhân viên | `/emp-prj/qtht-001/invoice/list` |
| Doanh thu admin | `/adm-prj/qtht-001/thong-ke/doanh-thu` |

Checklist demo tối thiểu: trang chủ có sản phẩm → mở sản phẩm → chọn size/màu → thêm giỏ → chọn thanh toán → nhập người nhận, số điện thoại, địa chỉ → COD → nhận mã đơn → admin mở chi tiết đơn. Dùng dữ liệu thử, tránh tạo đơn nhầm trên database thật.

Chế độ `local`: chọn tỉnh/thành phố → phường/xã → quận/huyện tự điền. Danh mục 3 cấp là địa chỉ cũ trước 07/2025; có thể nhập tên mới bằng “Dùng địa chỉ”, để trống quận/huyện nếu không có. Phí mặc định 30.000đ, miễn phí từ 500.000đ. Không cần GHN cho demo này.

## 8. Chạy “đầy đủ” nghĩa là gì?

Chạy được trang chủ/COD không chứng minh mọi dịch vụ đã sẵn sàng. Muốn dùng đăng ký/quên mật khẩu cần SMTP; upload cần Cloudinary; VNPay cần merchant Sandbox; GHN cần token/shop/kho; OAuth cần ứng dụng Google/Facebook. Xem [cấu hình tích hợp](05-CAU-HINH-VA-TICH-HOP.md) và kiểm tra từng luồng riêng.

## 9. Cập nhật bản mới

Nếu dùng Git, xem thay đổi local trước khi pull:

```powershell
git status
git pull origin main
```

Giữ lại cấu hình Run và sao lưu database. Không dùng `git reset --hard` để xử lý xung đột khi chưa bảo toàn thay đổi. Nếu dùng ZIP, giải nén bản mới riêng rồi mở đúng project. Dừng app cũ, build/chạy bản mới và Ctrl+F5 trình duyệt. Chỉ import SQL lại khi có hướng dẫn thay đổi dữ liệu cụ thể; cập nhật tài liệu/UI không cần import.
