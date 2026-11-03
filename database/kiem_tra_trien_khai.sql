IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép kiểm tra triển khai.',1;
SET NOCOUNT ON;
SELECT DB_NAME() database_name,
    OBJECT_ID(N'dbo.product_images',N'U') product_images_object,
    OBJECT_ID(N'dbo.chat_lieu',N'U') chat_lieu_object,
    COL_LENGTH(N'dbo.product_variants',N'chat_lieu_id') material_column_bytes,
    COL_LENGTH(N'dbo.order_details',N'ten_chat_lieu') material_snapshot_bytes,
    COL_LENGTH(N'dbo.orders',N'da_tru_kho') stock_flag_bytes;
SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE DATA_TYPE IN(N'varchar',N'char',N'text',N'nvarchar',N'nchar',N'ntext')
ORDER BY TABLE_NAME,ORDINAL_POSITION;
CREATE TABLE #deployment_counts(table_name NVARCHAR(128),row_count BIGINT);
DECLARE @table SYSNAME,@query NVARCHAR(MAX);
DECLARE tables_to_count CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo') ORDER BY name;
OPEN tables_to_count; FETCH NEXT FROM tables_to_count INTO @table;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @query=N'INSERT #deployment_counts SELECT N'''+REPLACE(@table,N'''',N'''''')+N''',COUNT_BIG(*) FROM dbo.'+QUOTENAME(@table)+N';';
    EXEC sp_executesql @query;
    FETCH NEXT FROM tables_to_count INTO @table;
END;
CLOSE tables_to_count; DEALLOCATE tables_to_count;
SELECT * FROM #deployment_counts ORDER BY table_name;
DROP TABLE #deployment_counts;
