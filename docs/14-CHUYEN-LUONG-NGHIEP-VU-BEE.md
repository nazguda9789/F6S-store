# Chuyển luồng nghiệp vụ theo Bee

Đợt tiếp theo về menu/tên mục và xử lý tài khoản: [chi tiết nội dung mới](15-MODULE-VA-NOI-DUNG-THEO-BEE.md). Không dùng việc hoàn tất menu để kết luận các luồng còn thiếu bên dưới đã xong.

Yêu cầu: giữ tên F6S, thay chức năng và cách thao tác theo video Bee, không chỉ đổi màu/menu. Video thể hiện hành vi giao diện, không cung cấp source hoặc đầy đủ quy tắc hoàn tiền, bảo mật và xử lý tranh chấp. Không tuyên bố tương đương 100% khi chưa đối chiếu và kiểm thử từng luồng.

## Đã đổi trong đợt này

- Đơn chờ được khóa khi sửa/chuyển giao dịch. Lưu hóa đơn, trừ tồn và đóng đơn chờ trong một transaction; thất bại rollback. Không còn tùy chọn giữ đơn chờ đã chuyển để chuyển lần nữa.
- Khi chuyển, kiểm tra lại trạng thái kinh doanh của giày/biến thể và tồn dưới khóa. Chặn trạng thái khởi tạo hủy/trả hàng hoặc loại đơn giả mạo.
- Chọn khách hàng lưu `customer_id` trên đơn chờ và hóa đơn, độc lập với nhân viên tạo. Kiểm tra khách ROLE_USER đã xác thực/đang hoạt động. Nút Khách lẻ bỏ liên kết; không suy đoán khách hàng cho hóa đơn cũ từ email hoặc `user_id`.
- Form được lưu trước yêu cầu chuyển giao dịch, tránh bỏ mất khách hàng/phí/ghi chú vừa nhập. Đơn hoàn thành được ghi thời điểm hoàn thành.

Đây mới là phần nền tảng giao dịch, **không phải hoàn tất mọi chức năng Bee**. Cơ chế COD, các enum trạng thái và nhiều service F6S vẫn tồn tại; không đổi tên lớp rồi coi như thay nghiệp vụ.

## Các luồng còn phải thay và nghiệm thu

| Nhóm | Luồng cần đối chiếu và hoàn thiện |
|---|---|
| Bán tại quầy | Nhiều hóa đơn chờ → khách/khách lẻ → quét/tìm SKU → màu/size/số lượng → voucher → tiền khách đưa/tiền thừa hoặc VNPay xác minh → thanh toán → in hóa đơn |
| Giao dịch | Lọc đơn → chi tiết → timeline → chuyển trạng thái hợp lệ → hủy/hoàn tồn đúng một lần → đổi/trả và hoàn tiền có kiểm soát |
| Sản phẩm | Mẫu giày → thuộc tính → ma trận biến thể → ảnh → mã/QR → nhập/điều chỉnh tồn có lịch sử |
| Tài khoản | Hồ sơ nhân viên/khách riêng → nhiều địa chỉ/mặc định → trạng thái → quyền theo nghiệp vụ và nhân viên phụ trách |
| Giảm giá | Đợt giảm sản phẩm và phiếu giảm riêng → điều kiện/thời hạn/lượt dùng → gợi ý phiếu → kiểm tra server khi thanh toán |
| Thống kê/thu chi | Hóa đơn đã thanh toán thực → doanh thu theo thời gian/kênh/phương thức → hoàn tiền → thu/chi → báo cáo và cảnh báo tồn |
| Cửa hàng online | Tìm/lọc giày → biến thể → giỏ → địa chỉ/voucher → COD/VNPay → theo dõi, hủy/đổi trả theo trạng thái |

Không giả lập thanh toán thành công, không dùng nút trống làm chức năng và không xóa dữ liệu cũ để che khác biệt. Tính năng không hiện rõ trong video cần ghi quy tắc triển khai riêng, không gán là quy tắc của Bee.

## Cập nhật SQL

Dừng web và sao lưu. Database hiện có chạy `sql/nang_cap_f6s.sql`; bản đã nâng trước đợt này có thể chạy riêng `sql/cap_nhat_luong_ban_hang_20261023.sql`. Cài mới dùng `f6s_shop.sql`. Hai cột customer_id nullable để giữ dữ liệu cũ, không backfill bằng nhân viên tạo đơn. Không cần secret mới. Không import dump có DROP DATABASE vào dữ liệu đang dùng.

Kiểm thử chuyển giao dịch tạo hóa đơn/trừ tồn chỉ chạy trên database dùng riêng cho test: `F6S_BASE_URL=... F6S_DISPOSABLE_DB=1 NODE_PATH=... node src/test/js/pos-checkout-browser.cjs`. Kịch bản giữ hóa đơn thử để đối chiếu; không chạy trên dữ liệu cửa hàng.

## Kiểm chứng đợt này

- 67 unit test Java chọn lọc đạt (14 workflow đơn chờ), 12 test JavaScript đạt; không phải toàn bộ suite.
- MySQL 8: migration chạy lại hai lần, SQL cài mới có đủ hai cột; app chạy với `ddl-auto=validate`, không dựa vào tự thêm cột.
- Trình duyệt/API: chọn khách → lưu/tải lại giữ customerId; tạo đơn/chọn hàng, chặn số lượng âm và tổng tiền giả; hai yêu cầu chuyển đồng thời chỉ một thành công, chỉ tạo một hóa đơn/chi tiết và trừ một sản phẩm; yêu cầu lặp và thêm hàng vào đơn đã đóng bị chặn.
- Chưa kiểm chứng XAMPP/MariaDB thực tế, thu tiền thật, hoàn tiền, mọi tình huống đồng thời hoặc tất cả module Bee. customerId mới chỉ lưu liên kết; chưa viết lại toàn bộ lịch sử mua hàng/tài khoản theo model khách mới.
