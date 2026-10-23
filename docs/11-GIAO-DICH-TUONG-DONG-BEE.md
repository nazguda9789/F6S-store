# Giao dịch F6S theo luồng tương đồng Bee — 23/10/2026

Tiếp nối [đối chiếu video](10-DOI-CHIEU-VIDEO-BEE.md). Mục tiêu là thao tác tương đồng trên nghiệp vụ F6S; không khẳng định đã sao chép đầy đủ mọi quy tắc Bee.

## Thay đổi trong đợt này

### Bán hàng tại quầy

1. Bấm **Tạo hóa đơn chờ**: server tạo đơn Khách lẻ, ghi database, hiện tab và chọn ngay. Không tạo tab giả hoặc chỉ lưu trong trình duyệt.
2. **Chọn khách hàng**: tìm tên/email/SĐT, tối đa 20 khách đã xác thực đang hoạt động. Không đưa admin/nhân viên vào picker. API chỉ trả id, tên, email, điện thoại, địa chỉ; không trả entity User hoặc thông tin xác thực.
3. Chọn khách điền thông tin liên hệ vào form; kiểm tra lại tỉnh/phường rồi **Cập nhật đơn chờ** để lưu. Có thể để khách lẻ. Picker hiện **chưa gắn customerId** hoặc chuyển quyền sở hữu đơn cho tài khoản khách; hóa đơn vẫn dùng cơ chế phân công/userId cũ của F6S.
4. **Thêm sản phẩm**: chọn giày theo màu/size trong hộp thoại. Chặn biến thể ngừng bán/hết tồn và sản phẩm cha ngừng bán; lấy giá từ server, fallback giá sản phẩm nếu biến thể chưa có giá riêng.
5. Đổi số lượng: server chặn <=0 hoặc vượt tồn; hóa đơn chờ **không giữ chỗ tồn kho**, kiểm tra/trừ tồn khi chuyển đơn theo luồng cũ.
6. Server tính lại tiền hàng từ dòng giỏ khi lưu, không nhận totalAmount do trình duyệt tự khai. Giữ người tạo đơn cũ khi cập nhật, không nhận createdBy từ request. Chặn phí/giảm giá âm hoặc không hữu hạn.
7. Trước chuyển đơn: kiểm tra lại số lượng, giá, giảm giá không vượt tiền hàng; lấy tiền hàng từ dòng giỏ, giữ phí vận chuyển đã lưu thay vì luôn đặt 0. Không thay đổi cách chọn hình thức thanh toán hiện có và không báo đã thu tiền khi chưa thu.

### Danh sách giao dịch

- Bộ lọc loại: Tại quầy / Online / Facebook / Tất cả.
- Từ khóa kết hợp với trạng thái và ngày, không bỏ qua điều kiện khác khi có search.
- Cho lọc riêng từ ngày hoặc đến ngày; đến ngày bao gồm cả ngày đó.
- Nút Làm mới xóa từ khóa, ngày, loại và trở về tất cả trạng thái.
- Sắp xếp tiền hàng dùng đúng field `totalAmount`, không dùng field `price` không tồn tại.
- Áp dụng cho cả trang admin và nhân viên.

### Khả năng chạy local

CSS SweetAlert dùng bản có sẵn trong project thay vì CDN, giữ thông báo phía trên sidebar. Trạng thái giỏ trống ẩn khi thêm dòng và hiện khi xóa dòng cuối.

## Cập nhật

Pull code, Reload Maven, Stop/Run web và Ctrl+F5. **Không thêm bảng/cột, không cần SQL hoặc secret mới trong đợt này.** Nếu chưa nâng module 23/10 trước đó, dùng [hướng dẫn SQL](../sql/README.md).

## Kiểm chứng và giới hạn

- 54 unit test Java chọn lọc: 9 workflow đơn chờ, 3 bộ lọc, 8 thuộc tính giày, 34 đăng ký/Google/điều hướng. Không phải toàn bộ suite Java.
- 12 test JavaScript hiện có về checkout, địa chỉ và hiện diện; kiểm tra trình duyệt 9 trang quản trị, lọc loại giao dịch kết hợp từ khóa, làm mới và đóng/mở menu đều đạt.
- Kịch bản Playwright `src/test/js/pos-workflow-browser.cjs` kiểm tra luồng UI tạo đơn, chọn khách, lưu/tải lại, chọn biến thể; kiểm tra chặn số lượng âm, tổng tiền giả và chặn tìm khách khi chưa đăng nhập. Chỉ chạy trên database demo có khách đã xác thực; đơn kiểm thử được xóa cuối lượt.
- Chưa thực hiện thu tiền thật, chưa thêm hoàn tiền/đổi trả theo Bee, chưa giải quyết chuyển cùng một đơn chờ nhiều lần, quyền sở hữu theo từng nhân viên hoặc voucher tự động. Những quy tắc đó cần triển khai riêng, không coi giao diện mới là đã hoàn thiện toàn bộ giao dịch.
