IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM promotion_details GROUP BY promotion_id,product_id HAVING COUNT(*)>1)
    THROW 51008, 'Duplicate promotion/product links: resolve before Phase 8 migration.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_promotion_details_product_phase8' AND object_id=OBJECT_ID('promotion_details'))
    CREATE UNIQUE INDEX UX_promotion_details_product_phase8 ON promotion_details(promotion_id,product_id);
COMMIT TRANSACTION;
