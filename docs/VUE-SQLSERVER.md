# Chạy F6S — Vue 3 / SQL Server

Đây là bản phát triển chính tại `backend/` và `frontend/`, không còn đặt trong `f6s-store-project`. Source Thymeleaf/MySQL cũ tại `src/`, `pom.xml`, SQL cũ vẫn được giữ để rollback. Không trộn hai bản. Chi tiết nghiệp vụ và request: [API-RETAIL.md](API-RETAIL.md).

## 1. Chuẩn bị SQL Server

Cài JDK **17**, Maven 3.9+ (hoặc Maven tích hợp IntelliJ), **Node 20.19+ / 22.12+**, npm, SQL Server 2019/2022 Developer/Express và SSMS. SQL Server là hệ quản trị khác MySQL của XAMPP.

Bật TCP/IP trong SQL Server Configuration Manager, đặt port 1433, bật SQL Server Authentication/Mixed Mode nếu dùng tài khoản SQL, restart dịch vụ. Trong SSMS chạy [00_create_database.sql](../database/00_create_database.sql). Script chỉ CREATE database mới, không DROP; nếu `f6s_shop` đã có dữ liệu, dừng và sao lưu/kiểm tra. Có thể dùng database mới tên khác rồi đổi DB_URL. **Không import bộ SQL phpMyAdmin vào SQL Server**.

Tùy chọn Docker: copy `.env.example` ở gốc thành `.env`, đặt mật khẩu mạnh riêng, `docker compose up -d`. Image Developer cho test/dev, ACCEPT_EULA=Y nghĩa là bạn chấp nhận Microsoft EULA. Đợi SQL Server ready, kết nối SSMS `localhost,1433`, chạy script tạo database. Không dùng `docker compose down -v` nếu cần giữ dữ liệu.

## 2. Backend

Mở [backend/pom.xml](../backend/pom.xml) trong IntelliJ như Maven project. Project SDK và Run JRE = 17. Run Configuration → Environment variables, điền giá trị thật:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=f6s_shop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true
DB_USERNAME=sa
DB_PASSWORD=mat-khau-SQL-Server-cua-ban
ADMIN_USERNAME=admin
ADMIN_PASSWORD=mat-khau-quan-tri-rieng-it-nhat-12-ky-tu
SEED_DEMO=true
```

Dùng bảng Environment Variables của IntelliJ để chuỗi JDBC có dấu `;` không bị cắt thành nhiều biến. `sa` chỉ dùng trên máy demo. Mật khẩu phải do bạn đặt, không commit secret. `ADMIN_PASSWORD` không phải mật khẩu MySQL hay tài khoản admin cũ. `.env` không được Spring Boot tự đọc; phải đặt các biến trong IntelliJ hoặc terminal.

Nếu Run Configuration kế thừa `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD` của bản cũ, các biến đó có thể ghi đè `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`. Hãy dùng Run Configuration riêng cho backend mới, không kế thừa URL MySQL; không sửa hoặc xóa cấu hình đang dùng của web cũ.

Chạy `com.example.sneaker.SneakerApplication`; hoặc PowerShell:

```powershell
cd backend
$env:DB_PASSWORD='mat-khau-SQL-Server-cua-ban'
$env:ADMIN_PASSWORD='mat-khau-quan-tri-rieng-it-nhat-12-ky-tu'
$env:SEED_DEMO='true'
mvn spring-boot:run
```

API mặc định cổng **8081**. `http://localhost:8081/api/products` phải trả JSON. [application.yml](../backend/src/main/resources/application.yml) dùng JDBC SQL Server, Unicode nationalized, `ddl-auto=update` cho local. Không ép dialect MySQL vào đây. Schema lỗi thì dừng khởi động, không tiếp tục seed.

`SEED_DEMO=true` chỉ tạo khi chưa có bất kỳ danh mục: 5 danh mục, 25 sản phẩm, 250 biến thể, size hết hàng để kiểm tra UI. Không tạo đơn/biên lai doanh thu giả, không tạo OTP hay secret. Tắt seed sau lần đầu. Hình ảnh SVG và sản phẩm chỉ là minh họa, không phải ảnh/thông số hãng đã xác minh.

## 3. Frontend

```powershell
cd frontend
npm ci
npm run dev
```

Mở **http://localhost:5173**, quản trị **http://localhost:5173/admin**. Dùng ADMIN_USERNAME / ADMIN_PASSWORD ở trên. Credential chỉ giữ trong bộ nhớ trình duyệt, không localStorage; reload phải đăng nhập lại. Hóa đơn tạm POS nằm trên server, đăng nhập lại sẽ tải lại được.

Vite proxy `/api` → backend 8081. Nếu đổi cổng API, đặt `API_PROXY_TARGET` trước `npm run dev`. VITE_API_BASE là biến công khai lúc build, không dùng để lưu secret. Backend cho CORS origin FRONTEND_ORIGIN mặc định localhost:5173; production ưu tiên reverse proxy cùng origin.

## 4. Thử các màn nghiệp vụ

1. Quản trị → Sản phẩm → Thêm sản phẩm: chọn danh mục, nhập danh sách màu và size, giá/tồn mặc định → Sinh tổ hợp → chỉnh từng dòng → Lưu. Có thể thêm nhanh danh mục.
2. Danh sách sản phẩm → Mã QR: xem/in tem từng biến thể. SKU không thay đổi khi sửa cùng màu–size.
3. Khuyến mãi → tạo mã %/tiền, thời gian, số lượt, chọn sản phẩm áp dụng. Không tự áp toàn catalog.
4. POS → tạo tối đa 5 hóa đơn tạm → quét SKU/QR hoặc chọn biến thể còn hàng → áp mã/tính lại → nhập tiền khách đưa → chốt → In hóa đơn. Không có giữ chỗ kho lúc mới thêm vào hóa đơn tạm.
5. Đơn hàng → xác nhận COMPLETED chỉ sau giao/thu tiền, hoặc hủy PENDING để hoàn tồn. Không hỗ trợ tự hoàn tiền đơn đã hoàn thành.
6. Tổng quan → lọc ngày/tháng, xem doanh thu thuần, tỷ lệ thành công, sản phẩm bán chạy.

## 5. Kiểm thử

```text
cd backend
mvn test
cd ../frontend
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

Backend test dùng **H2 MSSQL compatibility mode**, 25 test hiện có, gồm CRUD/checkout và matrix/POS/promotion/dashboard. Không chạy test này trên database thật. E2E chỉ dùng database demo riêng: test tạo sản phẩm, mã, hóa đơn và trừ tồn. Admin E2E mặc định bỏ qua nếu không đặt E2E_ADMIN_PASSWORD; đặt thêm E2E_ADMIN_USERNAME nếu khác admin. E2E_BASE_URL mặc định localhost:5173; CHROMIUM_PATH nếu dùng browser có sẵn.

Chưa xác nhận runtime trên SQL Server thực tế: image Microsoft bị chặn tải trong môi trường triển khai. Cần nghiệm thu schema/Unicode/locking trên SQL Server của bạn, thiết bị QR camera/USB và bản in máy in thật. Build/test H2 không chứng minh production-ready.

## 6. Dữ liệu và phạm vi chuyển đổi

- Không xóa/đổi database MySQL cũ. Backup cả SQL, ảnh upload và biến môi trường trước khi chuyển dữ liệu thật.
- Model mới khác model cũ. Chưa có script tự di chuyển tài khoản, hash mật khẩu, hóa đơn, voucher, OTP hoặc payment history giữa hai hệ. Không sao chép từng bảng tùy ý làm mất quan hệ/lịch sử. Nếu cần migration dữ liệu thật, phải chốt bảng ánh xạ và kiểm thử trên bản sao trước.
- OTP/email, Google/Facebook, VNPAY và các secret/callback cũ **chưa port**. Chỉ hoạt động ở source cũ; bản mới hiện COD online và tiền mặt POS. Không đổi callback/ngrok sang bản mới để nhận thanh toán khi chưa có backend xử lý chữ ký/IPN.
- Quản trị mới dùng một tài khoản môi trường. Chưa có quản lý nhân viên, phân quyền chi tiết, thu/chi, đổi trả/hoàn tiền, đối soát hoặc toàn bộ module của video.
- Thiết kế tham khảo ảnh Bee đã gửi, không xác nhận đã xem trực tiếp toàn bộ video hoặc giống 100%.

## 7. Trước triển khai công khai

Dùng HTTPS, tài khoản DB giới hạn quyền, chứng chỉ SQL Server hợp lệ (`trustServerCertificate=false`), migration versioned và `DDL_AUTO=validate`, tắt seed. Không dùng `sa`, mật khẩu mẫu, dev server, H2 test hoặc tự động update schema trên dữ liệu kinh doanh. Cần thêm auth khách hàng, RBAC nhân viên, rate limiting/anti-abuse, audit tồn kho và kiểm thử tích hợp thanh toán trước khi bán thật.

Frontend production: `npm run build`, phục vụ `frontend/dist` bằng Nginx; SPA fallback `try_files $uri $uri/ /index.html;`, reverse proxy `/api` sang backend. Không dùng Vite dev/preview làm production server.
