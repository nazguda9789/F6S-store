# Luồng nghiệp vụ hiện hành

## Tài khoản

Email/mật khẩu → yêu cầu OTP → kiểm tra mã còn hạn/đúng mục đích → tạo/kích hoạt theo luồng đăng ký. SMTP cần được cấu hình; không seed OTP giả. Google OAuth dùng client/callback cấu hình riêng. Người dùng do admin cấp không phải một lần đăng ký OTP công khai.

## Catalog

Danh mục/hãng/đế/chất liệu/màu/size → mẫu giày → tổ hợp màu × size → giá/tồn/ảnh → lưu → server cấp mã/SKU. Admin có thể thêm nhanh sáu nhóm thuộc tính trong form giày; tạo thuộc tính là lưu ngay, hủy sản phẩm sau đó không xóa thuộc tính đã tạo.

Giày ngừng bán không xuất hiện ở trang chi tiết công khai; admin có thể xem qua bộ lọc Ngừng bán để giữ lịch sử. Bài viết lưu trữ không lên trang chủ/danh sách/URL bài công khai.

## Online

Tìm/lọc → chọn biến thể/số lượng → giỏ → thông tin nhận hàng/voucher → COD hoặc cổng VNPAY hiện có → theo dõi đơn. Không coi URL chuyển về hoặc ảnh chụp màn hình là xác nhận tiền nếu chữ ký/kết quả giao dịch chưa hợp lệ. Địa chỉ/phí vận chuyển và dịch vụ ngoài cần cấu hình tương ứng.

## Tại quầy

1. Tạo/chọn tab hóa đơn chờ; chọn khách hoặc khách lẻ.
2. Thêm giày bằng picker hoặc SKU chính xác; quét lại tăng số lượng, server chặn ngừng bán/vượt tồn.
3. Lưu thông tin và báo giá server; mã phiếu lấy mức giảm thật trong DB, không tin số giảm của client.
4. Tại quầy/Hoàn thành: nhập tiền khách đưa, server kiểm tra nguyên VND và đủ tiền, tính tiền thừa.
5. Chốt trong transaction: kiểm tra hàng/tồn, lưu hóa đơn/tiền mặt, trừ tồn, đóng draft; chốt trùng chỉ một lần thành công.
6. Mở chi tiết/lịch sử và in hóa đơn. Admin/nhân viên dùng đúng URL riêng, không chuyển nhân viên sang trang admin.

Ghi nhận tiền mặt là xác nhận của nhân viên, không phải một biên lai VNPAY giả. Đơn chờ vận chuyển chưa được coi là đã nhận tiền mặt. Giá trị preview có thể thay đổi; server kiểm tra lại lúc chốt.

## Giới hạn còn phải nghiệm thu

Đổi/trả, hoàn tiền, hoàn tồn có kiểm toán, nhiều địa chỉ, nhập kho, thu/chi và đối soát báo cáo chưa tương đương hoàn toàn Bee. Phiếu theo schema hiện tại có khóa/mức tối thiểu/giảm tiền, chưa có đầy đủ quota/thời hạn/lượt dùng theo khách. Một số báo cáo lịch sử cần đối soát sau các vòng đổi hàng.

[Dữ liệu và maintenance](07-CATALOG-VA-DU-LIEU.md) · [Cấu hình dịch vụ](05-CAU-HINH-VA-TICH-HOP.md).
