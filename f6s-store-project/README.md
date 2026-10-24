# F6S · Spring Boot + Vue 3 + SQL Server

Bản kiến trúc mới, độc lập với source Spring/Thymeleaf/MySQL ở thư mục gốc. **Không import SQL MySQL/XAMPP cũ vào SQL Server. Không trỏ bản mới vào database của hệ thống cũ.** Database cùng tên `f6s_shop` nhưng nằm trên hai hệ quản trị khác nhau.

## 1. Cấu trúc

```text
f6s-store-project/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/sneaker/
│       │   ├── SneakerApplication.java
│       │   ├── config/        # Security, CORS, seed demo
│       │   ├── controller/    # REST, validation, error mapping
│       │   ├── dto/           # API request/response, không trả Entity trực tiếp
│       │   ├── model/         # Category, Product, Variant, Order, OrderItem
│       │   ├── repository/
│       │   └── service/       # Catalog CRUD, filter, checkout transaction
│       ├── main/resources/application.yml
│       └── test/java/com/example/sneaker/
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── public/images/         # 5 ảnh SVG minh họa gốc
│   ├── tests/
│   └── src/
│       ├── components/        # ProductCard, CartDrawer
│       ├── views/             # Home, Catalog, PDP, Checkout, 404
│       ├── router/
│       ├── stores/            # Pinia cart, persistence, toast
│       └── lib/               # Fetch API, currency, cart validation
├── database/00_create_database.sql
├── compose.yml
└── .env.example
```

## 2. Kết nối SQL Server trước

Cần **JDK 17**, Maven 3.9+ (hoặc Maven tích hợp IntelliJ), **Node.js 20.19+ / 22.12+**, npm, **SQL Server 2019/2022** và SSMS. Có thể dùng SQL Server Express/Developer. XAMPP/phpMyAdmin không chạy T-SQL của bản này.

Trong SSMS, kết nối SQL Server bằng tài khoản có quyền tạo database rồi chạy [database/00_create_database.sql](database/00_create_database.sql). Script chỉ tạo database mới, không DROP database. Nếu `f6s_shop` đã tồn tại, dừng để kiểm tra; không nhập chồng lên dữ liệu cũ.

[backend/src/main/resources/application.yml](backend/src/main/resources/application.yml):

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=f6s_shop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
```

`trustServerCertificate=true` chỉ để phát triển trên máy cá nhân. Production phải dùng certificate hợp lệ, tài khoản database giới hạn quyền, migration có version và `DDL_AUTO=validate`; không sử dụng `sa` hoặc `ddl-auto=update` cho production.

SQL Server Configuration Manager → bật **TCP/IP**, đặt TCP port 1433 và restart dịch vụ. Nếu dùng SQL Express, bật SQL Server authentication/Mixed Mode và dùng đúng tài khoản/mật khẩu SQL. Không nhầm mật khẩu này với mật khẩu Windows.

### Tùy chọn Docker thay cho cài SQL Server trực tiếp

Trong thư mục này, copy `.env.example` thành `.env`, đặt mật khẩu mạnh riêng, rồi `docker compose up -d`. Việc khởi chạy là chấp nhận Microsoft SQL Server EULA; image Developer chỉ dùng development/test. Đợi log báo ready rồi kết nối SSMS `localhost,1433` và chạy script tạo database. Không dùng `docker compose down -v` nếu muốn giữ dữ liệu. File `.env` chỉ được Compose tự đọc; Spring Boot **không tự đọc .env**.

## 3. Chạy backend — IntelliJ hoặc terminal

Mở `backend/pom.xml` như một Maven project, SDK/Run JRE = 17. Run Configuration → Environment variables:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=f6s_shop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true
DB_USERNAME=sa
DB_PASSWORD=mat-khau-SQL-Server-cua-ban
ADMIN_USERNAME=admin
ADMIN_PASSWORD=mat-khau-quan-tri-rieng-it-nhat-12-ky-tu
SEED_DEMO=true
```

Điền giá trị thật trên máy bạn, không commit secret. Nếu IntelliJ tách biến bằng `;`, dùng hộp chỉnh sửa bảng Environment Variables để không cắt chuỗi JDBC có dấu chấm phẩy.

Chạy `SneakerApplication`. Hoặc PowerShell:

```powershell
cd backend
$env:DB_PASSWORD='mat-khau-SQL-Server-cua-ban'
$env:ADMIN_PASSWORD='mat-khau-quan-tri-rieng-it-nhat-12-ky-tu'
$env:SEED_DEMO='true'
mvn spring-boot:run
```

API chạy ở `http://localhost:8081/api/products`. Seed chỉ chạy khi `SEED_DEMO=true` **và chưa có danh mục nào**: 5 danh mục × 5 giày, 250 biến thể, có size hết hàng để thử UI. Seed không tạo đơn hàng/tài khoản khách/OTP/biên lai giả. Lần sau đặt `SEED_DEMO=false`. Dữ liệu nhãn hàng và ảnh là demo, không phải thông tin sản phẩm chính hãng đã kiểm chứng.

## 4. Chạy frontend

Mở `frontend` trong cửa sổ IntelliJ thứ hai hoặc terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:5173`. Vite proxy `/api` đến backend 8081 nên không cần gắn URL backend vào từng component. Nếu đổi cổng API, đặt `API_PROXY_TARGET` trước `npm run dev`. `VITE_API_BASE` là biến công khai lúc build, **không đặt secret vào biến VITE_**.

## 5. Model & luồng nghiệp vụ

- `categories` → `products` → `product_variants`; mỗi biến thể là duy nhất theo sản phẩm + màu + size, SKU ổn định. `Product.stock` và `colorways` được tổng hợp từ các biến thể đang bán, không tạo hai nguồn tồn kho độc lập.
- `orders` → `order_items`: lưu giá, tên, SKU, màu, size tại thời điểm mua; sửa sản phẩm không sửa hóa đơn cũ.
- Home → Catalog → PDP → Pinia cart → Checkout → `POST /api/orders` → kiểm tra biến thể/giá/tồn kho → khóa sản phẩm theo thứ tự ID → trừ tồn và ghi đơn trong cùng transaction → trả mã đơn.
- Backend không nhận giá/tổng tiền từ client. Nếu bất kỳ dòng nào hết hàng, rollback toàn bộ. Tồn kho và chỉnh sửa sản phẩm dùng cùng cơ chế khóa.
- Client tạo UUID `requestKey` và dùng lại khi thử lại cùng dữ liệu trong phiên checkout đang mở. Backend chống trùng yêu cầu bằng unique key và kiểm tra nội dung; lỗi tranh chấp có thể trả 409, thử lại cùng key. Không tự đảm bảo khôi phục request key sau khi đóng/refresh trang; chưa có tài khoản/lịch sử đơn của khách trong bản mới.
- Xóa sản phẩm/danh mục là ngừng bán, không xóa lịch sử. Sửa biến thể giữ SKU/ID; bỏ biến thể khỏi request thì ngừng bán biến thể đó.

## 6. REST API

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/products?category=1&brand=Nike&min=0&max=2000000&size=40&color=Đen&sort=price-asc&page=0&limit=12` | Filter cùng một biến thể còn hàng; page bắt đầu 0 |
| GET | `/api/products/{id}` | Chi tiết + biến thể, tồn kho |
| GET | `/api/categories`, `/api/facets` | Danh mục, brands/colors/sizes cho filter |
| POST | `/api/orders` | Checkout guest COD |
| POST / PUT / DELETE | `/api/admin/products`, `/api/admin/products/{id}` | Thêm/sửa/ngừng bán sản phẩm |
| POST / PUT / DELETE | `/api/admin/categories`, `/api/admin/categories/{id}` | Thêm/sửa/ẩn danh mục |

Sort hợp lệ: `newest`, `price-asc`, `price-desc`, `name`. Admin API dùng HTTP Basic với `ADMIN_USERNAME`/`ADMIN_PASSWORD`, chỉ dùng trên localhost hoặc HTTPS. Không có tài khoản/mật khẩu quản trị mặc định. Không lưu mật khẩu quản trị ở frontend. Thử bằng Postman Authorization → Basic Auth.

Body thêm/sửa sản phẩm:

```json
{
  "name":"F6S Velocity Pro","brand":"F6S","price":1290000,
  "description":"Giày chạy bộ mẫu F6S.",
  "imageUrl":"/images/shoe-0.svg","hoverImageUrl":"/images/shoe-1.svg",
  "categoryId":1,"featured":true,
  "variants":[{"color":"Đen","size":"40","stock":10},{"color":"Đen","size":"41","stock":0}]
}
```

Body đặt hàng (thay ID biến thể bằng dữ liệu GET sản phẩm):

```json
{
  "requestKey":"aa474f35-3a39-482b-a990-a975027fb16f",
  "customerName":"Nguyễn Văn An","email":"an@example.com","phone":"0912345678",
  "address":"123 Nguyễn Trãi, Thanh Xuân, Hà Nội","note":"",
  "items":[{"variantId":1,"quantity":1}]
}
```

## 7. Kiểm thử và phạm vi

```text
cd backend
mvn test
cd ../frontend
npm test
npm run build
```

Backend integration test dùng H2 ở MSSQL compatibility mode: không thay thế kiểm thử SQL Server thực tế. Có test transaction rollback, chống oversell đồng thời, request replay, filter màu/size còn hàng, SKU ổn định và bảo vệ admin API. Frontend unit test cart + production build.

Kiểm tra trình duyệt: với backend và frontend đã chạy trên **database test riêng** có `SEED_DEMO=true`, chạy `npx playwright install chromium`, rồi `npm run test:e2e` trong frontend. Mặc định URL là localhost:5173, có thể đổi `E2E_BASE_URL`. Nếu dùng Chromium có sẵn, đặt `CHROMIUM_PATH`. Bài test desktop/mobile **tạo đơn COD và trừ tồn thật trong database test**, không chạy trên dữ liệu kinh doanh. Luồng kiểm tra: Home → filter Nike → size hết hàng disabled → thêm giỏ → reload giữ giỏ → checkout → giỏ về 0.

Kết quả phiên triển khai: 12 test backend trên H2, 3 test cart, build Vue và 2 test desktop/mobile đã đạt. Chưa xác nhận runtime SQL Server thực tế: image Microsoft bị chặn tải trong môi trường kiểm thử. Cần chạy thêm trên SQL Server của máy bạn trước nghiệm thu.

Đây là lõi storefront/API mới theo stack yêu cầu, **không phải chuyển xong tất cả module Bee/F6S cũ**. OTP/email, Google login, VNPAY, POS, báo cáo, hoàn trả, quản trị UI và migration dữ liệu cũ chưa được port. Source/cấu hình/SQL của bản cũ vẫn giữ nguyên ở ngoài thư mục này. Hiện chỉ COD, miễn phí vận chuyển demo. Trước public cần auth khách hàng, rate limit/anti-abuse checkout, kiểm tra thực tế SQL Server, email xác nhận, inventory audit và triển khai HTTPS.

Giao diện dựa vào ảnh tham chiếu đã có: F6S xanh rừng/kem/lime, không sử dụng logo Bee. Không xác nhận đã xem toàn bộ YouTube hoặc sao chép đúng từng màn hình trong video.

Production frontend: `npm run build`, phục vụ `dist` bằng Nginx với SPA fallback `try_files $uri $uri/ /index.html;`, reverse proxy `/api` sang backend. Vite dev server/preview không phải production server.
