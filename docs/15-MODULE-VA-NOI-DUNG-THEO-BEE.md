# Module và nội dung theo video Bee

Đối chiếu video Bee 14 phút 40,8 giây người dùng đã gửi. Giữ thương hiệu F6S và tông tím than/xanh ngọc đã chọn; không lấy tên khách, nhân viên hoặc dữ liệu cá nhân trong video làm seed.

## Cấu trúc menu chính mới

| Module | Danh mục / màn hình |
|---|---|
| Tổng quan | Báo cáo tổng quan hiện có |
| Quản lý giao dịch | Danh sách giao dịch, Tạo giao dịch |
| Quản lý sản phẩm | Danh sách sản phẩm, Thêm sản phẩm, Danh mục, Thương hiệu, Đế giày, Chất liệu, Màu sắc, Kích cỡ |
| Quản lý tài khoản | Nhân viên, Khách hàng |
| Giảm giá | Đợt giảm giá, Phiếu giảm giá |
| Thư viện hình ảnh | Danh sách ảnh, tìm theo sản phẩm, đến trang quản lý ảnh sản phẩm |

Menu được render trực tiếp bằng fragment `common/workspace-sidebar.html`, dùng chung admin/nhân viên; không còn JavaScript tạo thêm menu và sửa nhãn khi trang đã tải. Nhân viên chỉ thấy nhóm được phép; backend vẫn kiểm tra quyền. Nhóm bài viết/banner/cấu hình cũ được bỏ khỏi menu chính theo cấu trúc video; không xóa database hoặc chặn URL quản trị cũ.

## Nội dung và chức năng đã điều chỉnh

- Đồng bộ tên trang, tiêu đề giao dịch, kích cỡ, đợt/phiếu giảm giá; bỏ nhãn Showing/Sort và metadata tác giả mẫu trên các template liên quan. Giày dùng đơn vị Đôi.
- Danh sách sản phẩm có cột Thương hiệu, lấy tên thật qua truy vấn gom nhóm, không suy đoán thương hiệu theo tên giày. Dữ liệu chưa phân loại hiển thị rõ. Trạng thái Đang bán/Ngừng bán.
- Bảng sản phẩm sắp theo tên giày, số lượng, danh mục, thương hiệu, trạng thái, thao tác như video; ảnh nhỏ và mã sản phẩm nằm cùng ô tên để không dồn hơn mười cột. Ảnh nguồn lỗi dùng placeholder, không giả ảnh của sản phẩm khác. Bộ lọc thương hiệu nằm cùng khối lọc.
- Bỏ widget đơn mới kéo thả che nội dung và nút Làm mới không có xử lý; thay bằng chuông ở header có số đơn realtime và link đến giao dịch. Bộ đếm online cũ không còn hiện trên header; không tuyên bố đã sửa toàn bộ cơ chế đếm backend.
- Màn tài khoản có tiêu đề riêng Nhân viên/Khách hàng, tìm tên/email/điện thoại, lọc trạng thái ở database, lọc riêng từ/đến ngày và giữ điều kiện khi chuyển trang. Trang rỗng không tạo số trang âm.
- Form thêm nhân viên/khách mặc định vai trò theo mục đang mở. Trạng thái hoạt động/làm việc độc lập khóa bảo mật; lưu trạng thái thật, không chỉ đổi nhãn. Lưu xong quay về đúng danh sách vai trò.
- Chặn quản trị viên tự khóa, ngừng hoạt động hoặc bỏ quyền quản trị của tài khoản đang đăng nhập. Không hiển thị nút xóa cứng tài khoản trong bảng mới; ngừng hoạt động trong form để giữ lịch sử.
- Sửa lỗi form sửa tài khoản điền sẵn hash vào ô mật khẩu: ô này luôn trống, bỏ trống thì giữ mật khẩu cũ. Nút xem tài khoản và tạo giao dịch trên chi tiết đã có đích điều hướng thật.
- Link sửa sản phẩm cho nhân viên dùng đúng prefix nhân viên, không đưa sang URL quản trị bị cấm. Giữ breadcrumb, active menu, menu responsive và thông báo realtime.

## Chưa đồng nghĩa thay toàn bộ nghiệp vụ

Đây là đợt đồng bộ cấu trúc module/nội dung và bổ sung chức năng tài khoản, không phải tuyên bố sao chép toàn bộ Bee. Phần payment/refund, state machine giao dịch, voucher tự động, lịch sử kho, upload thư viện độc lập và một số luồng storefront vẫn cần triển khai/đối chiếu tiếp theo [bảng luồng còn thiếu](14-CHUYEN-LUONG-NGHIEP-VU-BEE.md). Mã Java, các bảng dữ liệu cũ và dữ liệu sản phẩm hiện có không bị đổi tên/xóa hàng loạt chỉ để giống video. Không tự đổi danh mục giày bóng đá thành sneaker rồi gán sai sản phẩm.

## Cập nhật máy thành viên

Đợt này không thêm cột hoặc secret. Nếu đã cập nhật SQL theo bản trước, chỉ tải/pull code mới, dùng JDK 17, Reload Maven, khởi động lại và Ctrl+F5. Nếu chưa cập nhật các bản trước, dừng web/sao lưu rồi dùng `sql/nang_cap_f6s.sql` như hướng dẫn. Gói SQL 23/10 vẫn dùng được; không import lại dump đầy đủ trên database cần giữ.

Các URL cũ phục vụ cấu hình vẫn hoạt động cho admin: `/adm-prj/qtht-001/cloudkey/list`, `/banner/list`, `/blog/list`, `/contact/list` (các mục sau cùng prefix). Không cấp thêm quyền này cho nhân viên.

## Kiểm chứng

- 74 test Java chọn lọc, 12 test JavaScript đạt; không phải toàn bộ suite.
- MySQL 8 và web `ddl-auto=validate`: mở 9 trang quản trị, menu desktop/mobile, bộ lọc giao dịch; kịch bản tạo sản phẩm/SKU, thuộc tính/thương hiệu và thư viện tìm ảnh đạt. Test dữ liệu chỉ chạy ở schema riêng, không đưa tài khoản/hóa đơn thử vào SQL tải về.
- Kiểm tra menu có sẵn trong HTML server, không cần JavaScript đổi tên; mở màn nhân viên/khách, bộ lọc trạng thái/vai trò/ngày độc lập, phân trang và form mặc định vai trò.
- Đăng nhập tài khoản nhân viên giả lập: mở bốn trang nhân viên, link sửa sản phẩm đúng khu vực, không thấy mục quản trị tài khoản/thuộc tính và bị chặn URL quản trị. Sửa trạng thái nhân viên thật qua form, thấy trong bộ lọc Đã nghỉ, khôi phục sau test; ô mật khẩu không chứa hash.
- Chưa xác nhận mọi chức năng Bee, chưa kiểm thử MariaDB/XAMPP Windows hoặc toàn bộ thanh toán/hoàn tiền.
