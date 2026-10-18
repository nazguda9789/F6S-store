# 03 — Chức năng, màn hình và phân quyền

[Mục lục](README.md) · [Model](02-KIEN-TRUC-VA-MODEL.md) · [Luồng xử lý](04-LUONG-NGHIEP-VU.md)

## 1. Vai trò và phạm vi

| Vai trò | Khu vực chính | Công việc |
|---|---|---|
| Khách chưa đăng nhập | Website bán hàng | Xem sản phẩm/bài viết, giỏ trong session, đặt đơn vãng lai, tra cứu đơn |
| `ROLE_USER` | Website và tài khoản | Giỏ theo tài khoản, thông tin cá nhân, mật khẩu, đơn của tài khoản |
| `ROLE_EMPLOYEE` | `/emp-prj/qtht-001` | Các màn sản phẩm, đơn, khách, bài viết, liên hệ, chat dành cho nhân viên |
| `ROLE_ADMIN` | `/adm-prj/qtht-001` | Quản trị danh mục, sản phẩm, tài khoản, đơn, khuyến mại, nội dung và thống kê |

Quy tắc hiện tại: API nằm dưới `/adm-prj/qtht-001/api/**` cho cả admin và nhân viên; các trang khác dưới prefix admin yêu cầu admin; prefix employee yêu cầu nhân viên; `/tai-khoan` yêu cầu user. Không mặc định admin được vào mọi trang employee, hoặc user thường được vào trang nội bộ.

Đây là mô tả [WebSecurityConfig](../src/main/java/com/web/config/WebSecurityConfig.java), không phải chứng nhận bảo mật. Một số API nằm ngoài các prefix trên; cần đối chiếu kiểm tra quyền tại method/service. Không dùng việc “ẩn menu” để chứng minh quyền backend đã chặt chẽ. CSRF hiện bị tắt; phải đánh giá lại trước triển khai thật.

## 2. Website khách hàng

| Module | Nội dung | Trang/API chính |
|---|---|---|
| Trang chủ | Sản phẩm, danh mục, banner, nội dung giới thiệu/gợi ý theo dữ liệu | `/`, `/index` |
| Sản phẩm | Danh mục, lọc/tìm kiếm, chi tiết, biến thể size/màu, ảnh và giá | `/category/{slug}`, `/product/{slug}`, `/api/v1/products/*` |
| Giỏ hàng | Thêm biến thể, xem giỏ, đổi số lượng, xóa | `/cart`, `/api/v1/cart/*` |
| Checkout | Chọn sản phẩm thanh toán, người nhận, địa chỉ, voucher, phí ship và phương thức trả tiền | GET/POST `/checkout`, POST `/checkout-vnpay` |
| Xác nhận đơn | Mã đơn, chi tiết và thông tin xác nhận theo token | `/success-order/{code}?token=...` |
| Tra cứu đơn | Tìm đơn theo mã | `/tra-cuu-don-hang?code=...` |
| Tài khoản | Đăng nhập, đăng ký/OTP, sửa thông tin, đổi/quên mật khẩu | `/login`, `/register`, `/verify-otp`, `/tai-khoan`, `/forgot` |
| Nội dung | Danh sách/chi tiết bài viết, giới thiệu, liên hệ | `/blogs`, `/blog/{slug}`, `/gioi-thieu`, `/contact` |
| Chat/gợi ý | Chat realtime, chatbot AI, gợi ý và ghi nhận xem sản phẩm | `/api/chat-realtime/*`, `/api/chat-bot`, `/api/recommendations/*` |

Giỏ vãng lai và giỏ đăng nhập đi qua hai nhánh lưu trữ khác nhau; chưa khẳng định tự hợp nhất giỏ khi đăng nhập. Đăng ký/quên mật khẩu phụ thuộc cấu hình email. Có route tra cứu đơn không có nghĩa đã đáp ứng yêu cầu bảo mật dữ liệu cá nhân; cần kiểm thử quyền xem và thông tin hiển thị.

## 3. Quản trị admin

Các đường dẫn trong cột cuối được nối sau `/adm-prj/qtht-001`.

| Module | Nội dung trong code | Màn hình |
|---|---|---|
| Dashboard | Trang tổng quan | `/dashbroad/view` (giữ đúng cách viết trong source) |
| Sản phẩm | Danh sách, form tạo/sửa, ảnh theo màu, biến thể, SKU, giá và tồn | `/product/list`, `/product/save` |
| Danh mục | Danh sách, tạo/sửa, phân cấp và xóa | `/category/list`, `/category/save` |
| Màu, size | Danh sách, thêm/sửa/xóa thuộc tính | `/color/list`, `/size/list` |
| Đơn hàng | Lọc, phân trang, chi tiết, lịch sử, chuyển trạng thái | `/invoice/list`, `/invoice/detail` |
| Bán tại quầy | Hóa đơn chờ, thêm biến thể, số lượng, khách và chuyển hóa đơn | `/invoice/create`, API `/api/order-draft/*` |
| Tài khoản | Lọc tài khoản, tạo/sửa tài khoản và vai trò | `/user/list`, `/user/create` |
| Khách hàng | Thống kê/tìm khách từ dữ liệu giao dịch | `/customer/list` |
| Voucher | Mã giảm theo số tiền và điều kiện tối thiểu | `/voucher/list`, `/voucher/save` |
| Đợt giảm giá | Mức giảm, sản phẩm, thời gian áp dụng | `/discount-product/list`, `/discount-product/save` |
| Bài viết | Danh sách, soạn/sửa/xóa | `/blog/list`, `/blog/save` |
| Banner | Quản lý quảng cáo/banner | `/banner/list`, `/banner/save` |
| Liên hệ | Danh sách và xử lý dữ liệu liên hệ | `/contact/list` |
| Cloudinary | Quản lý cấu hình upload và cấu hình chính | `/cloudkey/list` |
| Shopee | Có màn quản lý khóa | `/shopee/key`; không khẳng định đã đồng bộ đơn/sản phẩm Shopee |
| Doanh thu | Trang doanh thu, đơn hoàn tất, doanh thu ngày/tháng và sản phẩm bán chạy | `/thong-ke/doanh-thu` |

Chức năng thêm/sửa/xóa có thể dùng AJAX/API thay vì cùng route với form. “Có màn hình” không khẳng định mọi validation, xóa dữ liệu liên quan và lỗi dịch vụ đã được kiểm chứng.

## 4. Khu vực nhân viên

Prefix `/emp-prj/qtht-001`: `/product/list`, `/product/save`, `/invoice/list`, `/invoice/detail`, `/invoice/create`, `/customer/list`, `/blog/list`, `/blog/save`, `/contact/list`, `/chat/list`.

Nhân viên có các controller riêng nhưng dùng lại nhiều service/API admin được cấp cho nhân viên. Cần kiểm thử từng thao tác, không suy ra từ tên vai trò rằng nhân viên không thể sửa/xóa dữ liệu qua API chung. Chức năng quản lý tài khoản, cấu hình dịch vụ, voucher và thống kê không có đầy đủ controller employee tương đương trong cây hiện tại.

## 5. Nhóm API đáng chú ý

| Nhóm | Ví dụ | Chú ý |
|---|---|---|
| Giỏ | POST `/api/v1/cart/add`, POST `/api/v1/cart/update-quantity/{idProductDetail}`, DELETE `/api/v1/cart/delete/{idProductDetail}` | ID biến thể, không phải ID sản phẩm cha |
| Đơn | POST `/adm-prj/qtht-001/api/invoice/filter`, `/history`, `/change-status` | Kiểm tra role, đơn tồn tại và trạng thái hợp lệ |
| Đơn chờ | POST `/adm-prj/qtht-001/api/order-draft/create`, `/add-to-order`, `/change-quantity`, `/save-order-to-invoice` | Chuyển đơn là thao tác ghi dữ liệu/tồn kho |
| Vận chuyển | GET `/api/shipping/public/province`, `/district`, `/wards`, `/tinh-phi` | Dành cho GHN, không phải JSON địa chỉ local |
| Thống kê cũ | GET `/adm-prj/qtht-001/api/statistic/revenue-year` | Theo prefix API được bảo vệ |
| Thống kê V2 | GET `/admin/api/statistic/dashboard`, `/category`, `/daily`, `/growth`, `/order-status` | Prefix khác; cần rà soát quyền trước public |
| Upload | POST `/adm-prj/qtht-001/api/upload/upload-file`, `/upload-multiple-file` | Cần Cloudinary; không gửi khóa thật trong request chia sẻ |

Đây là chỉ mục, không thay thế đặc tả OpenAPI. Xem [controller](../src/main/java/com/web/controller) và [API](../src/main/java/com/web/api) để biết tên/thể loại từng tham số. Một số API trả HTTP 200 kèm `Result.success=false`; phải đọc cả body, không chỉ status code. `AddressApi` hiện là lớp rỗng, chưa có endpoint lấy danh sách.

## 6. Trạng thái chức năng và việc còn thiếu

| Hạng mục | Hiện trạng cần trình bày đúng |
|---|---|
| COD local | Đã kiểm tra tạo đơn qua trình duyệt trên MySQL 8 |
| Địa chỉ tìm kiếm | Đã kiểm tra chọn danh mục, tự điền quận, nhập địa chỉ mới và lỗi tải dữ liệu |
| Admin | Đã kiểm tra đăng nhập/trang đơn và độ tương phản header; không phải tất cả CRUD |
| VNPay | Có tạo URL/ký/return; test bằng khóa giả đạt, chưa xác minh giao dịch merchant thật |
| MoMo | Có giá trị enum `MOMO`, không có nghĩa luồng thanh toán đã hoàn chỉnh; giao diện hiện chặn lựa chọn chưa hỗ trợ |
| Báo cáo | Có số liệu/biểu đồ trong code; chưa xác nhận toàn bộ báo cáo theo kênh online/quầy, phương thức trả tiền, khoảng thời gian tùy chọn và cảnh báo sắp hết |
| Tích hợp | Email, GHN, Cloudinary, OAuth, Telegram, AI/SMS cần cấu hình và kiểm thử với dịch vụ |
| Production | Cần rà soát quyền, CSRF, thanh toán/IPN, đồng thời tồn kho, hoàn hàng/hoàn tiền và bảo vệ dữ liệu cá nhân |
