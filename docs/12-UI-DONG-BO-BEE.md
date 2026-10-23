# UI F6S theo bố cục tương đồng Bee

Tham khảo các ảnh/video người dùng đã gửi. Giữ tên F6S và palette tím than `#211e38`, xanh ngọc `#107c70` theo lựa chọn của người dùng; không lấy logo/tên Bee hoặc dữ liệu cá nhân từ video. Đây là thiết kế tương đồng, không cam kết khớp pixel từng màn hình.

## Phạm vi thay đổi

| Nhóm màn hình | UI áp dụng |
|---|---|
| Header cửa hàng | Hai tầng: logo/tìm kiếm/tài khoản/giỏ, sau đó menu danh mục; dải thông tin thương hiệu; active menu theo URL; tìm kiếm mở kết quả thật |
| Trang chủ | Thay hero quảng bá lớn bằng tiêu đề gọn và chip danh mục có đường dẫn thật; giữ banner và dữ liệu sản phẩm hiện có |
| Danh mục / sản phẩm | Card ít bo góc, ảnh trên nền sáng, tên/giá dễ đọc; bộ lọc, breadcrumb, bảng và ô nhập thống nhất |
| Giỏ / checkout | Các khối trắng, đường viền nhẹ, thông tin đơn và giao hàng tách rõ; giữ trạng thái disabled, cảnh báo, phí, voucher và backend cũ |
| Đăng nhập / đăng ký / quên mật khẩu / OTP | Desktop chia thương hiệu và form; điện thoại xếp dọc; đồng bộ nút, trường nhập, focus. Không thay cấu hình SMTP/Google |
| Tài khoản / tra đơn / nội dung | Theme chung cho sidebar, bảng, form, tiêu đề; footer tím than. Không thay dữ liệu hoặc nội dung chính sách |
| Quản trị / nhân viên | Tiếp nối sidebar tối và workspace ở các đợt trước; đồng bộ thêm form cũ nằm ngoài main, bảng giao dịch và tiêu đề card |

Theme storefront dùng `css/f6s-bee-ui.css`, `js/bee-ui.js`, nạp qua `common/user/libuser`. Không đổi tên ID form, endpoint, phân quyền hay quy tắc thanh toán. Hộp tìm kiếm dự phòng hỗ trợ cả những trang auth không có modal footer.

Header đo chiều cao thực bằng ResizeObserver, cập nhật khoảng đệm nội dung khi menu điện thoại mở/đóng. Có skip-link, focus bàn phím và giảm chuyển động theo cài đặt hệ thống. Không dùng overflow hidden để che lỗi tràn ngang.

## Kiểm tra

- Playwright `src/test/js/bee-ui-browser.cjs`: 14 trang desktop gồm trang chủ/danh mục/chi tiết giày/giỏ/login/register/forgot/OTP/blogs/chi tiết bài viết/liên hệ/giới thiệu/tra đơn/checkout. Kiểm tra tìm kiếm từ header trên trang chủ và login, mở menu mobile và 7 trang mobile không tràn ngang ở 390px.
- Checkout thử bằng giỏ guest trong session, không gửi đơn, gửi email hoặc thanh toán.
- Kịch bản dùng variant demo 239, có thể đổi bằng `F6S_TEST_VARIANT`. Không dùng database sản xuất cho bộ kiểm thử; API giỏ guest của F6S không tạo đơn.
- Kiểm tra lại 9 trang quản trị và luồng đơn chờ từ đợt trước; không coi render trang thành công là đã kiểm thử mọi popup/hành động của mọi module.
- Màn tài khoản có dữ liệu, trang kết quả thanh toán và các trạng thái lỗi hiếm chưa được kiểm thử hết từng biến thể. Theme dùng chung vẫn áp dụng khi trang có `libuser`.

## Cập nhật

`git pull origin main`, Reload Maven nếu cần, Stop/Run web rồi Ctrl+F5. Không có SQL hoặc secret mới. Không nhập lại database chỉ để thay UI.
