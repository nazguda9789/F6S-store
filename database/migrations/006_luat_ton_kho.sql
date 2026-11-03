IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
-- SQL Server. Chạy sau migration 005 trên database đã được sao lưu.
-- Chỉ bù cờ một lần; chạy lại không thay đổi cờ của các đơn theo luật mới.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH('dbo.orders','da_tru_kho') IS NULL
    BEGIN
        ALTER TABLE dbo.orders ADD da_tru_kho BIT NOT NULL
            CONSTRAINT DF_orders_da_tru_kho DEFAULT 0 WITH VALUES;
        EXEC sp_executesql N'UPDATE dbo.orders SET da_tru_kho=CASE WHEN status=''CANCELLED'' THEN 0 ELSE 1 END;';
    END;
    -- Lý do trừ/hoàn kho phải giữ nguyên tiếng Việt khi ghi lịch sử.
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.inventory_logs') AND name='reason' AND TYPE_NAME(user_type_id)<>'nvarchar')
        ALTER TABLE dbo.inventory_logs ALTER COLUMN reason NVARCHAR(500) NOT NULL;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
