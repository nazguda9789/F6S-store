IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.product_images',N'U') IS NULL
 BEGIN
  CREATE TABLE dbo.product_images (
   id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
   product_id INT NOT NULL,
   duong_dan NVARCHAR(2048) NOT NULL,
   thu_tu INT NOT NULL CHECK(thu_tu>=0),
   la_anh_chinh BIT NOT NULL DEFAULT 0,
   CONSTRAINT FK_product_images_product FOREIGN KEY(product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
  );
  CREATE INDEX IX_product_images_order ON dbo.product_images(product_id,thu_tu,id);
  CREATE UNIQUE INDEX UX_product_images_main ON dbo.product_images(product_id) WHERE la_anh_chinh=1;
 END;
 INSERT INTO dbo.product_images(product_id,duong_dan,thu_tu,la_anh_chinh)
 SELECT p.id,p.image_url,0,1 FROM dbo.products p
 WHERE p.image_url IS NOT NULL AND LTRIM(RTRIM(p.image_url))<>N''
 AND NOT EXISTS(SELECT 1 FROM dbo.product_images i WHERE i.product_id=p.id);
 COMMIT TRANSACTION;
 SELECT N'COMMIT' ket_qua,DB_NAME() database_name,COUNT_BIG(*) image_count FROM dbo.product_images;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
