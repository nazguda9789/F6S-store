# SQL Server — schema + catalog model giày có thật

**Dùng SSMS/SQL Server, không dùng phpMyAdmin hoặc `sql/install` (bản MySQL cũ).** Các file này dùng database `f6s_shop`. Không xóa database cũ để cài lại. Sao lưu trước và dừng backend khi import.

## Chọn đúng thứ tự

### Database chưa tồn tại

1. Chạy `00_create_database.sql` trong SSMS để tạo `f6s_shop`.
2. Chạy `01_schema.sql`: tạo 19 bảng ứng dụng, đúng Entity hiện tại.
3. Chạy `02_real_products.sql`: thêm danh mục, sản phẩm và biến thể trực tiếp. Không cần chạy Java để có sản phẩm.

### Database đã tồn tại nhưng chưa có bảng

Bỏ qua 00; chạy **01 → 02** trong hai lần Execute riêng. Chờ mỗi file thành công trước khi chạy file sau, dừng khi có lỗi.

### Backend đã tạo bảng

Bản cũ đã có dữ liệu: sao lưu, dừng backend, chạy **03_upgrade_bee_modules.sql** để thêm nhân viên, khách hàng/địa chỉ, thuộc tính, thư viện ảnh và lịch sử đơn. Không xóa database, không chạy lại 01. Xem [hướng dẫn module Bee](../docs/BEE-MODULES.md).

**Không chạy 01.** Sau khi backup và kiểm tra đang dùng đúng schema của backend Vue/SQL Server, chạy riêng **02_real_products.sql**. File 02 kiểm tra các bảng/cột catalog cần thiết, không sửa schema cũ hoặc chuyển MySQL sang SQL Server.

File 01 từ chối database có bất kỳ bảng người dùng nào. File 02 dùng transaction, kiểm tra trùng mã, không xóa dữ liệu, không tự bật lại danh mục đã bị ngừng hoạt động. Nếu có schema chưa hoàn chỉnh, xử lý riêng thay vì bỏ qua lỗi.

## Bộ dữ liệu chứa gì?

- **25 tên model có thật / 5 danh mục / 250 biến thể demo**.
- Chạy bộ: Nike Air Zoom Pegasus 40, adidas Ultraboost Light, ASICS NOVABLAST 4, HOKA Clifton 9, New Balance Fresh Foam X 1080v13.
- Tập luyện: Nike Metcon 9, Nike Free Metcon 5, Reebok Nano X4, adidas Dropset 2, Under Armour TriBase Reign 6.
- Bóng rổ: Nike LeBron XXI, Nike G.T. Cut 3, adidas Anthony Edwards 1, Under Armour Curry 11, PUMA All Pro NITRO.
- Đi bộ/lifestyle: New Balance 574, New Balance 530, ASICS GEL-1130, PUMA Suede Classic XXI, Reebok Club C 85.
- Sneaker: Nike Air Force 1 '07, adidas Samba OG, adidas Stan Smith, Converse Chuck 70, Vans Old Skool.

**Tên model có thật không có nghĩa mọi dữ liệu đi kèm đã được xác minh từ hãng.** Giá 1.890.000–4.690.000 VND là giá bán mẫu do F6S đặt, không phải báo giá/giá khuyến nghị đã kiểm tra. Màu Đen/Trắng, EU 38–42 và tồn kho cũng là cấu hình demo, không chứng minh hãng có đúng phối màu/size hoặc cửa hàng có hàng. SKU `SKU-RC...` và mã `F6S-RC-...` là mã nội bộ, không phải style code hãng.

Ban đầu mỗi sản phẩm 10 biến thể, 90 đôi demo (Đen/42 hết hàng): tổng 2.250 đôi. Không tạo hóa đơn, doanh thu, mã giảm giá hoặc thanh toán giả. Tài khoản quản trị vẫn qua `ADMIN_USERNAME`/`ADMIN_PASSWORD`, không có mật khẩu trong SQL.

Danh sách đầy đủ và giá mẫu: [catalog-products.csv](catalog-products.csv), mở bằng Excel UTF-8. Website hãng trong CSV chỉ là nguồn tham khảo thương hiệu, chưa phải link xác minh chi tiết từng model.

## Ảnh — chưa hoàn thành

Nguồn hãng và Wikimedia trả HTTP 403 trong môi trường thực hiện, kể cả sau yêu cầu quyền mạng. **Chưa tải hoặc xác minh ảnh đúng model/giấy phép.** `image_url` và `hover_image_url` hiện trỏ tới `/images/catalog-photo-pending.svg`, ghi rõ “Chưa có ảnh sản phẩm”, không gắn ảnh giày khác cùng hãng.

Đây chưa phải catalog đủ ảnh để kinh doanh thực tế. Khi có nguồn truy cập được hoặc ảnh được phép sử dụng, thêm ảnh vào `frontend/public/images/catalog/` và cập nhật từng sản phẩm qua trang quản trị. Không dùng logo/ảnh hãng như bằng chứng hàng chính hãng, tồn kho thực hay quyền phân phối.

## Chạy web sau import

1. Dùng source mới có ảnh placeholder đi kèm.
2. Backend trỏ đúng SQL Server/database, đặt `SEED_DEMO=false` vì SQL đã có dữ liệu; chạy `backend/pom.xml` → `SneakerApplication`.
3. Mở `http://localhost:8081/api/products` để kiểm tra JSON.
4. Chạy Vue trong `frontend`: `npm ci` (lần đầu), `npm run dev`, mở localhost:5173.

Kiểm tra SSMS:

```sql
USE f6s_shop;
SELECT COUNT(*) AS reference_products
FROM dbo.products WHERE code LIKE N'F6S-RC-%';

SELECT COUNT(*) AS reference_variants, SUM(v.stock) AS demo_stock
FROM dbo.product_variants v JOIN dbo.products p ON p.id=v.product_id
WHERE p.code LIKE N'F6S-RC-%';
```

Database mới cho kết quả **25 sản phẩm / 250 biến thể / 2250 tồn demo**. Sau bán hàng hoặc chỉnh sửa, tồn kho không còn giữ số ban đầu là bình thường. Nếu trước đó đã có 25 sản phẩm hư cấu Velocity/Urban..., chúng vẫn được giữ; file 02 chỉ thêm catalog mới, không tự ngừng bán hoặc xóa sản phẩm cũ.

## Nhập lại và bảo toàn dữ liệu

File 02 lưu release `real-models-20261025` vào `catalog_import_history`. Chạy lại sau thành công sẽ bỏ qua, không reset tên, giá, tồn hoặc trạng thái đã sửa. Lần đầu chỉ tạo biến thể cho sản phẩm mới; nếu mã catalog có sẵn nhưng thiếu biến thể, file dừng để kiểm tra, không tự đoán tồn kho. Không xóa marker để ép chạy lại.

Java `SEED_DEMO=true` trên database chưa có danh mục cũng dùng cùng [real-products.json](../backend/src/main/resources/catalog/real-products.json), cùng mã và SKU. Có danh mục thì Java không tự thêm, dùng SQL 02 hoặc trang quản trị. Không cần bật seed Java nếu đã import SQL.

## Tái tạo và kiểm thử cho developer

```text
cd backend
mvn -Dtest=SqlServerSchemaExportTest test
cd ..
node scripts/build-sqlserver-catalog.cjs
```

DDL được xuất bằng Hibernate **SQLServerDialect**, Unicode NVARCHAR, identity, index, unique/check/FK, không phải chuyển chuỗi CREATE TABLE MySQL. Java bootstrap và data smoke test được kiểm tra trên H2 MSSQL mode; smoke test đổi DATETIMEOFFSET sang kiểu timezone của H2 và mô phỏng OUTPUT khi chạy INSERT. Đã kiểm tra đủ 25/250, Unicode/dấu nháy, giữ sản phẩm cũ và không restock/reprice khi lặp INSERT. Các nhánh TRY/CATCH, marker và locking T-SQL chưa được thực thi trên engine Microsoft. Exporter không kết nối database. **Chưa thực thi toàn bộ T-SQL trên SQL Server thực tế**, do môi trường không tải được image Microsoft. Cần chạy thử trên database SQL Server riêng trước khi nghiệm thu hoặc dùng với dữ liệu thật.
