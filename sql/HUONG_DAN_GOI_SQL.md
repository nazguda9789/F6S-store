# Gói SQL F6S — 24/10/2026

[Tải ZIP hiện hành](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SQL-20261024.zip) → Download raw. Các ảnh minh họa mẫu nằm trong code mới, cần tải code cùng phiên bản.

## Máy đang có database: giữ dữ liệu

1. Dừng web, bật MySQL trong XAMPP.
2. phpMyAdmin → chọn đúng database → Export để sao lưu.
3. Import `nang_cap_f6s.sql` trong gói này: đã gộp các migration cũ và metadata thuộc tính mới.
4. Nếu muốn thêm bộ demo, import tiếp `du_lieu_mau_catalog_20261024.sql` trên cùng database. Không bắt buộc cho dữ liệu thật.
5. Chạy code mới bằng JDK 17 và Ctrl+F5. Không cần secret mới.

Nếu chỉ nâng cấp, không import `f6s_shop.sql`. SQL mới thêm code/mô tả/ngày tạo/cập nhật cho thuộc tính, bảng theo dõi bộ demo, khóa ngoại thuộc tính hợp lệ; không tự đoán hoặc phân loại lại giày cũ, không reset tài khoản. Phải chạy migration trước code mới. FK có tham chiếu mồ côi sẽ được bỏ qua và báo số lượng để xem xét; không tự xóa giá trị cũ.

## Cài mới hoàn toàn

Chỉ import `f6s_shop.sql`. ĐÃ GỘP cấu trúc và bộ demo, không phải import seed thêm. CẢNH BÁO: file có DROP DATABASE f6s_shop, xóa database cùng tên.

## Bộ mẫu mới

- 5 thương hiệu, 5 loại đế, 5 chất liệu.
- 5 danh mục: chạy bộ, tập luyện, đi bộ, bóng rổ, lifestyle.
- Size EU 38, 39, 40, 41, 42; 5 màu Đen, Trắng, Xám, Xanh navy, Đỏ.
- 5 sản phẩm có nhãn [DEMO], 25 biến thể (5 size/sản phẩm), 5 liên kết danh mục, 5 ảnh theo màu và 5 ảnh chi tiết. Ảnh SVG minh họa nằm trong code mới, không phải ảnh hàng thật.
- Nhóm đã có tên tương ứng được tái sử dụng, không tạo lại hoặc đổi trạng thái. Không ghi đè giá/tồn/ảnh/slug sản phẩm demo đã sửa khi chạy lại; không khôi phục biến thể đã chủ động xóa. Không chạy seed đồng thời hoặc khi web đang xử lý đơn.
- Bản cài mới giữ 50 sản phẩm nguồn + 5 demo = 55 sản phẩm, 176 biến thể nguồn + 25 = 201 biến thể. Không tạo hóa đơn/thanh toán/tài khoản giả. Vẫn giữ các bảng nguồn cũ để bảo toàn lịch sử; Java sử dụng bảng English được mô tả trong tài liệu catalog.

## Cảnh báo

Đây là dữ liệu minh họa học tập, không phải thông tin sản phẩm được các hãng xác nhận. Thay bằng dữ liệu thật hoặc ngừng bán mẫu trước khi công khai. Admin demo cài mới vẫn admin@gmail.com / 123123, phải đổi trước public; nâng cấp không đổi mật khẩu. Gói không chứa mật khẩu email, Google Client Secret hay VNPay HashSecret.

Không xóa bản ghi catalog_demo_seed để ép nhập lại. Nếu chủ động xóa hẳn sản phẩm demo, liên kết seed được cascade theo sản phẩm; nhập lại seed có thể tạo lại mẫu đã xóa. Hãy ngừng bán nếu muốn giữ mẫu nhưng không kinh doanh.

Đã kiểm tra trên MySQL 8; chưa kiểm chứng XAMPP/MariaDB Windows. MySQL DDL không rollback toàn bộ, hãy giữ bản sao lưu và đọc lỗi nếu import dở dang. Các thay đổi tài chính/hoàn tiền và toàn bộ nghiệp vụ Bee không nằm trong gói catalog này.
