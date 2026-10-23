# Catalog giày thể thao và bộ dữ liệu mẫu — 24/10/2026

## Phạm vi

Chuẩn hóa catalog đang dùng bởi Java, nâng cấp giữ dữ liệu và cung cấp bộ demo có liên kết thật. Không viết lại các bảng hóa đơn/thanh toán hoặc xác nhận mọi chức năng Bee đã xong. Các bảng tiếng Việt nguồn cũ vẫn được giữ để bảo toàn dữ liệu; ứng dụng Java sử dụng các bảng English dưới đây, không tự đồng bộ hai bộ bảng.

| Bảng | Vai trò / liên kết | Bộ mẫu mới |
|---|---|---|
| shoe_attribute | kind BRAND/SOLE/MATERIAL; mã unique, tên unique trong nhóm, mô tả, trạng thái, ngày tạo/cập nhật | 5 thương hiệu + 5 đế + 5 chất liệu |
| category | Danh mục giày, đường dẫn, cha/con, thứ tự, metadata | 5 nhóm mục đích sử dụng |
| size | Kích cỡ có thể tái sử dụng ở nhiều biến thể | EU 38–42, 5 giá trị |
| color | Tên và mã HEX #RRGGBB | 5 tên màu |
| product | Mẫu giày, mã F6S-SP, danh mục/thuộc tính, giá, cân nặng chung | 5 giày [DEMO] |
| product_detail | Biến thể theo sản phẩm + màu + size; mã F6S-SKU và tồn | 25 dòng, 5 size mỗi giày |
| product_category | Liên kết nhiều–nhiều giày/danh mục | 5 liên kết |
| product_color_image | Ảnh đại diện theo màu của từng giày | 5 liên kết |
| product_image | Ảnh chi tiết của giày | 5 liên kết |
| catalog_demo_seed | Ghi nhận danh tính 5 mẫu, liên kết FK tới product; không phải bảng tồn kho | 5 khóa seed |

Không cố tạo 5 dòng ở mọi bảng nghiệp vụ. Không thêm tài khoản admin, hóa đơn hoặc thanh toán giả. Cài mới có tổng 55 sản phẩm / 201 biến thể vì còn giữ 50 sản phẩm / 176 biến thể nguồn; không tự đổi dữ liệu S/M/L cũ thành số EU làm sai lịch sử.

## Danh sách mẫu

- Thương hiệu: F6S, Nike, Adidas, Puma, New Balance.
- Đế: Cao su bám đường, EVA, Phylon, TPU, Cao su non.
- Chất liệu: Lưới thoáng khí, Da tổng hợp, Vải knit, Vải canvas, Da lộn.
- Nhóm giày: chạy bộ, tập luyện, đi bộ, bóng rổ, lifestyle.
- Màu: Đen, Trắng, Xám, Xanh navy, Đỏ. Tái sử dụng tên tương ứng đã có, không ghi đè mã màu hoặc kích hoạt lại dòng bị ngừng dùng.
- Năm tên giày đều bắt đầu [DEMO], ảnh SVG tự tạo có nhãn minh họa. Giá/brand/tồn chỉ minh họa kiểm thử, không phải thông số thương mại được hãng xác nhận. Thay bằng hàng thật hoặc ngừng bán trước khi public.

## Ràng buộc và tương thích

Mã thuộc tính F6S-TH / F6S-DE / F6S-CL cấp theo ID, không đổi khi đổi tên. Metadata thời gian của thuộc tính cũ được khởi tạo tại thời điểm migration (không suy đoán ngày tạo gốc). Thuộc tính đang tắt chỉ được giữ trên giày cũ, không gán mới. Không xóa cứng thuộc tính đã dùng.

Thêm FK product.brand_id/sole_id/material_id → shoe_attribute.id khi không có tham chiếu mồ côi; nếu có, báo số lượng và bỏ qua FK đó để quản trị viên xử lý, không tự xóa giá trị. Kind của FK được kiểm tra trong service vì FK tới bảng chung không tự phân biệt BRAND/SOLE/MATERIAL.

Không thêm unique cứng tổ hợp màu/size trên dữ liệu nguồn vì còn dòng 245 đã lưu trữ phục vụ lịch sử. Service chặn trùng tổ hợp mới; migration gộp 245→244 đã duyệt vẫn được giữ có điều kiện/audit. `product_detail.color_id/size_id` và cột quan hệ cũ `color/size` được seed đồng bộ.

Tiền hiện vẫn dùng model Double của toàn bộ hệ thống cũ, không tự đổi một cột sang DECIMAL trong khi Java/checkout/báo cáo chưa chuyển đồng bộ. Cân nặng hiện dùng chung theo sản phẩm. Đây chưa phải chứng nhận tài chính/kho production.

## Trang được sửa

- Thuộc tính: tab nhóm, tìm theo tên/mã, lọc trạng thái và phân trang database; thêm/sửa mô tả, mã đọc-only, ngày cập nhật; trạng thái không xóa lịch sử.
- Thêm/sửa giày: kiểm tra tên/đường dẫn, giá hữu hạn không âm, cân nặng >0 ở server và input số ở form; size số được ưu tiên trước nhãn legacy. Bảng tổ hợp, SKU server và quản lý ảnh theo màu vẫn được giữ.
- Màu: chuẩn hóa tên, kiểm tra HEX ở form/server; kích cỡ trim và giới hạn độ dài.
- Danh mục: tên/đường dẫn/thứ tự hợp lệ, chặn tự làm cha, chỉ chọn nhóm gốc đang dùng; giữ ảnh/ngày tạo khi sửa; chặn xóa dây chuyền nhóm còn mục con. Không phân loại lại hàng cũ tự động.

## Cập nhật

Đọc [hướng dẫn import](../sql/HUONG_DAN_GOI_SQL.md). Dừng web, sao lưu, chạy `sql/nang_cap_f6s.sql` trước code mới. Muốn thêm mẫu thì chạy `sql/du_lieu_mau_catalog_20261024.sql`. Cài mới dùng duy nhất `f6s_shop.sql` có DROP DATABASE. Không cần secret mới; gói SQL cũ 23/10 chưa có metadata mới, không dùng thay bản hiện tại.

Seed chỉ khởi tạo liên kết/biến thể cho mẫu chưa đăng ký trong catalog_demo_seed. Nhập lại không đặt lại giá/tồn, không tạo lại biến thể đã chủ động xóa hoặc sản phẩm đã đổi slug. Nếu chủ động xóa hẳn mẫu, registry cascade theo sản phẩm và lần seed sau có thể tạo lại mẫu đó. Không xóa registry để ép chạy; không nhập đồng thời hoặc khi web xử lý đơn.

## Kiểm chứng bản này

- 86 test Java được chọn (catalog/thuộc tính/danh mục/tài khoản/luồng đơn) và 12 test JavaScript đều đạt; không đồng nghĩa đã kiểm thử toàn bộ website.

- MySQL 8.4, database thử riêng: import mới có 55 sản phẩm/201 biến thể; 5 sản phẩm mới có tổng 25 biến thể. Nâng cấp từ bản trước giữ 50 sản phẩm/176 biến thể trước khi thêm demo.
- Nhập lại migration/seed không nhân bản mẫu; đã thử giữ tồn kho chỉnh sửa và slug đổi. Khóa ngoại có tham chiếu mồ côi được báo và bỏ qua, dữ liệu cũ không bị xóa.
- Kiểm thử trình duyệt: tạo/sửa thuộc tính, lọc thương hiệu, chặn tham chiếu ngừng dùng, form tạo tổ hợp màu/size, SKU server, dữ liệu mẫu và ảnh minh họa.
- Chạy bằng schema validation, không dựa vào Hibernate tự sửa bảng. Chưa xác nhận trên mọi phiên bản MariaDB/XAMPP.

[Tải bộ SQL 24/10](https://github.com/nazguda9789/F6S-store/blob/main/downloads/F6S-SQL-20261024.zip) → Download raw.
