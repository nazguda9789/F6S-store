# Thao tác thuộc tính theo Bee — giữ nguyên OTP và VNPAY

Lưu ý bản POS tiếp theo **cần SQL tiền mặt** theo [tài liệu 20](20-LUONG-POS-THEO-BEE.md). Nội dung không cần SQL bên dưới chỉ mô tả riêng tính năng thêm nhanh thuộc tính.

## Giới hạn thay đổi

Không sửa mã xác thực OTP, Google OAuth, cấu hình mail, VNPAY, URL callback, TMN Code hoặc Hash Secret. Không thêm secret; không tự import SQL vào database đang dùng của bạn. Test Java về đăng ký/xác thực và VNPAY là kiểm thử hồi quy, không xác nhận gửi Gmail hoặc thu tiền thật.

Đợt này bổ sung thao tác catalog theo ảnh Bee, không tuyên bố toàn bộ nghiệp vụ đã tương đương Bee. Module giao dịch, trạng thái, hoàn tồn/đổi trả, nhiều địa chỉ, hoàn tiền và báo cáo vẫn cần nghiệm thu riêng theo [bảng luồng](14-CHUYEN-LUONG-NGHIEP-VU-BEE.md).

## Thao tác mới

Trang **Quản lý sản phẩm → Thêm sản phẩm** và trang cập nhật có nút thêm nhanh tại sáu ô:

| Nhóm | Nội dung thêm | Sau khi lưu |
| --- | --- | --- |
| Danh mục | Tên, đường dẫn tự gợi ý, nhóm cha hoặc nhóm gốc | Chọn ngay vào sản phẩm; nhóm gốc mới dùng được làm cha ngay |
| Thương hiệu | Tên, mô tả | Cấp mã `F6S-TH…`, chọn ngay |
| Loại đế | Tên, mô tả | Cấp mã `F6S-DE…`, chọn ngay |
| Chất liệu | Tên, mô tả | Cấp mã `F6S-CL…`, chọn ngay |
| Màu sắc | Tên, HEX `#RRGGBB` | Chọn ngay và thêm vào danh sách màu của biến thể |
| Kích cỡ | Tên tối đa 20 ký tự | Chọn ngay và thêm vào danh sách size của biến thể |

1. Nhập tên giày, giá, cân nặng.
2. Bấm `+ Thêm…`, điền thông tin, bấm **Thêm và chọn**. Không tải lại form, không mất tên/giá đang nhập.
3. Chọn màu và size, bấm **Tạo các biến thể đã chọn**, nhập tồn/giá từng biến thể.
4. Bổ sung ảnh/nội dung, bấm **Lưu sản phẩm**. SKU do server cấp; không dùng tên thương hiệu làm SKU.

**Thuộc tính được lưu ngay khi bấm Thêm và chọn**, kể cả sau đó bạn hủy tạo giày. Nút Hủy trong hộp thêm nhanh không ghi dữ liệu. Lỗi tên trùng/màu sai/danh mục cha sai hiện ngay trong hộp; dữ liệu sản phẩm vẫn giữ. Không tự đổi hoặc kích hoạt lại thuộc tính cũ.

Quyền: chỉ admin thêm dữ liệu thuộc tính. Nhân viên giữ quyền quản lý giày hiện có nhưng không có nút/API thêm nhanh thuộc tính. Endpoint không nằm dưới nhóm `/api/**` dành cho nhân viên; quyền được kiểm tra ở server, không chỉ ẩn nút.

Đường dẫn sản phẩm mới được gợi ý cả khi nhập, dán hoặc dùng bàn phím điện thoại. Sửa tên giày cũ không tự đổi URL; đường dẫn do người dùng nhập riêng cũng được giữ.

## Giữ dữ liệu OTP/VNPAY đang sử dụng

Nếu đã ở bản SQL catalog 24/10: chỉ pull code, dùng JDK 17, khởi động lại web và Ctrl+F5. **Không cần import SQL cho tính năng thêm nhanh này.** Giữ nguyên database/biến môi trường/secret/callback đang cấu hình.

Nếu chưa nâng schema catalog: sao lưu rồi dùng `sql/nang_cap_f6s.sql`, không import dump có DROP DATABASE vào dữ liệu cần giữ. Migration không reset mật khẩu tài khoản.

## Dữ liệu có sẵn cho từng màn

[Bộ demo tất cả module](18-DU-LIEU-MAU-TAT-CA-MODULE.md) có 5 nhân viên/5 khách và 5 dòng mỗi nhóm nghiệp vụ được hỗ trợ; mẫu giày có 25 biến thể. Đây là dữ liệu bổ sung, không ép tổng số dòng của bảng cũ về 5.

Để xem hóa đơn mẫu, cài `f6s_shop_demo.sql` trong gói ZIP vào database riêng; **không đổi website đang chạy sang database demo nếu muốn tiếp tục dùng dữ liệu OTP/tài khoản hiện tại**. Chạy bản trình diễn bằng cấu hình riêng. Không nhập hóa đơn giả vào dữ liệu thật. Các bảng OTP, pending registration, khóa dịch vụ và lịch sử thanh toán không được thêm 5 dòng giả chỉ để đủ số lượng.

## Kiểm chứng

- 95 test Java chọn lọc (gồm đăng ký/xác thực, VNPAY, catalog và 7 test thêm nhanh), 12 test JavaScript đạt.
- Trình duyệt: thêm đủ sáu nhóm, giữ dữ liệu nhập, chọn ngay vào ma trận, chặn tên trùng/HEX sai, lưu giày thật với SKU server; chặn chưa đăng nhập và nhân viên gọi API thêm thuộc tính.
- Hồi quy form tạo tổ hợp 2 màu × 2 size, giữ giá/tồn đã sửa, xóa dòng, lưu và tải lại.
- Không sửa file OTP/VNPAY/application properties trong commit này. Không kiểm thử thanh toán thật hay thay đổi secret của người dùng.
