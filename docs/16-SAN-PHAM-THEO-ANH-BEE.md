# Sản phẩm theo các ảnh Bee bổ sung

Giữ thương hiệu F6S và màu tím than/xanh ngọc đã chọn. Dùng ảnh để đối chiếu bố cục và thao tác, không sao chép tài khoản hoặc dữ liệu cá nhân trong ảnh.

## Đã làm

- Menu Quản lý sản phẩm có hai mục: Sản phẩm và Thuộc tính sản phẩm; mục thuộc tính chứa danh mục, thương hiệu, đế, chất liệu, màu, kích cỡ. Desktop có menu nổi khi trỏ chuột/focus, mobile dùng menu thu gọn.
- Danh sách giày: tìm tên/mã ở phía trên, nút Thêm sản phẩm bên cạnh, phía dưới là trạng thái/danh mục/thương hiệu. Tự lọc khi thay lựa chọn; tìm kiếm chờ 300ms để tránh gửi từng phím. Bộ lọc giá vẫn có trong phần bổ sung, số dòng/sắp xếp đặt dưới bảng.
- Form thêm/sửa giày tách khối tên, thuộc tính, bảng các sản phẩm cùng loại theo từng màu, ảnh, mô tả/thiết lập bổ sung. Di chuyển chính các input đang dùng, không tạo ô giả hoặc ghi đè dữ liệu khi đổi bố cục.
- Chọn nhiều màu × nhiều kích cỡ để tạo biến thể, điền số lượng/giá mặc định; mỗi hàng vẫn sửa được. Tạo lại không trùng tổ hợp và không xóa số lượng đã sửa. Xóa hàng chưa lưu cập nhật lại nhóm.
- Dòng mẫu ẩn không còn bị gửi như một biến thể thật. Biến thể đã lưu không bị gửi lại thành dòng mới; vẫn sửa bằng nút Lưu của từng hàng. SKU luôn do server cấp.
- Không có file ảnh mới thì không gọi endpoint upload với file undefined. Nếu trình soạn thảo CDN không tải được, vẫn lưu mô tả từ textarea.

## Giới hạn cần ghi rõ

Ảnh tham khảo có cân nặng từng biến thể; model hiện tại vẫn dùng cân nặng chung của sản phẩm (kg) cho vận chuyển. Form ghi rõ là cân nặng chung, chưa bổ sung cột/luồng vận chuyển theo cân nặng riêng. Ảnh theo màu đã lưu vẫn quản lý qua khối Hình ảnh sản phẩm; chưa có toàn bộ cơ chế chọn ảnh từng màu ngay trong hàng biến thể mới như ảnh tham khảo. Tên giày hiện nhập trực tiếp, không giả lập danh mục tên giày độc lập bằng một nút + không lưu dữ liệu.

Không thêm SQL hoặc secret trong đợt này. Chỉ cập nhật code, khởi động lại và Ctrl+F5 nếu database đã nâng đúng bản trước.

## Kiểm chứng

Kịch bản `src/test/js/product-builder-browser.cjs` chạy trên database demo riêng: mở menu nổi/thuộc tính; tạo 2 màu × 2 size; giữ số lượng sửa khi tạo lại; chỉ lấy đúng 4 dòng (không lấy mẫu ẩn), xóa còn 3; lưu qua form thật, tải lại 3 biến thể có SKU server; xóa sản phẩm thử sau chạy. Kiểm tra lại 9 trang quản trị, lọc giao dịch, menu desktop/mobile đạt. Không chứng nhận toàn bộ nghiệp vụ Bee hoặc XAMPP/MariaDB.
