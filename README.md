# F6S Sports — Vue 3 + Spring Boot + SQL Server

Bản phát triển chính hiện nằm tại `backend/` và `frontend/`. Giữ thương hiệu F6S, giao diện xanh rừng/kem/lime và nghiệp vụ quản lý bán giày.

Đợt cập nhật giao hàng: tách hiển thị giao hàng/thanh toán; khách yêu cầu hủy và admin duyệt; giao thất bại/giao lại/nhận hoàn; hàng chờ VNPAY quá hạn để admin đối soát trước khi hủy. [Hướng dẫn và cách sao lưu ảnh](docs/FULFILLMENT-AND-MEDIA.md). Database đã nâng 03/04 chạy thêm `database/05_upgrade_fulfillment.sql` sau khi backup và dừng backend.

Đã bổ sung đăng ký/đăng nhập khách bằng email OTP, VNPAY có kiểm tra IPN, phí giao hàng/vận đơn demo và hồ sơ hoàn toàn bộ tiền. [Hướng dẫn chạy demo online/offline, hộp thư OTP cục bộ và cấu hình VNPAY](docs/DEMO-CHECKOUT-PAYMENTS.md). Database đã có module Bee chạy thêm `database/04_upgrade_integrations.sql` sau khi sao lưu và dừng backend.

Đợt hoàn thiện theo Bee: [module, phân quyền, ảnh và cách nâng cấp database không xóa dữ liệu](docs/BEE-MODULES.md). Database đã có dữ liệu dùng `database/03_upgrade_bee_modules.sql`, không chạy lại schema 01.

```text
backend/     Spring Boot 3, JPA, Validation, Security, SQL Server
frontend/    Vue 3, Composition API, Pinia, Vite, Tailwind
database/    T-SQL tạo database và truy vấn thống kê
docs/        Hướng dẫn chạy, API, phạm vi chuyển đổi
```

## Chạy bản mới

1. Cài JDK 17, Maven 3.9+, Node 20.19+ hoặc 22.12+, SQL Server 2019/2022 và SSMS.
2. Trong SSMS chạy [database/00_create_database.sql](database/00_create_database.sql) để tạo **database trống** `f6s_shop`. Nếu đã tồn tại, dừng và sao lưu/kiểm tra; không xóa hoặc nhập chồng. **Không import SQL MySQL/XAMPP vào SQL Server.**
   Muốn có bảng và sản phẩm trực tiếp từ SQL: xem [bộ schema + catalog SQL Server](database/README.md), chạy **01_schema.sql → 02_real_products.sql** nếu database chưa có bảng. Backend đã tạo bảng thì bỏ qua 01, chỉ import 02 sau khi backup. Bộ này có 25 tên model có thật, giá/màu/size/tồn demo; ảnh thật chưa xác minh.
3. Mở `backend/pom.xml` trong IntelliJ, đặt `DB_PASSWORD`, `ADMIN_PASSWORD` (ít nhất 12 ký tự), `SEED_DEMO=true`; chạy `SneakerApplication`. API: `http://localhost:8081`.
4. Trong `frontend`: `npm ci` rồi `npm run dev`. Cửa hàng: `http://localhost:5173`, quản trị: `http://localhost:5173/admin`.
5. Đăng nhập quản trị bằng `ADMIN_USERNAME` (mặc định `admin`) và `ADMIN_PASSWORD` đã đặt, không dùng tài khoản admin MySQL cũ.

**Hướng dẫn chi tiết:** [Vue/SQL Server](docs/VUE-SQLSERVER.md) · [API và nghiệp vụ](docs/API-RETAIL.md) · [Cấu hình backend](backend/src/main/resources/application.yml).

## Đã có trong bản mới

- Catalog, lọc sản phẩm, PDP, giỏ hàng Pinia, checkout COD.
- Sinh ma trận màu × size ở backend, giá và tồn kho riêng, giữ SKU khi chỉnh sửa, tem QR từng biến thể.
- POS tối đa 5 hóa đơn tạm đang mở mỗi nhân viên, lưu server, quét QR/SKU, tiền khách đưa/tiền thừa, in hóa đơn.
- Khuyến mãi % hoặc số tiền, giới hạn danh sách sản phẩm, thời gian, lượt dùng và mức giảm tối đa.
- Đơn hàng: chốt đơn online, hủy đơn chờ hoàn tồn/lượt mã đúng một lần.
- Dashboard: doanh thu ngày/tháng theo giờ Việt Nam, tỷ lệ thành công và top sản phẩm bán chạy.

Các thao tác ghi nhạy cảm được kiểm tra tại backend; hóa đơn tạm không giữ chỗ tồn kho. Đây là bản phát triển, chưa chứng nhận production hoặc giống toàn bộ Bee. Video chưa được xem trực tiếp trong phiên; giao diện dựa trên ảnh tham chiếu đã có.

## Dữ liệu và kiểm thử

`SEED_DEMO=true` trên database chưa có danh mục tạo 5 danh mục, 25 sản phẩm, 250 biến thể; không tạo hóa đơn doanh thu hay biên lai ngân hàng giả. Tắt seed sau lần đầu.

Backend: `cd backend` → `mvn test`. Frontend: `cd frontend` → `npm test`, `npm run build`. E2E: xem hướng dẫn vì các test tạo đơn/trừ tồn trong database test riêng. Kiểm thử tự động backend dùng H2 MSSQL mode, không thay thế kiểm thử runtime SQL Server; xem giới hạn trong tài liệu.

## Bản cũ và tích hợp chưa chuyển

Source `src/`, `pom.xml`, Maven Wrapper gốc cùng `sql/`, `downloads/` là **bản Java/Thymeleaf/MySQL cũ được giữ để đối chiếu/rollback**. Không chạy `mvn spring-boot:run` tại gốc nếu muốn dùng bản mới; phải vào `backend/`. [Hướng dẫn MySQL cũ](docs/01-CAI-DAT-VA-CHAY.md) chỉ áp dụng bản cũ.

Database, tài khoản, Google login và secret cũ **không tự chuyển sang bản SQL Server**. Không xóa database cũ hoặc copy nguyên application.properties sang backend mới. Bản mới có COD online/tiền mặt POS, tài khoản khách qua email OTP, VNPAY bật bằng cấu hình merchant, vận đơn thủ công và hoàn tiền có chứng từ ngoài hệ thống. Chưa tự gọi API hãng vận chuyển hay VNPAY refund API; chưa có đối soát ngân hàng tự động hoặc migration dữ liệu thật từ hệ thống cũ. Không thay đổi callback của merchant đang chạy trước khi nghiệm thu bản mới.
