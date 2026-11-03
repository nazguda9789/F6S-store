IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
-- Chọn database đích bằng sqlcmd -d. Chỉ chạy sau khi đã sao lưu.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF NOT EXISTS(SELECT 1 FROM dbo.users u JOIN dbo.roles r ON r.id=u.role_id WHERE u.username=N'admin' AND r.name=N'ROLE_ADMIN' AND u.status=1 AND LEN(u.password)>=60)
  THROW 51000,N'Không có admin hoạt động với mật khẩu băm. Hủy dọn.',1;
 SELECT u.* INTO #protected_users FROM dbo.users u WHERE u.username=N'admin' OR EXISTS(SELECT 1 FROM dbo.employees e WHERE e.user_id=u.id);
 SELECT * INTO #employees FROM dbo.employees;
 SELECT * INTO #roles FROM dbo.roles;
 SELECT * INTO #categories FROM dbo.categories;
 SELECT * INTO #sizes FROM dbo.sizes;
 SELECT * INTO #promotions FROM dbo.promotions;
 SELECT id,user_id INTO #customer_users FROM dbo.customers;
 DECLARE @voucher_count BIGINT=(SELECT COUNT_BIG(*) FROM dbo.vouchers);
 DELETE FROM dbo.pos_held_items;
 DELETE FROM dbo.pos_held_orders;
 DELETE FROM dbo.invoices;
 DELETE FROM dbo.order_details;
 DELETE FROM dbo.orders;
 DELETE FROM dbo.inventory_logs;
 DELETE FROM dbo.promotion_details;
 IF OBJECT_ID(N'dbo.product_images',N'U') IS NOT NULL EXEC(N'DELETE FROM dbo.product_images');
 DELETE FROM dbo.product_variants;
 DELETE FROM dbo.products;
 -- Giữ voucher, bỏ FK khách đã xóa. Voucher riêng không thể dùng cho khách khác.
 UPDATE dbo.vouchers SET customer_id=NULL WHERE customer_id IS NOT NULL;
 DELETE FROM dbo.customers;
 DELETE u FROM dbo.users u JOIN dbo.roles r ON r.id=u.role_id WHERE r.name=N'ROLE_USER'
  AND EXISTS(SELECT 1 FROM #customer_users c WHERE c.user_id=u.id)
  AND NOT EXISTS(SELECT 1 FROM #protected_users p WHERE p.id=u.id);
 IF EXISTS(SELECT 1 FROM dbo.pos_held_items) OR EXISTS(SELECT 1 FROM dbo.pos_held_orders)
  OR EXISTS(SELECT 1 FROM dbo.invoices) OR EXISTS(SELECT 1 FROM dbo.order_details)
  OR EXISTS(SELECT 1 FROM dbo.orders) OR EXISTS(SELECT 1 FROM dbo.inventory_logs)
  OR EXISTS(SELECT 1 FROM dbo.promotion_details) OR EXISTS(SELECT 1 FROM dbo.product_variants)
  OR EXISTS(SELECT 1 FROM dbo.products) OR EXISTS(SELECT 1 FROM dbo.customers)
  THROW 51001,N'Dữ liệu mục tiêu chưa về 0. Hủy transaction.',1;
 IF EXISTS(SELECT * FROM #protected_users EXCEPT SELECT * FROM dbo.users)
  OR EXISTS(SELECT * FROM #employees EXCEPT SELECT * FROM dbo.employees)
  OR EXISTS(SELECT * FROM #roles EXCEPT SELECT * FROM dbo.roles)
  OR EXISTS(SELECT * FROM #categories EXCEPT SELECT * FROM dbo.categories)
  OR EXISTS(SELECT * FROM #sizes EXCEPT SELECT * FROM dbo.sizes)
  OR EXISTS(SELECT * FROM #promotions EXCEPT SELECT * FROM dbo.promotions)
  OR @voucher_count<>(SELECT COUNT_BIG(*) FROM dbo.vouchers)
  THROW 51002,N'Dữ liệu phải giữ đã thay đổi. Hủy transaction.',1;
 COMMIT TRANSACTION;
 SELECT N'COMMIT' AS ket_qua,DB_NAME() AS database_name;
 SELECT N'products' AS bang,COUNT_BIG(*) AS so_dong FROM dbo.products
 UNION ALL SELECT N'product_variants',COUNT_BIG(*) FROM dbo.product_variants
 UNION ALL SELECT N'customers',COUNT_BIG(*) FROM dbo.customers
 UNION ALL SELECT N'orders',COUNT_BIG(*) FROM dbo.orders
 UNION ALL SELECT N'order_details',COUNT_BIG(*) FROM dbo.order_details
 UNION ALL SELECT N'invoices',COUNT_BIG(*) FROM dbo.invoices
 UNION ALL SELECT N'inventory_logs',COUNT_BIG(*) FROM dbo.inventory_logs
 UNION ALL SELECT N'promotion_details',COUNT_BIG(*) FROM dbo.promotion_details
 UNION ALL SELECT N'pos_held_orders',COUNT_BIG(*) FROM dbo.pos_held_orders
 UNION ALL SELECT N'pos_held_items',COUNT_BIG(*) FROM dbo.pos_held_items;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
