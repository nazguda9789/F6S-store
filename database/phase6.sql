IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
-- Phase 6: additive/idempotent migration. Run against the existing shopbangiay database.
-- Does not drop tables or alter prices/stock/order totals.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM product_variants GROUP BY product_id,size_id HAVING COUNT(*)>1)
    THROW 51000, 'Duplicate product/size pairs must be resolved before Phase 6 migration.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_product_variants_product_size' AND object_id=OBJECT_ID('product_variants'))
    CREATE UNIQUE INDEX UX_product_variants_product_size ON product_variants(product_id,size_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_variant_price_positive')
    ALTER TABLE product_variants WITH CHECK ADD CONSTRAINT CK_variant_price_positive CHECK(price>0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_variant_quantity_nonnegative')
    ALTER TABLE product_variants WITH CHECK ADD CONSTRAINT CK_variant_quantity_nonnegative CHECK(quantity>=0);
IF OBJECT_ID('inventory_logs','U') IS NULL
BEGIN
    CREATE TABLE inventory_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        variant_id INT NOT NULL REFERENCES product_variants(id),
        quantity_change INT NOT NULL,
        quantity_before INT NOT NULL CHECK(quantity_before>=0),
        quantity_after INT NOT NULL CHECK(quantity_after>=0),
        reason NVARCHAR(500) NOT NULL,
        actor NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_inventory_log_delta CHECK(quantity_after=quantity_before+quantity_change)
    );
    CREATE INDEX IX_inventory_logs_variant_time ON inventory_logs(variant_id,created_at DESC,id DESC);
END;
IF COL_LENGTH('order_details','product_name') IS NULL
    ALTER TABLE order_details ADD product_name NVARCHAR(255) NULL;
IF COL_LENGTH('order_details','size_name') IS NULL
    ALTER TABLE order_details ADD size_name NVARCHAR(10) NULL;
-- Dynamic SQL allows the new snapshot columns to be used in the same migration batch.
EXEC sp_executesql N'UPDATE d SET product_name=COALESCE(d.product_name,p.name),size_name=COALESCE(d.size_name,s.size_value) FROM order_details d JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id WHERE d.product_name IS NULL OR d.size_name IS NULL';
COMMIT TRANSACTION;

