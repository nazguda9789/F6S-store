# Tài liệu F6S-store

[Về trang chính](../README.md)

Tài liệu đối chiếu mã nguồn đến ngày **19/10/2026**, dành cho người cài demo, thành viên phát triển và người viết báo cáo. “Có trong code” không đồng nghĩa “đã kiểm thử đầy đủ”.

## Đọc theo mục đích

| Bạn cần làm gì? | Tài liệu |
|---|---|
| Cài lần đầu, biết cần tải gì, chạy IntelliJ + XAMPP | [01 — Cài đặt và chạy](01-CAI-DAT-VA-CHAY.md) |
| Hiểu MVC, các lớp, model/entity, bảng và quan hệ | [02 — Kiến trúc và model](02-KIEN-TRUC-VA-MODEL.md) |
| Biết từng vai trò được dùng màn nào, chức năng gì | [03 — Chức năng và phân quyền](03-CHUC-NANG-VA-PHAN-QUYEN.md) |
| Hiểu luồng mua hàng, tại quầy, thanh toán, đơn hàng | [04 — Luồng nghiệp vụ](04-LUONG-NGHIEP-VU.md) |
| Cấu hình email, VNPay, GHN, upload và dịch vụ khác | [05 — Cấu hình và tích hợp](05-CAU-HINH-VA-TICH-HOP.md) |
| Chạy kiểm thử, tìm lỗi và biết giới hạn hiện tại | [06 — Kiểm thử và xử lý lỗi](06-KIEM-THU-VA-XU-LY-LOI.md) |
| Sửa gửi mã đăng ký, lấy banner cũ, cập nhật danh mục đủ sản phẩm | [07 — Email và nội dung 19/10](07-CAP-NHAT-EMAIL-VA-NOI-DUNG.md) |
| Google, tài khoản chờ OTP và điều hướng sau đăng nhập | [08 — Google và xác thực tài khoản](08-DANG-NHAP-GOOGLE-VA-XAC-THUC.md) |
| Bộ khung quản trị theo ảnh tham khảo và phạm vi đã đổi | [09 — Giao diện Workspace](09-GIAO-DIEN-WORKSPACE.md) |

## Tài liệu bổ trợ

- [Hướng dẫn VNPay Sandbox](../VNPAY_SANDBOX.md).
- [Danh sách dữ liệu SQL đã bổ sung](../DANH_SACH_BO_SUNG.md).
- [Lịch sử sửa lỗi và giao diện](../CAP_NHAT_UI_VA_LOI.md).
- [Nguồn và giới hạn danh mục địa chỉ](../src/main/webapp/data/README.md).

## Lộ trình đề xuất

Người chạy demo: **01 → 05 (nếu dùng dịch vụ ngoài) → 06**. Người nhận code/phân công nhóm: **02 → 03 → 04 → 06**. Người viết tài liệu: dùng nội dung hiện trạng làm cơ sở, bổ sung ảnh chụp và kết quả kiểm thử của chính nhóm; không trình bày phần chưa xác minh như chức năng hoàn chỉnh.

Tài liệu trong thư mục này là Markdown, đọc trực tiếp trên GitHub; không yêu cầu bật GitHub Wiki hoặc GitHub Pages. Khi thay đổi nghiệp vụ, cập nhật mục tương ứng cùng commit sửa code.
