# Giao hàng, yêu cầu hủy, VNPAY quá hạn và lưu ảnh

## Nâng cấp bản đang có dữ liệu

Sao lưu SQL Server `f6s_shop` và thư mục ảnh, dừng backend. Bản đã nâng 03/04 chạy thêm `database/05_upgrade_fulfillment.sql` trong SSMS, rồi cập nhật code và chạy lại backend/Vue. Không chạy lại 01 trên database có bảng. Bản cũ hơn cần 03 → 04 → 05 theo thứ tự. Database hoàn toàn trống dùng 00 nếu cần, sau đó 01 mới nhất → 02.

05 chỉ thêm các cột nullable và chỉ mục, không xóa dữ liệu hoặc thay đổi tồn/thanh toán. Không tự dựng thời điểm hoàn kho cho đơn cũ. Schema 24 bảng được xuất từ SQLServerDialect; kiểm thử dùng H2 MSSQL mode, chưa thay thế nghiệm thu script trên SQL Server thật.

## Hai trạng thái độc lập

Danh sách hóa đơn, chi tiết, bản in và Đơn hàng của tôi hiển thị riêng **giao hàng** và **thanh toán**. Ví dụ đơn có thể Đang hoàn về nhưng Đã thanh toán; hủy đơn không tự hoàn tiền. Có bộ lọc thanh toán và yêu cầu hủy ở trang quản trị.

Luồng giao thành công:

```text
Chờ xác nhận → Xác nhận → Đóng gói → Đang giao → Hoàn tất
```

Luồng giao thất bại:

```text
Đang giao → Giao thất bại → Giao lại (Đang giao)
                         ↘ Đang hoàn về → Đã nhận hàng hoàn
```

COD chưa trả tiền vẫn xử lý bình thường, hoàn tất giao/thu tiền mới ghi PAID. Với VNPAY, admin có thể kiểm tra/xác nhận/đóng gói khi đang chờ tiền, nhưng backend chặn giao và hoàn tất tới khi IPN xác nhận PAID. Không được nhập tay PAID để bỏ qua IPN.

## Khách yêu cầu hủy

Khách đăng nhập, mở `/account`, bấm **Tải lại đơn hàng**, nhập lý do và **Gửi yêu cầu hủy đơn**. Chỉ chủ tài khoản đặt đơn online được làm, ở Chờ xác nhận/Xác nhận/Đóng gói. Đơn vãng lai chưa có cơ chế tự hủy bằng mã đơn: cần liên hệ cửa hàng; không tự nhận đơn theo email hoặc số điện thoại.

Yêu cầu tạo REQUESTED, **chưa hủy và chưa cộng tồn**. Admin lọc Yêu cầu hủy → Chờ duyệt, mở chi tiết, nêu lý do và duyệt/từ chối. Trong khi chờ quyết định, không tiếp tục xử lý/tạo vận đơn. Từ chối cho phép tiếp tục xử lý; khách nhìn thấy phản hồi của cửa hàng. Duyệt chuyển CANCELLED và trả tồn/lượt mã đúng một lần. Nếu có vận đơn, phải xác nhận đã hủy ở hãng và hàng chưa bàn giao. Đây là xác nhận thủ công, F6S không tự gọi hãng để hủy.

Đã trả tiền mà hủy: giữ trạng thái tài chính PAID, thực hiện hồ sơ hoàn tiền riêng. Không xóa lịch sử.

## Nhận hàng hoàn và tồn kho

Admin mở chi tiết đơn đang giao, nhập lý do rồi **Ghi nhận giao thất bại**. Có thể giao lại hoặc **Chuyển sang đang hoàn về**. Hai trạng thái này không cộng tồn và không được hủy như đơn chưa giao.

Khi thực sự nhận lại hàng: kiểm tra toàn bộ, tích **Đã nhận toàn bộ hàng về kho, kiểm tra đủ và có thể bán lại**, rồi **Xác nhận đã nhận hàng hoàn**. Backend dùng khóa dòng đơn và dấu hoàn kho để tránh cộng lặp, kể cả hai yêu cầu đồng thời. Hàng hỏng/thiếu/không thể bán lại chưa được hỗ trợ nhập lại một phần; không tích xác nhận sai để đóng đơn.

RETURNED không tự thu/hoàn tiền. COD chưa thu thì giữ UNPAID; VNPAY đã thu giữ PAID và có thể mở hồ sơ hoàn toàn bộ tiền. Hồ sơ tiền hoàn không cộng tồn lần nữa vì nhận hàng hoàn đã xử lý trước đó. Hoàn tiền vẫn thực hiện ngoài hệ thống rồi ghi chứng từ, chưa tự chuyển tiền qua ngân hàng/VNPAY.

## VNPAY quá hạn sau đối soát

Vào Quản lý giao dịch → Danh sách hóa đơn → **VNPAY quá hạn cần đối soát**. Danh sách gồm đơn trước khi giao, chưa ghi nhận thu tiền, đã qua hạn link cộng thời gian chờ. Hạn link mặc định 15 phút; thời gian chờ mặc định 300 giây, cấu hình `VNPAY_RECONCILIATION_GRACE_SECONDS` nếu cần.

Admin phải kiểm tra giao dịch trên merchant portal VNPAY. Nếu thực sự chưa thu tiền: mở chi tiết, ghi mã tham chiếu lần đối soát, tích xác nhận chưa thu tiền (và xác nhận hủy vận đơn nếu có), bấm **Hủy đơn quá hạn sau đối soát**. Hệ thống lưu người/thời điểm/tham chiếu và trả tồn đúng một lần.

Đây là **đối soát thủ công có xác nhận**, chưa tự gọi VNPAY QueryDr, chưa có tác vụ nền tự kiểm tra rồi hủy. Không có IPN không chứng minh chưa trả tiền. Khi portal đã báo thu tiền mà IPN chưa tới, không dùng thao tác hủy do chưa thanh toán; cần khắc phục IPN và đối soát khoản thu. Admin cần kiểm tra hàng chờ thường xuyên để giải phóng tồn cho đơn thực sự chưa trả.

Nếu IPN PAID đến trước thao tác hủy, backend từ chối hủy do quá hạn chưa trả tiền. Nếu tiền về sau khi đã hủy, ghi nhận PAID trên đơn CANCELLED để hoàn tiền, không mở lại đơn hoặc trừ tồn nữa. Kiểm thử có tình huống IPN và hủy quá hạn chạy đồng thời.

## Ảnh nằm ở đâu?

Ảnh **tải lên qua Thư viện hình ảnh và lưu thành công** được giữ qua các lần khởi động backend. Không phải chỉ xem tạm trong trình duyệt.

| Nơi lưu | Nội dung |
|---|---|
| `MEDIA_DIR` | File JPEG/PNG thật, tên file theo hash |
| SQL `media_assets` | ID, tên gốc/tên file lưu, loại, kích thước, hash, người tải, trạng thái |
| SQL `product_color_images` | Liên kết sản phẩm, màu và ảnh |
| SQL `products.image_url`, `hover_image_url` | Địa chỉ phục vụ ảnh chính/ảnh hover |

Mặc định `MEDIA_DIR=./uploads/media`: khi chạy backend từ thư mục `backend`, vị trí là `backend/uploads/media`. URL `/api/media/<UUID>` để trình duyệt đọc ảnh từ backend. SQL **không lưu toàn bộ byte ảnh dạng BLOB**.

Nên đặt đường dẫn bền vững bên ngoài source, ví dụ trên Windows:

```text
MEDIA_DIR=C:\F6S-data\media
```

Nếu đổi đường dẫn khi đã có ảnh: dừng backend, **sao chép toàn bộ nội dung thư mục ảnh cũ sang đường dẫn mới, giữ nguyên tên file**, rồi chạy lại. Không chỉ đổi biến mà bỏ quên file.

Khi sao lưu/chuyển máy/cập nhật bằng ZIP: sao lưu cả database **và** thư mục ảnh. Chỉ import SQL sẽ không khôi phục được file ảnh. Thư mục upload không commit GitHub. Ảnh gửi trong chat hoặc còn trên máy bạn chưa tự được nhập vào database/web: cần tải lên Thư viện và gắn vào sản phẩm.

Nếu chỉ điền một URL ảnh bên ngoài vào sản phẩm, SQL lưu URL đó; backend không tự tải bản sao file về `MEDIA_DIR`. Ảnh placeholder có sẵn trong source cũng không phải ảnh đã tải lên Thư viện.

Kiểm tra metadata trong SSMS (không xóa/sửa trực tiếp):

```sql
USE f6s_shop;
SELECT TOP (20) id, original_name, stored_name, active, created_at
FROM dbo.media_assets
ORDER BY created_at DESC;

SELECT p.code, p.name, i.color, m.original_name, m.stored_name
FROM dbo.product_color_images i
JOIN dbo.products p ON p.id = i.product_id
JOIN dbo.media_assets m ON m.id = i.media_id;
```

Bản này cũng sửa lỗi sắp xếp sản phẩm `ORDER BY id DESC,id` bị SQL Server từ chối. Không cần xóa sản phẩm/database để khắc phục.
