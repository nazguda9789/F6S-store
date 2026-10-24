# Bán tại quầy và chi tiết giao dịch theo Bee — 24/10/2026

## Cập nhật trước khi chạy

**Bản này cần thêm hai cột SQL**, khác đợt thêm nhanh thuộc tính trước đó.

1. Dừng web, sao lưu database đang dùng.
2. Đã nâng catalog 24/10: import `sql/cap_nhat_thanh_toan_tai_quay_20261024.sql` trên đúng database. Chưa nâng catalog: dùng bản `sql/nang_cap_f6s.sql` mới nhất đã gộp các migration.
3. Pull code mới, dùng JDK 17, khởi động lại và Ctrl+F5.

Chỉ thêm `invoice.cash_received` và `invoice.cash_change`, kiểu DECIMAL(19,0), nullable. Hóa đơn cũ giữ NULL, không tự gán là đã thu tiền. Migration chạy lại được, không reset tài khoản, OTP hoặc cấu hình thanh toán. Không cần secret mới; không sửa TMN/Hash Secret/return URL/luồng VNPAY hay mã OTP.

[Gói SQL hiện hành](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SQL-POS-20261024.zip) → Download raw. Không dùng ZIP catalog cũ thay cho migration POS. Bản `f6s_shop.sql` trong gói dành cho cài mới và có DROP DATABASE: **không import vào database cần giữ**.

[Gói demo độc lập](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-DEMO-POS-20261024.zip) có `f6s_shop_demo.sql`, tạo database riêng, không xóa database chính. Không nhập file cài demo lại vào database demo đã tồn tại; dừng khi gặp lỗi CREATE DATABASE. Các mẫu đầy đủ theo [tài liệu 18](18-DU-LIEU-MAU-TAT-CA-MODULE.md), không tạo biên lai hoặc thu tiền mặt giả cho năm hóa đơn mẫu.

## Luồng tại quầy hiện tại

1. Tạo/chọn hóa đơn chờ bằng các tab.
2. Chọn khách hàng hoặc Khách lẻ; chọn sản phẩm từ bảng màu/size. Có thể nhập/quét **SKU chính xác** qua máy quét dạng bàn phím và Enter; quét lại tăng số lượng. Không phải tính năng quét camera QR. Server kiểm tra ngừng bán, tồn kho, mã không tồn tại và đơn đã đóng.
3. Sửa số lượng, phí vận chuyển, giảm giá hoặc chọn phiếu. Tổng tiền hàng ở form chỉ đọc, lấy từ các dòng server.
4. Mở Xác nhận: hệ thống lưu form rồi lấy báo giá server. Phiếu khả dụng được sắp theo số tiền giảm, gợi ý tốt nhất trong danh sách đủ điều kiện.
5. Chọn **Tại quầy + Hoàn thành**, nhập tiền khách đưa nguyên VND; thấy tiền thiếu/tiền thừa. Giá và voucher được kiểm tra lại ở thời điểm chốt, không tin preview hoặc tiền giảm từ trình duyệt.
6. Chốt thành công lưu hóa đơn, tiền khách đưa/tiền thừa, trừ tồn, tăng số lượng đã bán, đóng đơn chờ trong một transaction. Yêu cầu trùng/đồng thời không tạo hai hóa đơn từ cùng một đơn chờ.
7. Sang Chi tiết giao dịch để xem lịch sử và **In hóa đơn**. Bản in bỏ menu/nút thao tác, hiển thị giày, phân loại, giá, tổng, tiền mặt nếu có. Không phải hóa đơn điện tử VAT.

Mặc định form chốt là Tại quầy/Hoàn thành; phí vận chuyển mặc định 0. Muốn tạo đơn giao hàng chọn Giao hàng và trạng thái phù hợp, nhập phí/địa chỉ. Đơn chờ vận chuyển không được ghi nhận đã nhận tiền mặt. Bán tại quầy ghi nhận nhân viên đã thu tiền mặt, **không xác minh giao dịch ngân hàng**. Không thêm lựa chọn VNPAY giả vào POS; thanh toán VNPAY hiện tại của storefront giữ nguyên.

Admin và nhân viên dùng chung template bán hàng/chi tiết, nhưng giữ navbar, URL và quyền riêng. Nhân viên vẫn đi qua `/emp-prj/...`, không bị chuyển sang trang admin sau khi chốt. Các template employee cũ được giữ để tra cứu, không còn là nguồn render cho hai màn này.

## Quy tắc đã bổ sung

- Có mã phiếu: tiền giảm lấy từ database, kiểm tra mã tồn tại, không khóa, mức đơn tối thiểu và cấu hình hợp lệ. Nhập số giảm khác không thay được giá trị của phiếu. Khóa phiếu sau khi báo giá sẽ bị chặn khi chốt.
- Không có mã: vẫn cho nhân viên giảm thủ công, không âm và không vượt tiền hàng; không cộng dồn phiếu với giảm thủ công.
- Cash được lưu DECIMAL nguyên VND; thiếu tiền, tiền lẻ hoặc vượt 19 chữ số bị chặn ở server. Giao diện giới hạn số nguyên an toàn của JavaScript. Giá hàng/tổng hệ thống vẫn dùng model Double cũ, chưa chuyển toàn bộ sang BigDecimal; tổng lẻ VND không chốt tiền mặt được.
- Chuyển trạng thái có khóa dòng hóa đơn. Hoàn thành lại sau yêu cầu đổi hàng không tăng `sold` lần nữa hoặc đặt lại ngày hoàn thành đầu tiên. Timeline lấy lịch sử thật theo ngày/ID, không tự dựng các bước chưa diễn ra.
- API chốt trả `data={orderId,invoiceId,code}` thay cho chỉ orderId để mở hóa đơn vừa tạo. Tích hợp riêng gọi API cũ cần cập nhật payload; Tại quầy/Hoàn thành bắt buộc gửi `cashReceived`.

## Phần chưa thể coi là tương đương Bee hoàn toàn

Không tuyên bố đã hoàn thành quy trình đổi/trả/hoàn tiền, hoàn tồn và lịch sử nhập kho, thu/chi, nhiều địa chỉ khách, in tem/QR qua camera, thanh toán ngân hàng tại quầy hoặc toàn bộ thống kê. Phiếu hiện dùng điều kiện khóa/mức tối thiểu/giảm tiền theo schema cũ, **chưa có hạn dùng, quota, lượt dùng mỗi khách hoặc giảm phần trăm**. Một số báo cáo cũ dựa vào lịch sử trạng thái vẫn cần đối soát trường hợp lặp sau đổi hàng. Không dùng đợt này làm chứng nhận tài chính production.

Video/ảnh chỉ cho thấy hành vi giao diện, không cung cấp đầy đủ quy tắc hoàn tiền và kế toán; các phần đó cần triển khai và nghiệm thu riêng. Giữ kiểm thử OTP/VNPAY, không thay secret của người dùng.

## Kiểm chứng

112 test Java chọn lọc và 12 test JavaScript đạt, gồm xác thực/đăng ký/VNPAY, tiền mặt/voucher/đơn chờ/trạng thái. Đây không phải toàn bộ test suite hay xác nhận dịch vụ ngoài hoạt động thực tế.

Kiểm thử trên MySQL 8.4 và database độc lập, chạy Hibernate `validate`; không import vào database cửa hàng đang dùng. Đã kiểm tra SQL cài mới, migration lặp, báo giá server, phiếu bị khóa, tiền thiếu/lẻ, chốt đồng thời đúng một lần, tổng tiền/tiền thừa/tồn/`sold`, lịch sử, PDF hóa đơn và chốt bằng modal thật. Chưa kiểm tra thanh toán tiền thật, Gmail thật hoặc mọi phiên bản MariaDB/XAMPP.
