IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
-- SQL Server. Chạy sau 006, sau khi sao lưu database.
-- Chuyển các cột văn bản hiển thị sang NVARCHAR, giữ độ dài và NULL/NOT NULL.
-- Không thể khôi phục dấu đã mất thành '?' trước khi áp dụng migration.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @table SYSNAME, @column SYSNAME, @length INT, @nullable BIT, @statement NVARCHAR(MAX);
    DECLARE text_columns CURSOR LOCAL FAST_FORWARD FOR
        SELECT t.name,c.name,c.max_length,c.is_nullable
        FROM sys.tables t
        JOIN sys.columns c ON c.object_id=t.object_id
        JOIN sys.types ty ON ty.user_type_id=c.user_type_id
        JOIN (VALUES
            (N'products',N'name'),(N'products',N'description'),
            (N'customers',N'full_name'),(N'customers',N'address'),
            (N'orders',N'receiver_name'),(N'orders',N'shipping_address'),
            (N'order_details',N'product_name'),(N'order_details',N'size_name'),
            (N'invoices',N'customer_name'),(N'invoices',N'employee_name'),
            (N'employees',N'full_name'),(N'employees',N'address'),(N'employees',N'position'),
            (N'categories',N'name'),(N'promotions',N'name'),(N'vouchers',N'name'),
            (N'users',N'full_name'),(N'inventory_logs',N'reason'),
            (N'pos_held_orders',N'customer_label'),(N'pos_held_orders',N'note')
        ) wanted(table_name,column_name) ON wanted.table_name=t.name AND wanted.column_name=c.name
        WHERE t.schema_id=SCHEMA_ID(N'dbo') AND ty.name=N'varchar';
    OPEN text_columns;
    FETCH NEXT FROM text_columns INTO @table,@column,@length,@nullable;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @statement=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' ALTER COLUMN '+QUOTENAME(@column)
            +N' NVARCHAR('+CASE WHEN @length=-1 THEN N'MAX' ELSE CONVERT(NVARCHAR(10),@length) END+N') '
            +CASE WHEN @nullable=1 THEN N'NULL' ELSE N'NOT NULL' END+N';';
        EXEC sp_executesql @statement;
        FETCH NEXT FROM text_columns INTO @table,@column,@length,@nullable;
    END;
    CLOSE text_columns;
    DEALLOCATE text_columns;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
