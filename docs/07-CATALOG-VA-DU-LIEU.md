# Catalog Sports và dữ liệu

## Bộ hiện hành

5 nhóm: Chạy bộ, Tập luyện, Bóng rổ, Đi bộ, Sneaker. Mỗi nhóm 5 mẫu → **25 giày**, mỗi mẫu 2 màu × 5 size EU 38–42 → **250 biến thể**, tồn demo 10/biến thể. 5 hãng, 5 đế, 5 chất liệu, 5 màu, 5 size; 5 banner và 5 cẩm nang mới.

Hãng Nike/Adidas/Puma/New Balance/Converse là thuộc tính minh họa, không xác minh hàng hóa thương mại. Mẫu có nhãn [MẪU], ảnh SVG mới theo nhóm có DEMO. Không dùng ảnh/tên giày nguồn Wika/Mercurial làm catalog hiện hành. Thay ảnh, mô tả, giá, tồn và xác nhận chính sách thật trước khi kinh doanh.

## Cài mới

Theo [sql/install/README.md](../sql/install/README.md), chỉ import 01 + 02 để chạy catalog/admin. File 03 thêm 5 nhân viên/5 khách, 5 voucher/giỏ/đơn chờ/hóa đơn và các nội dung hỗ trợ mỗi nhóm. Không tạo OTP, khóa dịch vụ hoặc biên lai VNPAY. Khách/nhân viên demo do admin cấp, đang ngừng hoạt động và chưa có mật khẩu chung; admin đặt mật khẩu/bật riêng trong form nếu cần.

Các hóa đơn mẫu là snapshot lịch sử giả, không trừ tồn lúc import, không dùng để thử hoàn kho hoặc tính doanh thu thật. Mẫu giày/giá không phải catalog bán hàng được hãng xác nhận. Hóa đơn in từ web không phải hóa đơn điện tử VAT.

## Giữ database đang dùng

Không dùng file install hoặc file demo để nâng database thật. Backup đầy đủ, thử trên bản sao, đối soát thanh toán VNPAY đang chờ, dừng web. [SQL maintenance](../sql/README.md) gồm:

1. `maintenance/01_upgrade_existing.sql`: bổ sung schema hiện hành, không reset tài khoản/mật khẩu.
2. `maintenance/02_replace_catalog.sql`: **thay toàn bộ catalog đang có**, kể cả hàng tự thêm. Chỉ chạy khi thật sự muốn thay bộ hàng; không bắt buộc chỉ để cập nhật code.
3. `maintenance/03_restore_previous_catalog.sql`: khi cần phục hồi cờ hiển thị theo snapshot; không phải rollback toàn database.

Các file có routine nên chạy MySQL CLI, không dùng cùng bộ import phpMyAdmin. Dùng đúng database ở tham số CLI; giữ nguyên datasource/secret để tiếp tục dùng OTP/tài khoản thật.

## Tác động khi thay catalog

- Ngừng bán giày/biến thể cũ, ngừng dùng danh mục/thuộc tính cũ; **không xóa giá/tồn/mã hoặc dòng hóa đơn tham chiếu**. Một số hãng trùng tên được dùng lại ID, không đổi tên lịch sử.
- Banner cũ bị ẩn, bài cũ được lưu trữ; admin vẫn xem/sửa ở bộ lọc Lưu trữ. URL bài/sản phẩm đã ngừng không hiện công khai.
- Giỏ cũ được lưu vào `catalog_release_cart_archive` rồi dọn, tránh chứa hàng đã ngừng. Đơn chờ/hóa đơn không bị xóa; draft cũ cần thay dòng hàng ngừng bán trước khi chốt.
- Không sửa người dùng, OTP/đăng ký chờ, thanh toán, cấu hình dịch vụ, TMN/Hash Secret/callback. Không tự hủy giao dịch ngân hàng đang chờ.
- Chạy lại không đặt lại giá/tồn/tên/slug đã chỉnh. Không xóa marker để ép chạy lại.

## Khôi phục

Ưu tiên bản backup đầy đủ. File maintenance 03 khôi phục cờ cũ và giỏ dự phòng, ngừng dùng mẫu mới; không xóa đơn mới hoặc reset tồn. Giỏ mới với hàng bị ngừng có thể cần dọn lại. Chạy lại rollback không ghi đè lần nữa. Sau rollback cần kế hoạch release mới, không tự xóa marker để reapply.

Các ZIP/seed lịch sử đã gỡ khỏi main; Git history còn bản cũ để tra cứu. Ảnh nguồn có thể vẫn được hóa đơn lịch sử tham chiếu nên không xóa hàng loạt asset runtime.

## Tạo lại SQL cho người bảo trì

```bash
node scripts/build-sports-catalog.cjs build
node scripts/build-minimal-sql.cjs
```

Schema và demo template ở `scripts/sql-templates/`; không export dữ liệu thật để phát hành. Node chỉ cần cho bước tạo artifact, không cần để chạy website. Bản SQL không có routine phục vụ phpMyAdmin; các script maintenance có chủ đích riêng.
