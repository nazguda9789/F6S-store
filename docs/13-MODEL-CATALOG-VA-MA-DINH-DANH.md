# Chuẩn hóa model bán giày và mã định danh F6S

## Đã triển khai trong đợt này

- `Product.code`: mã mẫu giày, unique, dạng `F6S-SP000031`.
- `ProductDetail.sku`: mã biến thể bán được, unique, dạng `F6S-SKU000239`.
- `ProductDetail.legacySku`: giữ mã cũ để tra cứu, không dùng làm khóa duy nhất. Hóa đơn/đơn chờ đã lưu giữ snapshot mã cũ.
- Mã sinh trên server từ ID database, không từ tên giày/màu/size, không nhận mã tùy ý từ trình duyệt; số vượt 6 chữ số được giữ đầy đủ.
- Form sản phẩm hiện mã readonly, bỏ bộ ghép SKU bằng tên trong JavaScript. Danh sách hiển thị mã thay vì chỉ `#id`. Chi tiết khách hàng tách mã mẫu giày và SKU biến thể.
- Tạo mới chặn trùng tổ hợp màu/size, âm tồn kho, giá biến thể không hợp lệ. Cập nhật biến thể giữ sản phẩm cha, không tin `productId` hoặc SKU gửi lên.
- Sửa biến thể không đổi SKU; đổi sang tổ hợp đã tồn tại bị chặn. Các dòng trùng sẵn trong dữ liệu cũ được giữ để không tự làm mất tồn kho/lịch sử.
- Tìm theo mã mẫu, SKU mới và mã cũ trong quản trị; tìm kiếm cửa hàng hỗ trợ mã mẫu/SKU của biến thể đang bán.
- Xóa sản phẩm đủ điều kiện cũng dọn ảnh theo màu trong cùng transaction; sản phẩm có chi tiết hóa đơn vẫn bị chặn xóa như trước.

## Đối chiếu domain: chưa phải viết lại toàn bộ model

| Nhóm | Hiện trạng | Thiết kế cần làm tiếp trước khi thay hoàn toàn |
|---|---|---|
| Catalog | Product, ProductDetail, Category, ShoeAttribute, Color, Size, ảnh | Giữ phân biệt mẫu/biến thể; hoàn tất ràng buộc FK và xử lý dữ liệu trùng trước khi unique tổ hợp |
| Tồn kho | stock trên biến thể, kiểm tra/trừ khi chuyển đơn | Sổ giao dịch kho (nhập/xuất/điều chỉnh/hoàn), chống chuyển đơn lặp, kiểm kê |
| Tiền và thanh toán | Giá/tiền hiện vẫn dùng Double và model thanh toán F6S cũ | Thiết kế chuyển đồng bộ sang BigDecimal/DECIMAL, tách payment/refund; không sửa một cột SQL đơn lẻ rồi để Java lệch kiểu |
| Đơn hàng | Invoice, InvoiceDetail và lịch sử trạng thái cũ | State machine, snapshot người nhận, liên kết khách, hoàn tiền/đổi trả và chống thực thi lặp |
| Tài khoản | User + role, xác thực OTP/OAuth | Tách hồ sơ nhân viên/khách, địa chỉ nhận và audit quyền nếu quyết định viết lại domain |
| Khuyến mại | Voucher và DiscountProduct cũ | Điều kiện áp dụng, lượt dùng, phân bổ giảm giá và hoàn giảm giá cần bộ quy tắc rõ ràng |

Không tuyên bố toàn bộ model đã thay, không xóa các bảng tiếng Việt nguồn hoặc thay nghiệp vụ đang chạy chỉ để đổi tên lớp. Video không phải đặc tả đầy đủ cho tiền/tồn kho/hoàn trả.

## Điểm dữ liệu cần quyết định

Nguồn mẫu có hai biến thể **244 và 245**, cùng sản phẩm 31, màu 9, size 12. SKU mới của hai dòng khác nhau nhưng tổ hợp vẫn trùng. Migration chỉ báo cáo, không cộng tồn, xóa, đổi khóa tham chiếu hoặc tự chọn dòng giữ lại. Muốn ràng buộc unique tổ hợp phải quyết định giữ riêng hay gộp tồn và ngừng bán dòng dư, sau khi kiểm tra đơn/giỏ liên quan.

## SQL và cập nhật

- **Dừng web, sao lưu**, chạy `sql/nang_cap_f6s.sql` (đã gộp) trên schema đang dùng rồi chạy code mới. Nếu đã ở bản thuộc tính giày 23/10, có thể chỉ chạy `sql/cap_nhat_ma_san_pham_20261023.sql`.
- Cài mới dùng duy nhất `f6s_shop.sql`, đã chứa cấu trúc và backfill mã.
- Seed bổ sung ghi mã nguồn vào `legacy_sku`, để server/script cấp SKU chính thức; tránh mất dòng khi database đã có unique SKU.
- Script không đổi ID, không đổi tồn hoặc mã snapshot trong hóa đơn, không reset mật khẩu. Có thể chạy lại; không chạy dump đầy đủ lên database có dữ liệu cần giữ.
- Không cần secret mới. Nếu khởi động code mới trước migration, Hibernate có thể gặp SKU trùng khi tạo unique index: dừng web và chạy migration trước, không xóa dữ liệu để né lỗi.

## Kiểm chứng

- MySQL 8: cài mới + bổ sung hai lần + nâng cấp hai lần; giữ 50 sản phẩm, 176 biến thể; có 50 mã mẫu và 176 SKU riêng, giữ 176 mã cũ. Vẫn báo đúng tổ hợp trùng thay vì âm thầm xóa.
- Kiểm thử mã cho padding, namespace, ID lớn, ID không hợp lệ, chống trùng tổ hợp và giả mạo mã/sản phẩm cha.
- Trình duyệt/API kiểm tra tạo hai biến thể, đổi tên không đổi mã, sửa tồn không đổi SKU, tìm mã, readonly trong form và chặn tổ hợp trùng. Chỉ chạy script `src/test/js/catalog-codes-browser.cjs` trên database demo riêng.
- Chưa kiểm chứng MariaDB/XAMPP hoặc chuyển đổi toàn bộ domain; không chứng nhận đầy đủ bảo mật/hiệu năng production.

Lượt kiểm tra cuối: 61 unit test Java chọn lọc, 12 test JavaScript; kiểm tra trình duyệt catalog, 9 trang quản trị, luồng POS và 14 trang storefront/7 trang mobile. App chạy trên SQL cài mới với `ddl-auto=validate` và kịch bản catalog đạt, không dựa vào Hibernate tự bổ sung cột. Đây không phải toàn bộ test Java hoặc mọi trạng thái nghiệp vụ.
