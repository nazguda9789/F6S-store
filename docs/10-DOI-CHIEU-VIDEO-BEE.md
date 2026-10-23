# Đối chiếu video Bee và module F6S — 23/10/2026

Nguồn: video người dùng tải lên, thời lượng 14 phút 40,8 giây, 360p. Đã trích khung hình để xem màn hình; không có source Bee và không suy ra được toàn bộ quy tắc nghiệp vụ chỉ từ ảnh. Tên F6S và tông tím than/xanh ngọc được giữ theo xác nhận trước đó.

| Mốc khoảng | Nội dung nhìn thấy | Cách triển khai / hiện trạng F6S |
|---|---|---|
| 00:25–03:30 | Danh sách sản phẩm, thêm giày, thương hiệu/danh mục, đế, màu/size, biến thể, chọn ảnh | Giữ backend sản phẩm/biến thể hiện có; bổ sung danh mục thương hiệu, đế giày, chất liệu thật và liên kết sản phẩm |
| 04:30–08:00 | Tạo giao dịch, hóa đơn chờ, chọn hàng/khách hàng, danh sách và trạng thái đơn | Tái sử dụng đơn chờ, tab hóa đơn, picker giày và chuyển trạng thái của F6S; không khẳng định tương đương mọi quy tắc Bee |
| 08:30–10:30 | Khách hàng, nhân viên, thông tin và địa chỉ | Giữ quản lý tài khoản/khách hàng và xác thực email của F6S; không lấy dữ liệu cá nhân từ video vào SQL |
| 11:00–12:00 | Danh sách và form voucher | Giữ voucher và giảm giá sản phẩm F6S, đổi tên nhóm thành Giảm giá |
| 12:30–14:40 | Danh sách giày cho khách, chi tiết, giỏ hàng, voucher | Giữ storefront F6S và các tích hợp đã cấu hình; chưa thay toàn bộ storefront theo video trong đợt này |

## Các module mới

- **Thương hiệu / Đế giày / Chất liệu**: tạo, tìm theo tên, đổi tên, bật/tắt; chặn tên trống, quá 100 ký tự, trùng tên trong cùng nhóm, đổi loại hoặc gán thuộc tính sai nhóm. Không xóa cứng để tránh mất tham chiếu.
- **Liên kết sản phẩm**: chọn 3 thuộc tính trong form thêm/sửa giày, lưu xuống database. Dữ liệu cũ để “Chưa phân loại”, không tự đoán từ tên. Thuộc tính đã tắt chỉ được giữ trên sản phẩm cũ, không gán mới.
- **Lọc thương hiệu**: bộ lọc sản phẩm của quản trị/nhân viên có thương hiệu, áp dụng trên truy vấn database trước phân trang, không lọc riêng trang hiện tại.
- **Thư viện hình ảnh**: tìm theo tên giày, xem ảnh chi tiết đang lưu, phân trang 24 ảnh và mở đúng trang quản lý ảnh sản phẩm. Chưa có tái sử dụng/upload/xóa ảnh độc lập như bộ quản lý media hoàn chỉnh.
- **Menu**: Tổng quan – Thống kê, Bán hàng tại quầy, Quản lý giao dịch, Quản lý sản phẩm, Quản lý tài khoản, Giảm giá, Thư viện hình ảnh; giữ Nội dung website/Cấu hình hệ thống để không làm mất các chức năng F6S cũ.

## Cập nhật máy thành viên

1. Pull code, dùng JDK 17, Reload Maven và khởi động lại web.
2. Database hiện có: sao lưu rồi chạy riêng `sql/cap_nhat_module_giay_20261023.sql` trên đúng schema. Script chỉ thêm bảng/cột, có thể chạy lại. Profile dev với Hibernate `ddl-auto=update` cũng tự bổ sung nhưng không thay thế sao lưu.
3. **Không import lại SQL đầy đủ có DROP DATABASE**. Không thay mật khẩu/tài khoản admin hoặc secret trong đợt này.
4. Mở quản trị → Quản lý sản phẩm → thêm thuộc tính → sửa từng giày để phân loại.

## Chưa hoàn tất thay thế toàn bộ Bee

Chưa viết mới hệ thống giao dịch/hoàn tiền, các luật voucher/đổi trả theo Bee, thư viện upload độc lập hoặc dựng lại toàn bộ storefront. Không coi việc đổi menu là đã xây đủ nghiệp vụ. Giữ các module cũ tương đương thay vì xóa backend và dữ liệu hiện tại.

## Model và dữ liệu bổ sung

`ShoeAttribute` → bảng `shoe_attribute`: id, kind (BRAND/SOLE/MATERIAL), name, active. Unique (kind,name); service kiểm tra tên trùng không phân biệt hoa thường. `Product` thêm brandId/soleId/materialId (nullable), service kiểm tra loại và trạng thái trước lưu. Không tạo quan hệ cascade xóa hay tự sửa dữ liệu cũ.

Thư viện dùng lại `ProductImage` và liên kết Product; không nhân bản file, không sao chép ảnh hoặc tài khoản trong video.

## Kiểm chứng

- 42 kiểm thử Java: 8 thuộc tính giày + 34 kiểm thử hồi quy đăng ký/Google/điều hướng; không phải toàn bộ suite Java.
- 12 kiểm thử JavaScript về checkout/địa chỉ/hiện diện trực tuyến.
- Trình duyệt: mở ba nhóm thuộc tính, thư viện ảnh, form sản phẩm và danh sách khách hàng/nhân viên; tạo thuộc tính, chặn trùng, lưu/tải lại liên kết giày, lọc thương hiệu, tìm ảnh, chặn gán thuộc tính đã tắt và chặn ghi khi chưa đăng nhập. Sản phẩm kiểm thử được xóa sau chạy; thuộc tính demo được giữ ở trạng thái tắt trên database thử nghiệm.
- Migration chạy hai lần thành công trên MySQL 8 của môi trường demo. Chưa chạy trên máy XAMPP/MariaDB của thành viên.
- Các trang cũ được sửa lỗi đọc ảnh sản phẩm khi không có form, chặn subscribe realtime khi socket đã đóng và bỏ khởi tạo picker/bảng nâng cao khi thư viện CDN không có. Native table còn xem/thao tác được; không khẳng định có đủ tìm kiếm/phân trang nâng cao khi thiếu DataTables.

Chạy `src/test/js/shoe-modules-browser.cjs` chỉ trên database demo riêng; kịch bản tạo dữ liệu. Cần Playwright/Chromium cho kiểm thử, không phải phụ thuộc bắt buộc để người dùng chạy web.
