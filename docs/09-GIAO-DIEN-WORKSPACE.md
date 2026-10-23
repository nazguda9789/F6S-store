# Giao diện quản trị F6S theo ảnh tham khảo

Ghi chú: nội dung dưới đây mô tả đợt giao diện đầu. Đợt tiếp theo đã có tạo đơn nhanh và hộp thoại chọn khách; xem [11 — Giao dịch tương đồng Bee](11-GIAO-DICH-TUONG-DONG-BEE.md) cho hiện trạng mới hơn.

Đợt này điều chỉnh bộ khung chung cho quản trị và nhân viên theo ảnh mẫu bán giày mới: nền xám nhạt, header trắng, sidebar tím than có logo chữ F6S lớn, điểm nhấn xanh ngọc, đầu bảng xanh ngọc, thẻ nội dung, phân trang, ô nhập và trạng thái chọn. Giữ tên F6S, đường dẫn, phân quyền, API và dữ liệu hiện có.

## Những gì thay đổi

- Thêm `admin/css/f6s-workspace.css` sau stylesheet quản trị hiện có; không áp dụng cho cửa hàng khách hàng.
- Thêm `admin/js/workspace.js`: đường dẫn trang, đánh dấu menu hiện tại, lối vào nhanh Bán hàng / Đơn chờ cho đúng vai trò.
- Sắp xếp đầu menu: Thống kê (quản trị), Bán hàng tại quầy, Quản lý hóa đơn, Quản lý sản phẩm, Quản lý tài khoản, Khuyến mại; các module hỗ trợ hiện có giữ lại phía dưới.
- Gom đường dẫn Khách hàng vào Quản lý tài khoản cho quản trị; nhân viên không có quyền quản lý tài khoản vẫn giữ mục khách hàng riêng. Đổi nhãn danh sách sản phẩm thành Sản phẩm giày và đưa lên đầu nhóm. Tự mở nhóm chứa trang hiện tại.
- Không thêm các trường CCCD, ngày sinh, giới tính hoặc module thu chi chỉ để giống ảnh nếu backend chưa có. Nhóm tài khoản giữ nhãn Tất cả tài khoản vì dữ liệu không chỉ có nhân viên.
- Nhóm Nội dung website chứa bài viết, banner và liên hệ (tùy quyền); Cấu hình hệ thống giữ Cloudinary và liên kết Facebook hiện có. Không tạo màn cấu hình email/VNPay giả: các dịch vụ đó vẫn cấu hình bằng biến môi trường.
- Menu giữ nguyên đường dẫn server; cập nhật nhóm mở/đang chọn sau khi di chuyển liên kết. Nút menu đóng/mở trên desktop và điện thoại, phím Escape đóng menu điện thoại.
- Bảng hóa đơn: bộ lọc xuống dòng khi thiếu chiều rộng, trạng thái dạng pill; giữ nguyên ý nghĩa trạng thái và bộ lọc.
- Màn tạo đơn: chia thông tin chung thành hai cột trên màn hình lớn thay vì bốn cột hẹp; giữ form, ID và xử lý đơn chờ hiện có.
- Các trang khách hàng, sản phẩm, chi tiết đơn, báo cáo và biểu mẫu dùng chung ngôn ngữ thiết kế. Màu nguy hiểm/thành công không bị đổi thành màu thương hiệu.

## Giới hạn

Đây là giao diện tương tự dựa trên ảnh và nghiệp vụ F6S, không phải sao chép hoàn chỉnh Bee. Chưa xây mới lịch phân ca, quản lý đánh giá, tự động chọn voucher hoặc thay quy trình thanh toán. Không thêm menu giả cho tính năng chưa tồn tại. Chat hiện có không được thay đổi nghiệp vụ.

## Bán hàng tại quầy theo mẫu Bee

- Tab hóa đơn được sinh từ **đơn chờ đã lưu trong database**, không tạo giả 5 hóa đơn như hình. Bấm tab để tải thông tin và sản phẩm của đơn qua API đang có.
- Sau khi lưu đơn chờ mới, đơn được chọn và xuất hiện trong thanh tab. Tên khách hàng hiển thị bằng text, không ghép HTML.
- Nút Thêm sản phẩm mở hộp thoại tìm giày và chọn biến thể màu / size; giữ bảng sản phẩm và xử lý thêm hàng hiện có. Không giả lập quét QR khi chưa có luồng kiểm tra mã.
- Giỏ hàng được mở rộng toàn bộ chiều ngang; bổ sung trạng thái rỗng và nút về danh sách hóa đơn.
- Khối tóm tắt đọc khách hàng, tiền hàng, giảm giá nhập tay và phí vận chuyển từ form. Không tự tính một tổng thanh toán khác backend hoặc báo thành công khi chưa thanh toán.
- Chọn khách hàng vẫn nhập thông tin trên form hiện có; chưa xây popup liên kết tài khoản. Thu chi, thương hiệu, đế giày và chất liệu chưa được xây backend mới trong đợt giao diện này.

Không cần nhập lại SQL hoặc thay cấu hình Google/email cho thay đổi này. Khởi động lại web để Maven sao chép tài nguyên, sau đó tải lại trình duyệt bằng Ctrl+F5.

## Kiểm tra giao diện

Kịch bản `src/test/js/workspace-browser.cjs` dùng Playwright/Chromium và tài khoản admin trên **database demo riêng**. Đây là công cụ kiểm thử, không phải thành phần bắt buộc để chạy web. Cấu hình `NODE_PATH` tới thư mục có Playwright, `CHROMIUM_PATH` tới Chromium nếu cần, `F6S_BASE_URL` tới web đang chạy. Có thể thay `F6S_TEST_EMAIL` / `F6S_TEST_PASSWORD` bằng thông tin kiểm thử trong biến môi trường; không lưu mật khẩu thật trong file.

Kịch bản kiểm tra mở 9 trang quản trị, vị trí các liên kết sau khi gom nhóm, nhóm tự mở và menu đóng/mở trên desktop, điện thoại. Không thực thi tạo/xóa đơn hoặc chứng minh đầy đủ nghiệp vụ thanh toán. Nhân viên vẫn dựa trên menu server render và phân quyền backend hiện có; không dùng việc ẩn menu để thay thế kiểm soát quyền.

Nếu bật `F6S_MUTATING_SMOKE=1` trên database demo riêng, kịch bản tạo một đơn chờ kiểm thử, tải qua tab, thêm biến thể giày, kiểm tra dòng giỏ hàng và xóa đơn kiểm thử trong `finally`. Không bật trên database thật. Luồng này đã chạy đạt, không thực hiện thu tiền/chuyển đơn thành hóa đơn chính thức.

Giới hạn kiểm tra: môi trường chặn một số CDN; các trang cũ còn báo thiếu DataTables/iro, lỗi JS bộ lọc sản phẩm và kết nối hiện diện trực tuyến. Các lỗi đó không được coi là đã sửa trong đợt này; HTTP 200 và kiểm tra menu không chứng minh mọi thao tác của từng module hoạt động đầy đủ. Riêng form tạo đơn đã có fallback nếu Select2 không tải được để không làm dừng việc tải giỏ hàng.
