# Các module theo demo Bee — F6S

Luồng giao hàng hiện đã mở rộng: [khách yêu cầu hủy, giao thất bại/nhận hoàn, VNPAY quá hạn và lưu ảnh](FULFILLMENT-AND-MEDIA.md), nâng cấp SQL 05 cho bản đã có 03/04.

Giữ thương hiệu F6S và màu xanh đậm; bố cục quản trị chia sáu nhóm như video Bee: tổng quan, giao dịch, sản phẩm, tài khoản, giảm giá, thư viện ảnh. Đây là bản triển khai theo nghiệp vụ quan sát được, không phải mã nguồn Bee và chưa tương đương mọi chi tiết.

## Đã bổ sung

- Sản phẩm: danh sách, tạo ma trận màu × size, giá/tồn/cân nặng từng biến thể, QR; chọn hãng, đế, chất liệu và tối đa 5 ảnh cho mỗi màu. Trang khách đổi ảnh theo màu.
- Thuộc tính: hãng, đế, chất liệu, màu, size; bật/tắt sử dụng. Không đổi tên thuộc tính đã được tham chiếu gây sai lịch sử.
- Tài khoản: quản lý nhân viên, mật khẩu băm BCrypt, ngừng hoạt động; hồ sơ khách hàng và tối đa 10 địa chỉ, một địa chỉ mặc định.
- POS: tối đa 5 hóa đơn tạm theo người bán, quét/chọn sản phẩm, chọn khách hoặc khách lẻ, khuyến mãi theo sản phẩm, thanh toán tiền mặt, bản in hóa đơn. Thông tin hóa đơn là bản chụp tại thời điểm bán.
- Đơn online: chờ xác nhận → xác nhận → đóng gói → đang giao → hoàn tất. Hủy trước khi giao trả tồn một lần. Có kiểm tra trạng thái cũ và lịch sử thao tác; không tạo giả lịch sử cho đơn cũ.
- Thư viện ảnh JPEG/PNG: kiểm tra nội dung, giới hạn 5 MB, loại bỏ metadata khi mã hóa lại, chống trùng; không ẩn ảnh đang gắn sản phẩm.

## Phân quyền

Admin môi trường: tất cả module. Nhân viên tạo trong hệ thống: POS và hồ sơ khách hàng; không sửa sản phẩm/giá, quản lý nhân viên, báo cáo hoặc khuyến mãi. Ngừng tài khoản có hiệu lực từ yêu cầu tiếp theo. Phiên Basic nằm trong bộ nhớ trình duyệt; triển khai thật bắt buộc HTTPS.

## SQL Server — không dùng phpMyAdmin/XAMPP

Sao lưu `f6s_shop`, dừng backend trước khi thay schema.

- Database trống: chạy `database/01_schema.sql`, sau đó `database/02_real_products.sql` trong SSMS.
- Database Vue/SQL Server đã có dữ liệu: chỉ chạy `database/03_upgrade_bee_modules.sql`. Không chạy lại 01, không xóa database. Nếu cần thêm catalog mẫu, đọc cảnh báo đầu 02 trước khi chạy.
- Khởi động backend sau nâng cấp; danh mục thuộc tính mặc định chỉ được tạo khi bảng thuộc tính hoàn toàn trống. Không tự tạo nhân viên/mật khẩu hoặc khách giả.

Cấu trúc mới nhất có 24 bảng ứng dụng; nhập catalog có thêm bảng kỹ thuật `catalog_import_history`. Bản đã nâng Bee bằng 03 chạy thêm 04 cho tích hợp mới; xem [hướng dẫn demo](DEMO-CHECKOUT-PAYMENTS.md). Script nâng cấp không xóa sản phẩm, tồn kho, đơn hay lịch sử. DDL được xuất bằng SQLServerDialect; kiểm thử tự động dùng H2 MSSQL mode, chưa thay thế nghiệm thu trên SQL Server thật.

## Ảnh và giới hạn

Đặt `MEDIA_DIR` thành thư mục bền vững có quyền ghi; sao lưu cả thư mục này và database. Mặc định `backend/uploads/media` khi chạy từ backend. Không commit ảnh tải lên hoặc secret. Giới hạn upload của reverse proxy cũng cần phù hợp 5 MB.

25 tên model thật/250 biến thể trong catalog là dữ liệu demo: giá, phối màu, size và tồn không phải dữ liệu hãng. Ảnh hiện còn placeholder vì chưa có nguồn tải được kiểm chứng; hãy tải ảnh đúng model/màu và có quyền sử dụng qua thư viện ảnh. Không dùng ảnh minh họa sai sản phẩm để quảng cáo.

Đã thêm tài khoản khách qua email OTP, hồ sơ/đơn của tôi, VNPAY kiểm tra IPN, giao hàng thủ công và hoàn toàn bộ tiền có chứng từ. Chưa tự gửi email mời nhân viên, tra địa chỉ hành chính, đồng bộ hãng vận chuyển hoặc tự gọi API chuyển tiền hoàn. Không thay đổi phần OTP/VNPAY của mã cũ. Không coi bản này là sẵn sàng vận hành thương mại trước kiểm thử SQL Server, bảo mật và thanh toán thực tế.
