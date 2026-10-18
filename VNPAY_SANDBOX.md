# Chạy demo VNPay Sandbox

1. Dùng JDK 17, bật MySQL XAMPP và chạy project như hướng dẫn README.
2. Trong IntelliJ: Run → Edit Configurations → chọn cấu hình chạy ứng dụng → Environment variables, thêm:

   ```text
   VNPAY_TMN_CODE=ELBGJW4B
   VNPAY_HASH_SECRET= COVAANSJMDJDJVYLISYWSZMDINZNAVJB
   ```

   Không nhập dấu `< >`. Không gửi khóa qua chat, không lưu khóa vào Git. Nếu khóa đã bị chia sẻ, yêu cầu đổi khóa trong cổng VNPay Sandbox hoặc liên hệ hỗ trợ.
3. Dừng và chạy lại ứng dụng bằng đúng cấu hình Run vừa sửa. Biến môi trường mới không áp dụng cho tiến trình đang chạy.
4. Chạy `ngrok http 8080`. Mở website qua địa chỉ HTTPS ngrok hiện tại, không mở localhost khi bắt đầu thanh toán. Giữ nguyên trình duyệt và phiên đăng nhập đến khi VNPay trả về.
5. URL trở về của ứng dụng là `https://<tên-miền-ngrok-hiện-tại>/check-vnpay`. Khai báo tên miền/URL phù hợp trong hồ sơ Sandbox nếu VNPay yêu cầu. Cấu hình forwarded headers giúp ứng dụng tạo URL HTTPS khi đi qua ngrok.
6. Thêm sản phẩm vào giỏ, điền đủ địa chỉ và chọn VNPay. Chỉ dùng tài khoản/thẻ thử do VNPay Sandbox cung cấp; không dùng thẻ thật.

`System.getenv("VNPAY_TMN_CODE")` đọc biến có **tên** `VNPAY_TMN_CODE`, không đọc biến có tên là mã merchant. Tương tự, phải dùng `System.getenv("VNPAY_HASH_SECRET")`, không đặt khóa bí mật bên trong `getenv(...)`.

Nếu VNPay báo **Không tìm thấy website**, kiểm tra mã TmnCode thuộc Sandbox, merchant đã được kích hoạt và endpoint đang là `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`. Ngrok không thay thế mã merchant. Nếu các thông tin đúng mà lỗi còn xuất hiện, gửi mã tra cứu cho hỗ trợ VNPay, không gửi Hash Secret.

Code kiểm tra thiếu cấu hình trước khi chuyển cổng, ký HMAC-SHA512, dùng giờ Việt Nam và thời hạn 15 phút. Kiểm thử tự động sử dụng khóa giả; chưa chứng minh merchant đã được VNPay kích hoạt hoặc một giao dịch Sandbox đã thành công. Đây là luồng demo trả về theo phiên trình duyệt, không phải xác nhận sẵn sàng thu tiền thật/IPN production.
