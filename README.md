# F6S Sports

## Bản Spring Boot + Vue 3 + SQL Server

Kiến trúc mới nằm riêng tại [f6s-store-project](f6s-store-project/README.md). Xem hướng dẫn tại đó nếu chạy Vue/SQL Server. Phần hướng dẫn bên dưới dành cho bản Java/Thymeleaf/MySQL cũ; không trộn hai bộ database hoặc cấu hình.

## Bản Java/Thymeleaf/MySQL hiện có

Website giày thể thao: cửa hàng online, bán tại quầy và quản trị. Java Spring Boot + Thymeleaf + MySQL. Giữ OTP, Google và VNPAY qua cấu hình riêng, không lưu secret trong SQL mẫu.

## Chạy tối thiểu

Cần **JDK 17**, MySQL/XAMPP và Internet để tải Maven dependency lần đầu. Có Maven Wrapper trong repo; **không cần Node/npm, Docker hoặc cài frontend riêng** để chạy web trên máy cá nhân.

1. [Tải bộ SQL phpMyAdmin mới](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-PHPMYADMIN-MINIMAL-20261024.zip) → **Download raw**, giải nén.
2. Import **01_schema.sql → 02_catalog_admin.sql**. Chờ mỗi bước thành công; đây là hai file đủ chạy catalog. Không nhập lại SQL all-in-one cũ.
3. Muốn có thêm nhân viên/khách/đơn mẫu thì import **03_demo_optional.sql**. Đây là bước tùy chọn, không phải yêu cầu khởi động web.
4. Cấu hình datasource và chạy bằng JDK 17.

PowerShell, tại thư mục project:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/f6s_shop'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD=''
.\mvnw.cmd spring-boot:run
```

Thay cổng/mật khẩu MySQL theo máy. IntelliJ: đặt các biến trên trong Run Configuration rồi chạy `ShopgiaythethaoApplication`. Linux/macOS dùng `./mvnw spring-boot:run` với cùng biến môi trường.

Mở **http://localhost:8080**. Admin local: **admin@gmail.com / 123123**, phải đổi trước khi công khai.

**Bản SQL tạo database mới `f6s_shop`, không xóa `f6s_shop` hoặc `f6s_shop_demo` đã nhập dở. Nếu tên database mới đã tồn tại, dừng lại, không tiếp tục bỏ qua lỗi.** Cách xử lý và thứ tự chi tiết: [sql/install/README.md](sql/install/README.md).

## Nếu đang gặp #2014 khi import

Dừng file cũ, làm mới/đăng nhập lại phpMyAdmin để có kết nối mới; nếu còn lỗi trên kết nối mới, khởi động lại Apache trong XAMPP. Không xóa database có dữ liệu thật. Bộ SQL mới không có CALL/DELIMITER và chỉ dùng `FOREIGN_KEY_CHECKS = 0/1`, tránh nguồn nhiều result set trong bản all-in-one.

## Chức năng và dữ liệu

- Online: tìm/lọc giày, màu/size, giỏ, checkout, OTP và VNPAY theo cấu hình, tra cứu đơn.
- Tại quầy: nhiều đơn chờ, khách/khách lẻ, SKU, báo giá voucher server, tiền mặt/tiền thừa, hóa đơn và bản in.
- Quản trị: giao dịch, sản phẩm/biến thể/ảnh, danh mục/hãng/đế/chất liệu/màu/size, nhân viên/khách, giảm giá và nội dung.
- Catalog: **25 giày mẫu, 250 biến thể, 5 nhóm thể thao**, 5 banner và 5 bài viết. Ảnh minh họa có nhãn DEMO, không phải hàng hóa thật được hãng xác nhận.
- File 03 thêm 5 nhân viên/5 khách và dữ liệu nghiệp vụ giả; tài khoản mẫu đang ngừng hoạt động, không có mật khẩu chung. Hóa đơn giả ảnh hưởng báo cáo demo.

SMTP, OAuth, VNPAY, vận chuyển hoặc upload ảnh cần cấu hình dịch vụ tương ứng; SQL không thay thế các secret. Giữ nguyên cấu hình OTP/VNPAY hiện có khi cập nhật code. Xem [cấu hình tích hợp](docs/05-CAU-HINH-VA-TICH-HOP.md).

## Cấu trúc repo

```text
src/main/java/          Backend, bảo mật, nghiệp vụ
src/main/webapp/        Template, CSS/JS, ảnh
src/test/              Kiểm thử
sql/install/           3 file cài mới cho phpMyAdmin
sql/maintenance/       Nâng/chuyển/khôi phục DB đang dùng, không phải bộ cài
scripts/               Tạo lại SQL/ảnh; chỉ người bảo trì cần Node
docs/                  Hướng dẫn hiện hành
downloads/             Chỉ giữ một ZIP cài phpMyAdmin hiện hành
pom.xml, mvnw*         Build và chạy Java
```

Các ZIP/seed/tài liệu lịch sử dễ gây nhầm và hai template hóa đơn nhân viên không còn được controller dùng đã gỡ khỏi nhánh hiện hành, vẫn khôi phục được từ Git history. Admin/nhân viên hiện dùng template hóa đơn chung với navbar/quyền riêng. Không xóa ảnh ứng dụng có thể còn được hóa đơn cũ tham chiếu; file bot/IDE/target đang được Git bỏ qua không bị dọn dữ liệu tại máy người dùng.

## Database thật đang dùng

**Không dùng bộ cài mới để cập nhật database thật.** Backup, dừng web và dùng [hướng dẫn maintenance](sql/README.md). Các file maintenance còn routine, dành cho MySQL CLI; không import chúng chung với bộ phpMyAdmin. Thay catalog có ảnh hưởng sản phẩm/giỏ, phải đối soát thanh toán đang chờ trước.

## Tài liệu và giới hạn

[Mục lục tài liệu](docs/README.md) · [Catalog và dữ liệu](docs/07-CATALOG-VA-DU-LIEU.md) · [Kiểm thử/xử lý lỗi](docs/06-KIEM-THU-VA-XU-LY-LOI.md) · [VNPAY Sandbox](VNPAY_SANDBOX.md).

Đây là bộ trình diễn, chưa chứng nhận production hay tương đương mọi chức năng Bee. Đổi/trả/hoàn tiền, lịch sử kho và đối soát báo cáo còn cần nghiệm thu riêng. Không dùng mật khẩu/demo invoice hoặc hàng minh họa để kinh doanh thật.
