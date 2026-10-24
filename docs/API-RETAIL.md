# API và nghiệp vụ F6S Vue/SQL Server

Luồng code: Controller → Service (`@Transactional` ở ranh giới ghi) → Repository → Entity. API không serialize Entity trực tiếp. Admin API yêu cầu HTTP Basic, mật khẩu qua môi trường, chỉ dùng localhost hoặc HTTPS. Vue giữ credential trong bộ nhớ, không localStorage; reload sẽ đăng nhập lại. Chưa port cơ chế role/nhân viên từ bản MySQL.

## 1. Sinh biến thể — backend là nguồn quyết định

- `POST /api/admin/products/matrix/preview`: kiểm tra lựa chọn, trả toàn bộ màu × size; chưa ghi database.
- `POST /api/admin/products/matrix`: nhận metadata và `matrix`, sinh và lưu sản phẩm + tất cả biến thể trong transaction.
- `PUT /api/admin/products/{id}`: sửa danh sách biến thể đã xác nhận; giữ ID/SKU của màu–size cũ, ngừng bán biến thể bị bỏ, không xóa lịch sử hóa đơn.

Body tạo sản phẩm:

```json
{
  "name":"F6S Velocity Pro","brand":"F6S","description":"Giày chạy bộ mẫu.",
  "imageUrl":"/images/shoe-0.svg","hoverImageUrl":"/images/shoe-1.svg",
  "categoryId":1,"featured":true,
  "matrix":{
    "colors":["Đen","Trắng"],"sizes":["40","41"],
    "defaultStock":10,"defaultPrice":1290000,
    "overrides":[{"color":"Trắng","size":"41","stock":0,"price":1390000}]
  }
}
```

Preview nhận riêng object `matrix`. `overrides` bắt buộc là array (có thể rỗng), không được chứa tổ hợp ngoài lựa chọn. Tối đa 100 tổ hợp, không màu/size trùng sau trim/case normalization; size EU 35–50 (có .5), stock nguyên 0–100000, giá VND nguyên 1–1 tỷ. Mỗi biến thể có `id`, `sku`, `price`, `stock`, `qrPayload`. Không dùng ID tự tăng làm nội dung QR.

Giá/tồn được lưu ở `product_variants`; giá từ trên `products` là giá thấp nhất các biến thể đang bán, tồn tổng hợp không lưu trùng nguồn. Bộ lọc màu/size/giá kiểm tra cùng một biến thể còn hàng. PDP và giỏ Vue lấy giá của biến thể được chọn; checkout không nhận giá từ client làm giá bán.

## 2. POS và quét QR

| Method | Endpoint dưới `/api/admin/pos/drafts` | Nội dung |
| --- | --- | --- |
| GET | `/` | Hóa đơn OPEN của người đang đăng nhập |
| POST | `/` | Tạo với `{ "requestKey":"UUID" }` |
| POST | `/{id}/scan` | Thêm SKU/QR, có idempotency key |
| PUT | `/{id}/lines/{variantId}` | Đặt số lượng `{ "quantity":2 }` |
| DELETE | `/{id}/lines/{variantId}` | Bỏ dòng hàng |
| DELETE | `/{id}` | Hủy hóa đơn tạm, không đổi tồn |
| POST | `/{id}/quote` | `{ "promotionCode":"F6S10" }` tính lại tổng |
| POST | `/{id}/checkout` | Chốt tiền mặt, trả invoice |
| GET | `/{id}/receipt` | Đọc invoice đã thanh toán, kiểm tra chủ sở hữu |

Body quét:

```json
{"eventKey":"a077b0f4-e2b1-45d0-a6ac-586459771502","code":"F6S:VARIANT:SKU-0123456789ABCDEF","quantity":1}
```

Chấp nhận SKU thô `SKU-...` hoặc `F6S:VARIANT:SKU-...`, không mở URL/không tải tài nguyên từ nội dung QR. Lấy SKU thật từ API chi tiết sản phẩm; ví dụ trên chỉ minh họa định dạng. Quét lại có chủ đích dùng eventKey mới; thử lại cùng request sau lỗi mạng giữ eventKey cũ. Dùng lại key với payload khác bị chặn. Máy quét USB ở chế độ bàn phím nhập mã rồi Enter; camera dùng ZXing và cần HTTPS/localhost + quyền camera. Tem QR nằm ở Quản lý sản phẩm → Mã QR → In tem. Camera/USB phần cứng chưa nghiệm thu trên thiết bị thực.

Giới hạn **5 OPEN/người bán**, không phải 5 hóa đơn toàn hệ thống. Tạo hóa đơn khóa dòng operator trước khi đếm/tạo nên không vượt giới hạn khi mở nhiều tab. Hóa đơn tạm không đặt giữ hàng; số lượng tối đa 99/biến thể, 50 dòng. Mỗi thao tác kiểm tra chủ sở hữu bằng Principal trên server, không tin owner từ request.

Body chốt POS:

```json
{"cashReceived":2000000,"promotionCode":"F6S10","expectedTotal":1161000}
```

`expectedTotal` là tổng người bán vừa xem, không phải giá tính tiền. Backend khóa sản phẩm theo ID tăng dần, kiểm tra biến thể/stock, lấy giá hiện hành, kiểm tra mã, đối chiếu tổng đã xác nhận rồi trừ kho + tạo invoice + đóng draft trong một transaction. Tổng đổi → rollback và yêu cầu tải/tính lại. Tiền khách đưa phải là VND nguyên và đủ tổng; tiền thừa do server tính. Gửi lại thanh toán cùng draft không tạo đơn/trừ kho/lượt mã lần hai. Payload tiền/mã khác với hóa đơn đã chốt bị chặn.

Invoice lưu snapshot tên/SKU/màu/size/giá, subtotal, discount, total, cashReceived/cashChange. CSS in chỉ in receipt, không in sidebar/nút bấm; đây là hóa đơn bán hàng, **không phải hóa đơn VAT điện tử**.

## 3. Khuyến mãi theo sản phẩm

`GET/POST /api/admin/promotions`, `PUT/DELETE /api/admin/promotions/{id}`. DELETE ngừng hoạt động, không xóa lịch sử. Mã không đổi sau tạo. Body:

```json
{
  "code":"F6S10","name":"Ưu đãi giày chạy bộ","type":"PERCENT","value":10,
  "minSubtotal":500000,"maxDiscount":200000,"maxUses":100,
  "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-11-01T00:00:00Z",
  "active":true,"productIds":[1,2]
}
```

- `PERCENT`: 1–100%, làm tròn xuống đến đồng; `FIXED`: số tiền nguyên.
- Chỉ tính phần hàng trong `productIds`; minimum áp trên phần đủ điều kiện, không tính hàng ngoài danh sách.
- Giảm không vượt eligible subtotal, có thể thêm trần maxDiscount; không âm tổng tiền.
- Thời gian bắt đầu inclusive/kết thúc exclusive; `maxUses` được khóa khi redeem để tránh vượt lượt.
- Lượt được giữ khi đơn online được tạo (COD đang chờ) hoặc POS hoàn tất; hủy đơn online chờ giải phóng một lần. Không có quota từng khách/stack nhiều mã.
- Checkout online nhận thêm `promotionCode` và kiểm tra bằng cùng service với POS. Vue storefront chưa có màn gợi ý mã; có thể gửi qua API, UI quản trị/POS đã có.

## 4. Đơn hàng và dashboard

`GET /api/admin/orders?page=0` trả trang 20 đơn. `PUT /api/admin/orders/{id}/status` nhận `{"status":"COMPLETED"}` hoặc `CANCELLED`.

Online COD bắt đầu PENDING, đã trừ tồn khi đặt; COMPLETED chỉ sau giao/thu tiền thực tế, không trừ kho lại. CANCELLED chỉ từ PENDING, hoàn kho và lượt mã đúng một lần. Đơn COMPLETED không tự hủy/hoàn tiền. Chưa có nghiệp vụ đổi trả, hoàn tiền ngân hàng hoặc đối soát.

`GET /api/admin/dashboard?from=2026-10-01&to=2026-10-31&group=day` (`group=month` để nhóm tháng), tối đa 366 ngày.

- Doanh thu thuần sau giảm giá: chỉ COMPLETED, lọc theo **completedAt**.
- Tỷ lệ thành công: số đơn hiện COMPLETED / tổng đơn **tạo trong kỳ** (gồm chờ/hủy). Hai metric có tập thời gian khác nhau, được ghi rõ trên UI.
- Bucket ngày/tháng theo `Asia/Ho_Chi_Minh`, dùng khoảng nửa mở `[đầu ngày, đầu ngày sau ngày cuối)`, bao gồm bucket 0. Repository JPQL SUM/COUNT và top 10; service chia bucket lịch để tránh phụ thuộc hàm thời gian riêng vendor.
- Top bán chạy tính số đôi trên đơn hoàn thành; cột tiền sản phẩm là **gross trước giảm giá cấp đơn**, không phải phân bổ doanh thu thuần.
- [Truy vấn T-SQL tham khảo](../database/10_dashboard_queries.sql) thực hiện group trực tiếp cho dataset lớn. API hiện thực hiện 1 aggregate mỗi bucket (tối đa 366); cần đánh giá tải và thay native-group/caching khi triển khai lớn.

## 5. Kiểm thử và giới hạn chuyển đổi

`backend/src/test/.../AdvancedWorkflowTest` kiểm tra matrix, duplicate/limit, tạo 5 draft đồng thời, quét idempotent, quyền sở hữu, hết hàng, rollback tiền/stock/mã, checkout đồng thời, mã giảm theo sản phẩm, hủy đơn và múi giờ thống kê. Bộ cũ `ShopIntegrationTest` giữ kiểm tra CRUD, validation và checkout online. Test dùng database H2 MSSQL mode riêng, không kết nối MySQL đang dùng.

UI E2E trong `frontend/e2e`: storefront và admin trên desktop/mobile. Admin cần `E2E_ADMIN_PASSWORD` (và username nếu khác admin); chỉ chạy với database test vì tạo sản phẩm, mã giảm, đơn và trừ tồn. Chưa kiểm thử SQL Server thật vì image Microsoft bị chặn tải; H2 không thay thế nghiệm thu locking/schema/Unicode trên SQL Server. Chưa thể khẳng định tương đương hoàn toàn video Bee.
