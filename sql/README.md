# Chọn file SQL F6S

**Bản hiện hành là Sports**, xem [tài liệu 21](../docs/21-CATALOG-THE-THAO-MOI.md). Cài mới: `f6s_shop.sql` sạch, không DROP/không giày cũ. Đang dùng: backup + `thay_catalog_the_thao_20261024.sql` sau khi đã nâng schema POS. Seed `bo_sung_du_lieu.sql` cũ bị chặn; không dùng ZIP cũ bên dưới cho phiên bản này. Các phần sau là lịch sử trước Sports.

**POS mới nhất:** mã nguồn có `cash_received`/`cash_change`; chạy `cap_nhat_thanh_toan_tai_quay_20261024.sql` hoặc bản nâng cấp tổng hợp mới trước khi chạy web. [Gói SQL POS](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SQL-POS-20261024.zip) → Download raw. Không reset OTP/VNPAY. [Luồng và giới hạn](../docs/20-LUONG-POS-THEO-BEE.md).

**Mới nhất 24/10:** bắt buộc chạy bản `nang_cap_f6s.sql` hiện tại trước code mới để bổ sung metadata thuộc tính. Bộ mẫu riêng `du_lieu_mau_catalog_20261024.sql` thêm 5 giày/25 biến thể và 5 mẫu mỗi nhóm thuộc tính; không tự thêm mẫu khi chỉ nâng cấu trúc. Cài mới `f6s_shop.sql` đã có 55 giày gồm 50 nguồn + 5 demo. [Hướng dẫn chọn file và cảnh báo đầy đủ](HUONG_DAN_GOI_SQL.md). Các số lượng 50 sản phẩm bên dưới mô tả bộ nguồn cũ.

Bản chuyển luồng bán hàng bổ sung `order_draft.customer_id` và `invoice.customer_id`, tách khách mua với nhân viên tạo. Đã gộp trong cài mới/nâng cấp/bổ sung; nếu đã nâng bản trước, chạy riêng `cap_nhat_luong_ban_hang_20261023.sql` sau sao lưu và dừng web. Không tự gán khách cho hóa đơn cũ. [Luồng mới và giới hạn](../docs/14-CHUYEN-LUONG-NGHIEP-VU-BEE.md).

Các bản cài mới và bổ sung dữ liệu đã gộp cấu trúc module giày ngày 23/10/2026. Không lấy database đang chạy của người dùng để tạo dump: dữ liệu mẫu cũ được giữ, không kèm đơn/tài khoản kiểm thử hoặc secret mới.

| Tình huống | File |
|---|---|
| Cài mới, chấp nhận tạo lại schema `f6s_shop` | [`../f6s_shop.sql`](../f6s_shop.sql) — **có DROP DATABASE**, chỉ cần import một file |
| Đã có dữ liệu, nâng cấu trúc và chuẩn hóa mã | [`nang_cap_f6s.sql`](nang_cap_f6s.sql) — dừng web, chọn đúng schema trước khi import; đổi SKU hiện hành và giữ mã cũ, không xóa dữ liệu |
| Cần thêm bộ dữ liệu sản phẩm/bài viết nguồn | [`bo_sung_du_lieu.sql`](bo_sung_du_lieu.sql) — có seed, không phải lựa chọn mặc định khi chỉ nâng version; đã gộp cấu trúc mới |

## Bản đầy đủ hiện có

Đợt mã sản phẩm mới đã gộp: `product.code` (unique), `product_detail.legacy_sku`, chuẩn hóa SKU từ ID và unique SKU. **Dừng web, sao lưu và chạy migration trước khi chạy code mới**. Theo quyết định đã duyệt, bước `gop_bien_the_244_245.sql` chuyển tồn 245 sang 244 một lần, ngừng bán 245 và giữ lịch sử. Nếu còn giỏ/hóa đơn chờ tham chiếu 245 hoặc dữ liệu khác dự kiến, script bỏ qua để kiểm tra; không tự xóa tham chiếu. Bảng audit `catalog_variant_merge` phải có trước khi chạy code mới. [Chi tiết model/mã](../docs/13-MODEL-CATALOG-VA-MA-DINH-DANH.md).

- Giữ 50 sản phẩm, 13 bài viết, 1 admin và 2 nhân viên nguồn; giữ bảng tiếng Việt để bảo toàn nguồn, không đồng bộ tự động với bảng Java.
- Bảng `pending_registration` cho đăng ký chờ OTP.
- Bảng `shoe_attribute` cho thương hiệu / đế giày / chất liệu; 3 cột `product.brand_id`, `sole_id`, `material_id`.
- Không tự đoán thương hiệu/chất liệu/đế giày: dữ liệu cũ để NULL, quản trị viên phân loại sau.
- Admin demo giữ `admin@gmail.com` / `123123` trong bản cài mới; bản nâng cấp không đặt lại mật khẩu. Không dùng mật khẩu demo khi public.

## Script lịch sử và bảo trì

`cap_nhat_noi_dung_20261019.sql`, `cap_nhat_xac_thuc_20261019.sql`, `cap_nhat_module_giay_20261023.sql` được giữ để đối chiếu hoặc nâng từng phần. Cài mới không phải chạy thêm. `dat_lai_admin_demo.sql` chỉ dùng khi chủ động muốn reset admin demo, không chạy như bước nâng cấp.

Sao lưu trước khi import. Không chạy dump đầy đủ lên database có đơn hàng cần giữ. Các lệnh thêm cấu trúc có thể chạy lại nhưng DDL MySQL không rollback nguyên khối: nếu lỗi hãy đọc thông báo rồi chạy lại sau khi sửa, không xóa database để thử.

## Kiểm chứng

Đã import bản đầy đủ vào schema MySQL 8 demo riêng, chạy bản bổ sung và nâng cấp hai lần; số sản phẩm/bài viết/tài khoản không tăng, có đủ bảng/cột mới. Chưa kiểm chứng trực tiếp XAMPP/MariaDB trên Windows.

Đã nâng cấp riêng một schema được import từ SQL cũ (commit `61b5371`): từ 0 lên đủ 3 cột thuộc tính, giữ 50 sản phẩm / 13 bài viết / 3 tài khoản. Web khởi động trên bản cài mới bằng `ddl-auto=validate` (không tự tạo cột), đăng nhập admin và mở 9 trang quản trị đạt; kiểm tra đóng/mở menu desktop/điện thoại đạt. Tiến trình kiểm thử được tách file bot để không khóa file của web đang chạy.

Lệnh seed `product` đã ghi rõ danh sách cột để chạy được cả trước và sau khi thêm 3 cột mới, thay vì `INSERT ... VALUES` phụ thuộc thứ tự/toàn bộ cột.
