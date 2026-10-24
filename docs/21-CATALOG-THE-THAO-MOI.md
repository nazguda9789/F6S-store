# F6S Sports — catalog mới, không dùng lại bộ giày cũ

## Phạm vi

Thay dữ liệu và nội dung catalog theo cấu trúc quản lý giày đã quan sát từ Bee; giữ thương hiệu F6S, mã OTP và luồng VNPAY hiện tại. Không có source/model nội bộ của Bee để khẳng định cấu trúc database giống 100%. Các nghiệp vụ đã có về POS/thu tiền mặt/SKU/voucher/ảnh/tài khoản vẫn được giữ; phần đổi trả, hoàn tiền, kho và báo cáo cần tiếp tục nghiệm thu riêng.

Không dùng lại ảnh hoặc tên giày Wika/Mercurial và bộ catalog nguồn. Dữ liệu mới là **mẫu trình diễn**, không phải thông tin hàng hóa thật được nhà sản xuất xác nhận. Có dấu `[MẪU]`, ảnh SVG mới có nhãn DEMO, mô tả công khai giới hạn. Cần thay ảnh, mô tả, giá và tồn thực trước khi kinh doanh; không tự tuyên bố hàng chính hãng.

## Catalog mới

| Nội dung | Bộ mới |
| --- | --- |
| Danh mục | Chạy bộ, Tập luyện, Bóng rổ, Đi bộ, Sneaker |
| Mẫu giày | 25: Velocity Run, Power Train, Court Jump, Daily Walk, Urban Move; 5 mẫu mỗi nhóm |
| Hãng | Nike, Adidas, Puma, New Balance, Converse — tên thuộc tính minh họa, không xác minh hàng hóa |
| Đế | Cao su, EVA, Phylon, TPU, Foam |
| Chất liệu | Lưới Mesh, Da tổng hợp, Vải Knit, Canvas, Da lộn |
| Màu | Đen, Trắng, Xám, Xanh navy, Đỏ |
| Kích cỡ | EU 38–42 |
| Biến thể | 250: 2 màu × 5 size mỗi mẫu; tồn demo 10 mỗi biến thể |
| Nội dung | 5 banner mới và 5 cẩm nang mới |
| Mã | Mẫu `F6S-SP…`, biến thể `F6S-SKU…`, server cấp mã ổn định theo ID |

Ảnh mới nằm ở `src/main/webapp/image/sports/`. Năm ảnh minh họa dùng theo nhóm, không phải 25 ảnh chụp sản phẩm riêng. Chưa có ảnh thật do cửa hàng cung cấp.

Trang giới thiệu cũng được viết lại theo thể thao đa hoạt động, bỏ bộ ảnh/ngôi sao/giày bóng đá cũ và lời chứng thực khách hàng mẫu. Bỏ số điện thoại đặt mua hardcode; dẫn về trang Liên hệ. Không tiếp tục gắn cam kết đổi trả/bảo hành hoặc freeship chưa được đối chiếu với cấu hình và chính sách thực tế. Danh sách giày mặc định Đang bán; cảnh báo tồn/tổng tồn hiện hành chỉ tính giày và biến thể đang hoạt động, không cộng kho đã ngừng bán vào catalog mới.

## A. Máy đang dùng: giữ tài khoản, OTP, VNPAY và hóa đơn

**Đây là thay toàn bộ catalog đang có**, kể cả giày tự thêm sau những bản trước. Không chạy nếu vẫn muốn tiếp tục bán catalog cũ. Thử trên bản sao database trước, dừng web và Export backup đầy đủ.

Chọn thời điểm bảo trì không còn khách đang thanh toán: hoàn tất/đối soát các giao dịch VNPAY đang chờ trước khi dọn giỏ và ngừng bán giày cũ. Không dùng việc đổi catalog để tự hủy giao dịch ngân hàng; cấu hình OTP/VNPAY không đổi nhưng dữ liệu giỏ đang thanh toán vẫn cần được bảo vệ trong quá trình chuyển phiên bản.

1. Nếu chưa nâng schema các đợt trước, chạy bản `sql/nang_cap_f6s.sql` hiện hành trước. Nếu đã ở bản POS 24/10, bước này không bắt buộc.
2. Import `sql/thay_catalog_the_thao_20261024.sql` vào đúng database đang dùng. File tự thêm cột `blog.archived` cần cho code mới.
3. Pull code, JDK 17, khởi động lại và Ctrl+F5. **Giữ nguyên datasource, biến môi trường và secret hiện tại**; không đổi database để tiếp tục dùng OTP/tài khoản thực của bạn.

SQL thực hiện:

- Lưu cờ hiển thị cũ vào `catalog_release_backup` và ghi release một lần.
- Ngừng bán giày/biến thể cũ, ngừng dùng thuộc tính/danh mục cũ; không xóa giá/tồn/mã cũ hay dòng được hóa đơn tham chiếu. Một số tên hãng trùng bộ mới được tái sử dụng ID, không đổi tên lịch sử.
- Ẩn banner cũ, lưu trữ bài cũ; tạo bộ mới. Bài cũ vẫn xem/sửa được ở admin qua bộ lọc Lưu trữ, không xuất hiện ở trang chủ/danh sách bài hoặc URL công khai.
- Lưu giỏ cũ vào `catalog_release_cart_archive`, rồi dọn giỏ hiện tại vì chứa giày ngừng bán. **Đây là phần dữ liệu tạm được dọn, có bản dự phòng để khôi phục**. Không tự xóa đơn chờ/hóa đơn: đơn chờ cũ cần thay giày ngừng bán trước khi chốt.
- Không sửa người dùng/mật khẩu, OTP, pending registration, hóa đơn/chi tiết, lịch sử thanh toán, cloud key/Telegram/Shopee key hoặc secret VNPAY.

Chạy lại không thêm mẫu, không bật lại hàng đã ngừng, không đặt lại tên/slug/giá/tồn đã sửa. Không xóa marker để ép chạy lại. Chỉ import nối tiếp, không chạy khi web xử lý đơn.

Nếu chỉ muốn chạy code mới mà chưa thay catalog, import `sql/cap_nhat_luu_tru_bai_viet_20261024.sql` để thêm cột archive (và bảo đảm đã nâng schema POS trước đó). Việc thay catalog vẫn là bước riêng có ảnh hưởng dữ liệu như mô tả trên.

## B. Cài mới sạch

[Gói SQL Sports mới](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SPORTS-CATALOG-20261024.zip) → Download raw.

File `f6s_shop.sql` mới chỉ có schema ứng dụng đang dùng, 25 giày mới và nội dung mới; **không còn INSERT bộ giày nguồn, bảng nghiệp vụ tiếng Việt hoặc khóa dịch vụ cũ**. Các bảng kỹ thuật cần cho Java như ledger biến thể vẫn được tạo trống để ứng dụng chạy `validate`.

- Cần JDK 17, MySQL 8, Maven hoặc Maven Wrapper, IntelliJ nếu muốn chạy IDE.
- Bật MySQL, dùng user có quyền tạo database/routine; import `f6s_shop.sql`.
- File tạo **database mới** `f6s_shop`, không có DROP DATABASE. Nếu đã tồn tại: dừng import, không bấm tiếp tục bỏ qua lỗi. Muốn giữ database hiện tại dùng cách A; muốn cài mới tên khác thì sửa cả CREATE DATABASE và USE trước khi import.
- Tài khoản admin mới: `admin@gmail.com` / `123123`, chỉ dùng local và phải đổi trước công khai. Không reset tài khoản này trong cách A.
- Đặt datasource theo máy, chạy `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`), mở `http://localhost:8080`.
- Bản cài cơ bản chưa có người mua/nhân viên hoặc hóa đơn giả. Muốn đủ dữ liệu trình diễn từng màn dùng cách C.

Các file ZIP trước ngày/đợt Sports được giữ làm lịch sử, không dùng để cài catalog mới. `sql/bo_sung_du_lieu.sql` cũ đã bị chặn để không vô tình đưa giày cũ trở lại; bản nguồn cũ chuyển vào `sql/legacy/` chỉ để tra cứu. Seed catalog cũ cũng từ chối chạy sau release Sports.

## C. Demo đủ các màn

[Gói demo Sports](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SPORTS-DEMO-20261024.zip) → Download raw → import `f6s_shop_demo.sql` vào MySQL. Tạo database riêng `f6s_shop_demo`, không thay database cửa hàng. Đổi datasource riêng cho bản trình diễn:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/f6s_shop_demo
```

Gói này có cùng 25 giày/250 biến thể mới, cộng 5 nhân viên, 5 khách hàng, 5 voucher, giỏ, đơn chờ, hóa đơn/chi tiết/lịch sử, giảm giá, liên hệ, tin nhắn và lịch sử xem mỗi nhóm. Banner/bài viết giữ đúng 5 của catalog mới, không nhân đôi. Các dòng nghiệp vụ đều DEMO; hóa đơn giả sẽ ảnh hưởng báo cáo trong database demo.

Tài khoản nhân viên/khách mẫu do admin cấp, đang ngừng hoạt động và không có mật khẩu dùng chung. Trong bản demo Sports mới, chúng được đánh dấu cấp bởi admin (như luồng thêm người dùng tại quản trị), không giả một lần nhập OTP công khai. Admin vào form tài khoản, đặt mật khẩu và bật Hoạt động riêng nếu cần thử đăng nhập. Việc này không thay đổi luồng OTP cho người đăng ký thật hoặc cập nhật tài khoản cũ khi chuyển catalog. Không tạo OTP, key dịch vụ, biên lai VNPAY hoặc ghi nhận tiền mặt giả. Mã demo không gửi mail thật. Không đưa bản demo lên Internet với mật khẩu admin mặc định.

## Khôi phục

Ưu tiên backup đầy đủ trước import. Khi cần phục hồi cờ hiển thị catalog cũ: dừng web, sao lưu trạng thái hiện tại, chạy `sql/khoi_phuc_catalog_truoc_sports_20261024.sql`. File khôi phục cờ gốc và giỏ đã dự phòng, ngừng dùng catalog mới; không xóa sản phẩm mới hoặc đơn phát sinh sau đó, không đặt lại tồn kho. Giỏ mới phát sinh với sản phẩm mới bị ngừng có thể cần người dùng dọn lại.

Rollback chỉ chạy một lần theo trạng thái release; chạy lại không ghi đè thay đổi mới. Sau rollback không tự áp dụng lại cùng release bằng việc xóa marker; cần kế hoạch release mới. Đây không phải phục hồi toàn bộ database như một bản backup.

## Kiểm chứng và giới hạn

115 test Java chọn lọc và 12 test JavaScript đạt; không phải toàn bộ suite hay chứng nhận production.

- Kiểm thử MySQL 8.4 trên schema riêng: bản cài sạch, thay catalog từ bộ 55 giày cũ, chạy lại và rollback; tồn gốc, tài khoản/OTP/đăng ký chờ/lịch sử thanh toán/khóa cấu hình được đối chiếu không đổi.
- Kiểm tra browser trang chủ, 5 danh mục, 25 trang giày, 250 biến thể/SKU, ảnh mới, bài viết và admin/khách/nhân viên.
- Giữ test hồi quy đăng ký, OAuth và VNPAY; không sửa code hoặc secret của các tích hợp này. Chưa kiểm thử thư Gmail thật, tiền thật, XAMPP/MariaDB mọi phiên bản hoặc toàn bộ nghiệp vụ Bee.

Schema nguồn ở `sql/schema_f6s_sports_20261024.sql` không chứa dữ liệu. `node scripts/build-sports-catalog.cjs build` tạo lại các SQL/ảnh từ cấu hình mới, không đọc database. `node scripts/build-demo-sql.cjs /tmp/f6s_shop_demo.sql` tạo installer demo. Không lấy dump có dữ liệu từ database đang chạy để đóng gói.
