# Dữ liệu minh họa các module — 24/10/2026

Đây là bộ dữ liệu nghiệp vụ giả để trình diễn/kiểm thử, **không phải viết lại mọi chức năng của website**. Không gửi email, tạo thanh toán VNPAY, gọi vận chuyển hoặc chứng thực giao dịch bên ngoài khi import.

## Cài mới để trình diễn (khuyến nghị)

1. Tải `downloads/F6S-DEMO-ALL-MODULES-20261024.zip`, chọn Download raw trên GitHub, giải nén.
2. Dừng web; import **f6s_shop_demo.sql** bằng MySQL 8. File tạo database riêng `f6s_shop_demo`, không xóa `f6s_shop`. Nếu database demo đã tồn tại, dừng import, không bỏ qua lỗi để chạy tiếp.
3. Chạy code mới, đặt `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/f6s_shop_demo` (đổi cổng nếu cần). Các biến username/password MySQL giữ theo máy bạn.
4. Admin mẫu hiện có: `admin@gmail.com` / `123123`, chỉ dùng local. Đổi mật khẩu trước khi công khai; không mở database demo ra Internet.
5. Không cần thêm secret để xem dữ liệu. Email, Google, VNPAY và vận chuyển vẫn cần cấu hình thật nếu muốn thử tích hợp; SQL không thay thế các secret đó.

Không dùng `f6s_shop.sql` cũ để cập nhật database cần giữ vì file cũ có DROP DATABASE. Bộ mới **không thay thế** gói nâng cấp catalog dành cho dữ liệu thật.

## Database demo đã có schema mới

Sao lưu, dừng web. Tên database bắt buộc kết thúc `_demo`. Import `sql/nang_cap_f6s.sql`, `sql/du_lieu_mau_catalog_20261024.sql`, rồi `sql/du_lieu_mau_toan_bo_20261024.sql`. Không trộn hóa đơn giả với database kinh doanh.

Seed nghiệp vụ chạy một lần theo `demo_dataset_install`; import lại không chèn trùng, không khôi phục dòng đã sửa/xóa. Không xóa marker để ép chạy lại. Nếu thiếu năm sản phẩm/biến thể hợp lệ hoặc trùng định danh mẫu, transaction bị hủy. Cần quyền CREATE ROUTINE để import file seed.

## Danh sách dữ liệu mới

| Module / bảng đang dùng | Số bản ghi mẫu |
| --- | ---: |
| Nhân viên — `users`, ROLE_EMPLOYEE | 5 |
| Khách hàng — `users`, ROLE_USER | 5 |
| Hãng, loại đế, chất liệu — `shoe_attribute` | 5 mỗi loại (15) |
| Giày — `product` | 5 |
| Biến thể — `product_detail` | 25 (5 size mỗi giày) |
| Danh mục / kích cỡ / màu | 5 mỗi nhóm, tái sử dụng tên đã có |
| Liên kết danh mục, ảnh theo màu, ảnh sản phẩm | 5 mỗi bảng |
| Phiếu giảm — `voucher` | 5 |
| Giảm giá sản phẩm — `discount_product` | 5 |
| Giỏ hàng — `carts` | 5 |
| Đơn chờ / chi tiết đơn chờ | 5 mỗi bảng |
| Hóa đơn / chi tiết hóa đơn / lịch sử trạng thái | 5 mỗi bảng |
| Banner / bài viết / liên hệ / tin nhắn hỗ trợ | 5 mỗi bảng |
| Lịch sử xem sản phẩm | 5 |

Các số trên là **bộ mẫu bổ sung**, không phải tổng số dòng cuối cùng: dữ liệu cũ được giữ. Nhân viên/khách mẫu có tên `[DEMO]`, email `f6s-demo-nv-1@example.invalid` … `5`, `f6s-demo-kh-1@example.invalid` … `5`, điện thoại minh họa không hợp lệ, tài khoản **chưa kích hoạt, không có mật khẩu**. Muốn thử đăng nhập hãy dùng admin đặt mật khẩu và kích hoạt từng tài khoản; không tự gán chung mật khẩu cho tất cả người dùng.

5 hóa đơn: tại quầy hoàn thành; online chờ vận chuyển; online đã vận chuyển; online hoàn thành; online đã hủy. Mỗi đơn có 1 dòng giày; tổng tiền khớp giá × số lượng, phí ship/giảm giá 0. Đây là snapshot lịch sử giả, không trừ tồn khi import. Đơn chờ/giỏ chưa giữ tồn. Báo cáo doanh thu sẽ bao gồm đơn giả trong database demo, tuyệt đối không coi là doanh thu thật. Không dùng bộ này để kiểm thử hoàn trả tồn của hóa đơn lịch sử.

## Những bảng không chèn 5 dòng giả

- `history_pay`: dùng xác thực/chống xử lý lặp callback, không tạo biên lai VNPAY giả.
- `otp_codes`, `pending_registration`: chỉ phát sinh qua luồng đăng ký thật; không tạo OTP hoặc bypass xác minh.
- `cloud_key`, `key_telegram`, `shopee_key`: cấu hình/secret, không thêm khóa giả khiến web nhận nhầm là đã cấu hình.
- `province`, `wards`: dữ liệu địa chỉ phải đầy đủ, không rút còn 5 địa phương.
- `catalog_variant_merge`, các bảng registry: kỹ thuật/kiểm toán, không tạo lịch sử gộp kho giả.
- Các bảng tiếng Việt legacy (`tai_khoan`, `nhan_vien`, `khach_hang`, `san_pham`…): không dùng trong JPA hiện tại; thêm vào đó sẽ không hiện trên web. Không nhân đôi nguồn dữ liệu. `parent_category` cũ cũng không thay cho quan hệ cha/con ở `category`.

Không tạo thêm 5 admin quyền cao chỉ để đủ số dòng. Các bảng liên kết/biến thể có số dòng theo quan hệ thực tế, không ép đúng 5.

## Kiểm chứng

Đã thử trên MySQL 8.4, database độc lập và ứng dụng chạy `ddl-auto=validate`: 14 nhóm dữ liệu đúng số lượng; tài khoản mẫu không được kích hoạt; tổng đơn/chi tiết khớp; tồn kho giữ nguyên; import lặp không nhân bản; từ chối database không phải `_demo`. Trình duyệt kiểm tra 5 nhân viên/5 khách hiển thị, 8 trang quản trị, 5 chi tiết hóa đơn và 5 đường dẫn banner/sản phẩm. Chưa xác nhận mọi phiên bản XAMPP/MariaDB hoặc mọi thao tác chỉnh sửa sau import.

## Tạo lại file cài đặt

`node scripts/build-demo-sql.cjs /tmp/f6s_shop_demo.sql`

Script chỉ tạo file, không kết nối hoặc thay đổi database. SQL đầy đủ và seed tùy chọn đều được giữ riêng để phân biệt cài demo với nâng cấp dữ liệu thật.
