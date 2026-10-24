# Chọn đúng bộ SQL

## Cài mới / sửa lỗi import phpMyAdmin

Chỉ dùng [install/](install/README.md): `01_schema.sql` → `02_catalog_admin.sql` → tùy chọn `03_demo_optional.sql`. Không CALL/routine, không DROP database. Tạo database mới `f6s_shop`; không đụng database nhập dở `f6s_shop_demo`.

## Database thật cần giữ

Không nhập bộ install lên dữ liệu thật. Backup đầy đủ, thử trên bản sao, dừng web; giữ datasource và secret OTP/VNPAY. Nếu có giao dịch ngân hàng đang chờ, đối soát xong trước khi chuyển catalog/dọn giỏ.

Các file dưới đây có routine, **dùng MySQL CLI**, không phải bộ phpMyAdmin:

| File | Khi dùng |
| --- | --- |
| `maintenance/01_upgrade_existing.sql` | Nâng schema các đợt cũ lên bản hiện hành; không reset tài khoản |
| `maintenance/02_replace_catalog.sql` | Chủ động thay toàn bộ catalog bằng bộ Sports; ngừng bán hàng cũ, lưu trữ bài/banner và dọn giỏ có bản dự phòng |
| `maintenance/03_restore_previous_catalog.sql` | Khôi phục cờ hiển thị và giỏ theo snapshot; không xóa đơn mới hoặc đặt lại tồn |

Ví dụ Command Prompt Windows, không dùng `--force`:

```bat
mysql -u root -p TEN_DATABASE_THAT < sql\maintenance\01_upgrade_existing.sql
```

Đọc [phạm vi catalog và rollback](../docs/07-CATALOG-VA-DU-LIEU.md) trước khi dùng file 02/03. Không cần thay catalog chỉ để chạy code; schema phải tương thích trước.

Nguồn tạo installer nằm ở `scripts/sql-templates/` và `scripts/build-*.cjs`, không phải các file cần import bằng tay. Các file all-in-one/seed cũ đã gỡ; Git history giữ bản cũ nếu cần đối chiếu.
