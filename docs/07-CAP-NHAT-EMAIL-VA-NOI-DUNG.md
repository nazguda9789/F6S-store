# 07 — Sửa đăng ký email, banner và nội dung (19/10/2026)

[Mục lục](README.md) · [Cài đặt](01-CAI-DAT-VA-CHAY.md)

## 1. Vì sao không nhận được mã đăng ký?

Mã cũ gắn cứng địa chỉ From của một tài khoản khác, trong khi SMTP lấy thông tin từ biến môi trường. Hàm gửi thư chạy bất đồng bộ và bỏ qua lỗi gửi, nên trang đăng ký vẫn chuyển sang nhập OTP dù SMTP thất bại. Môi trường thử không có `MAIL_USERNAME`/`MAIL_PASSWORD`; sửa code không tự tạo tài khoản email hay App Password cho người chạy.

Đã thay đổi:

- From lấy từ `spring.mail.username`/`MAIL_USERNAME`, thống nhất với tài khoản gửi.
- Đăng ký/gửi lại mã chờ SMTP chấp nhận thư trước khi báo thành công. Khi thiếu cấu hình hoặc nhà cung cấp từ chối, hiện lỗi rõ ràng; không hiện OTP trên web hoặc log để né bước xác thực.
- Đăng ký/OTP dùng transaction: khi gửi thất bại, rollback dữ liệu vừa tạo, tránh email bị giữ bởi tài khoản chưa gửi được mã.
- Gửi lại dùng POST, kiểm tra khoảng cách 60 giây; gửi thành công mới vô hiệu mã đăng ký cũ. Mã có thời hạn 10 phút, không dùng lại sau xác thực.
- Chuẩn hóa email, encode tham số redirect; không giữ mật khẩu trong flash form sau lỗi. Có link mở lại bước xác thực cho tài khoản đã đăng ký nhưng chưa nhận mã.
- SMTP có timeout kết nối/đọc/ghi 10 giây. Thư thông báo đơn hàng vẫn best effort để không đổi hành vi đặt hàng khi SMTP thiếu.

SMTP chấp nhận **không bảo đảm thư vào Inbox**: vẫn có thể vào Spam, bị lọc hoặc bounce. Chưa kiểm chứng gửi đến hộp thư thật bằng tài khoản của người dùng.

## 2. Cấu hình Gmail trên máy của từng thành viên

1. Dùng Gmail của cửa hàng/nhóm được phép gửi thư, bật xác minh hai bước.
2. Tạo App Password theo hướng dẫn chính thức: https://support.google.com/accounts/answer/185833 . Nếu tài khoản không có mục này, kiểm tra chính sách tài khoản hoặc dùng nhà cung cấp SMTP được phép; không dùng mật khẩu đăng nhập Gmail thông thường.
3. IntelliJ → Run → Edit Configurations → cấu hình chạy ứng dụng → Environment variables:

   ```text
   MAIL_USERNAME=<email-gửi-của-bạn>
   MAIL_PASSWORD=<App-Password-của-email-đó>
   ```

4. Thay nội dung trong `< >` bằng giá trị thật trên máy, không nhập dấu ngoặc, không gửi khóa qua chat/Git. App Password thường được hiển thị theo nhóm; nhập liền, không kèm khoảng trắng phân nhóm.
5. Stop và Run lại đúng cấu hình đã chỉnh. Tạo tài khoản thử với email nhận bạn kiểm soát, kiểm tra Inbox/Spam. Nếu email đã tồn tại và chưa kích hoạt, mở link xác thực/gửi lại trên form thay vì import lại SQL.

Mặc định Gmail: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`. Các biến host/port/auth/TLS hỗ trợ nhà cung cấp hoặc SMTP sink nội bộ khi kiểm thử. Không tắt auth/TLS để dùng Gmail hoặc xử lý lỗi chứng chỉ. Project không tự nạp `.env`; biến trên máy bạn không tự sang máy khác khi tải GitHub.

Nếu dùng PowerShell thay IntelliJ, đặt biến trong chính phiên chạy đó, ví dụ `$env:MAIL_USERNAME = "email-cua-ban"`; nhập App Password riêng vào cấu hình cục bộ và không chia sẻ lịch sử lệnh. Không cần cài mail server trong XAMPP để Gmail gửi được thư; cần kết nối Internet cho SMTP và tài khoản hợp lệ.

## 3. Banner được khôi phục từ đâu?

Nguồn là thư mục `Hình ảnh/Banner` trong bộ source cũ người dùng cung cấp. Không tạo ảnh mới hay tải ảnh quảng cáo ngẫu nhiên.

| Ảnh gốc | File trong project | Hiển thị / đường dẫn đích |
|---|---|---|
| `banner1.jpg` | `/image/banners/source-banner1.jpg` | Slider 1 → danh mục Nike |
| `banner2.jpg` | `/image/banners/source-banner2.jpg` | Slider 2 → giày cỏ tự nhiên |
| `banner3.jpg` | `/image/banners/source-banner3.jpg` | Slider 3 → Zocker |
| `banner4.jpg` | `/image/banners/source-banner4.jpg` | Banner phụ → Adidas |
| `banner_x_munich-06_...jpg` | `/image/banners/source-munich.jpg` | Banner phụ → Futsal |

Tổng 5 ảnh local: 3 slider và 2 banner phụ, không phụ thuộc Cloudinary. Giữ ảnh gốc, không cắt/sửa nội dung thương hiệu trên ảnh. Slider có liên kết khi bấm. Truy vấn loại banner đã lọc `active=true`; trang chủ không lấy nhầm banner của mọi trang chỉ vì URL chứa dấu `/`.

Trong khi kiểm tra 12 trang danh mục, phát hiện bộ lọc giá lỗi `noUiSlider is not defined` khi CDN không tải được. Đã đóng gói noUiSlider 15.7.0 (MIT, giữ giấy phép trong `lib/nouislider-LICENSE.md`) và đổi template sang JS/CSS local.

## 4. Mỗi danh mục ít nhất 3 sản phẩm

Không tạo sản phẩm/giá/tồn kho giả. Giữ 50 sản phẩm và 13 bài viết nguồn; thêm 4 liên kết vào 2 danh mục đang thiếu:

| Danh mục | Trước | Thay đổi | Sau |
|---|---|---|---|
| Adidas F50 Messi | 1 | Đổi nhãn thành **Adidas nổi bật**, thêm `ADIDAS` và `Nemeziz` đã có; giữ mẫu F50 hiện tại và giữ URL `adidas-f50-messi-` | 3 |
| Nike Zoom Mercurial | 1 | Thêm Nike Air Zoom Mercurial Vapor 16 Elite FG Mbappé và Vapor 16 Pro FG Attack đã có | 3 |

Đổi nhãn Adidas để không trình bày Nemeziz như một mẫu F50 Messi. SQL chỉ đổi nhãn cũ khớp điều kiện, không ghi đè tên tùy chỉnh của chủ cửa hàng. Không đổi URL cũ nhằm tránh hỏng liên kết.

Kết quả trên bộ SQL cung cấp:

| Danh mục | Sản phẩm đang bật |
|---|---:|
| ADIDAS | 8 |
| WIKA | 9 |
| ZOCKER | 7 |
| Giầy Futsal | 9 |
| Giầy cỏ tự nhiên | 21 |
| Giầy cỏ nhân tạo | 15 |
| Adidas nổi bật | 3 |
| NIKE | 10 |
| JOMA | 8 |
| Hàng mới về | 5 |
| MIDUNO | 7 |
| Nike Zoom Mercurial | 3 |

Một sản phẩm có thể thuộc nhiều danh mục nên không cộng các dòng để tính tổng sản phẩm. Đây là kết quả trên seed hiện tại, không phải ràng buộc tự động buộc mọi danh mục do admin tạo sau này phải có 3 sản phẩm. Không thêm voucher/khuyến mại mới ngoài yêu cầu.

## 5. Cập nhật database không mất dữ liệu

- **Đã cài web:** pull/tải code mới, sao lưu DB, chọn đúng database trong phpMyAdmin → import [sql/cap_nhat_noi_dung_20261019.sql](../sql/cap_nhat_noi_dung_20261019.sql) → restart app → Ctrl+F5.
- **Cài mới:** SQL đầy đủ đã gồm nội dung này. Cảnh báo: `f6s_shop.sql` có DROP DATABASE; chỉ dùng khi chủ động cài mới, không import lại để sửa đăng ký/banner.
- Bản `sql/bo_sung_du_lieu.sql` cũng đã gồm cập nhật nội dung. Script nội dung kiểm tra trùng trước khi thêm, có thể chạy lại mà không tăng banner/liên kết. Không sửa tài khoản/mật khẩu, đơn hàng, giá hoặc tồn kho.
- Cuối script có truy vấn liệt kê danh mục chưa đủ 3 sản phẩm đang bật. Trên seed hiện tại kết quả rỗng; database tùy chỉnh cần tự đối chiếu và gán sản phẩm phù hợp, không tự tạo hàng loạt dữ liệu giả.
- SQL chỉ cập nhật nhóm bảng Java, không đồng bộ ngược sang nhóm bảng tiếng Việt được giữ lại từ nguồn cũ.

## 6. Phạm vi kiểm chứng

- 35 unit test Java chọn lọc (gồm 13 test mới về mail/đăng ký/banner), 9 test JavaScript.
- Import bản đầy đủ vào schema thử khác và chạy script nội dung lặp: giữ 50 sản phẩm, 5 banner, tất cả 12 danh mục đủ 3 sản phẩm đang bật.
- Kiểm tra HTTP/trình duyệt và SMTP sink cục bộ để xác minh thư được SMTP nhận, From, mã, gửi lại, xác thực và lỗi gửi/rollback. Đây không phải gửi thư thật tới Gmail.
- Chưa xác nhận SMTP thật, Inbox/Spam của người dùng, mọi phiên bản MariaDB/XAMPP hoặc chống lạm dụng đăng ký/gửi lại dưới tải đồng thời. Không coi giới hạn resend 60 giây là hệ thống chống spam hoàn chỉnh.
