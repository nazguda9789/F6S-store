/* Build reviewed T-SQL from Hibernate's SQL Server DDL and the same JSON used by Java. */
const fs = require('node:fs'),
  path = require('node:path'),
  assert = require('node:assert/strict')
const root = path.resolve(__dirname, '..')
const data = JSON.parse(
  fs.readFileSync(path.join(root, 'backend/src/main/resources/catalog/real-products.json'), 'utf8'),
)
const ddl = fs.readFileSync(path.join(root, 'backend/target/sqlserver-schema.sql'), 'utf8').trim()
assert.equal((ddl.match(/create table dbo\./g) || []).length, 19)
assert.doesNotMatch(ddl, /\b(?:drop|delete|truncate)\b/i)
assert.equal(data.products.length, 25)
assert.equal(new Set(data.products.map((p) => p.code)).size, 25)
const n = (value) => "N'" + String(value).replaceAll("'", "''") + "'"
const description = (p) =>
  `${p.name} — model của ${p.brand}. Nhóm sử dụng trong catalog F6S: ${p.category}. ${data.notice} Mã F6S/SKU là mã nội bộ, không phải mã phối màu của hãng. Website hãng để tham khảo: ${p.brandUrl}`
const variants = data.products.flatMap((p) =>
  data.colors.flatMap((c) =>
    data.sizes.map((size) => ({
      productCode: p.code,
      sku: `SKU-RC${p.code.slice(7)}-${c.code}-${size.replace('.', 'P')}`,
      color: c.name,
      size,
      price: p.samplePrice,
      stock: c.code === 'BLK' && size === '42' ? 0 : 10,
    })),
  ),
)
assert.equal(variants.length, 250)
assert.equal(new Set(variants.map((v) => v.sku)).size, 250)
const schema = `-- F6S SQL SERVER: only run against an EMPTY f6s_shop database.
-- Exported from current JPA entities with Hibernate SQLServerDialect.
-- No database/table is dropped; existing tables cause a stop.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped=0)
  THROW 51001, N'Database already contains tables. Skip 01 if backend created the schema; use 02 only after reviewing compatibility.', 1;
-- BEGIN HIBERNATE SQL SERVER DDL
${ddl}
-- END HIBERNATE SQL SERVER DDL
 COMMIT TRANSACTION;
 PRINT N'Created 19 application tables. Next run 02_real_products.sql.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
`
const rows = data.products
  .map(
    (p) =>
      ` (${[n(p.code), n(p.name), n(p.brand), n(p.category), p.samplePrice, p.featured ? 1 : 0, n(data.imageUrl), n(description(p))].join(',')})`,
  )
  .join(',\n')
const variantRows = variants
  .map(
    (v) => ` (${[n(v.productCode), n(v.sku), n(v.color), n(v.size), v.price, v.stock].join(',')})`,
  )
  .join(',\n')
const seed = `-- F6S REFERENCE CATALOG ${data.release}
-- Real model names; prices, colors, sizes and stock are DEMO shop configuration.
-- No verified product photos: image URLs point to an explicit photo-pending placeholder.
-- Stop the backend and backup first. Never reset a production database to import this file.
USE f6s_shop;
GO
SET XACT_ABORT ON;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.categories',N'U') IS NULL OR OBJECT_ID(N'dbo.products',N'U') IS NULL OR OBJECT_ID(N'dbo.product_variants',N'U') IS NULL
  THROW 51002, N'Missing catalog tables. On an empty database run 01_schema.sql first.', 1;
 IF COL_LENGTH(N'dbo.product_variants',N'price') IS NULL OR COL_LENGTH(N'dbo.product_variants',N'shoe_size') IS NULL OR COL_LENGTH(N'dbo.products',N'hover_image_url') IS NULL
  THROW 51003, N'Incompatible schema: this is for the Vue/SQL Server backend, not legacy MySQL.', 1;
 IF OBJECT_ID(N'dbo.catalog_import_history',N'U') IS NULL
  CREATE TABLE dbo.catalog_import_history(
   release_key nvarchar(64) NOT NULL PRIMARY KEY,
   applied_at datetimeoffset(6) NOT NULL,
   product_count int NOT NULL,variant_count int NOT NULL,
   note nvarchar(1000) NOT NULL);
 IF EXISTS(SELECT 1 FROM dbo.catalog_import_history WHERE release_key=${n(data.release)})
 BEGIN
  PRINT N'Already imported. Existing names, prices, stocks and visibility were not reset.';
 END
 ELSE
 BEGIN
  DECLARE @Products TABLE(code nvarchar(40) PRIMARY KEY,name nvarchar(180),brand nvarchar(100),category nvarchar(100),price numeric(19,0),featured bit,image_url nvarchar(1000),description nvarchar(4000));
  INSERT INTO @Products(code,name,brand,category,price,featured,image_url,description) VALUES
${rows};
  DECLARE @Variants TABLE(product_code nvarchar(40),sku nvarchar(60) PRIMARY KEY,color nvarchar(60),shoe_size nvarchar(10),price numeric(19,0),stock int);
  INSERT INTO @Variants(product_code,sku,color,shoe_size,price,stock) VALUES
${variantRows};
  IF EXISTS(SELECT 1 FROM dbo.products p JOIN @Products c ON c.code=p.code WHERE p.name<>c.name OR p.brand<>c.brand)
   THROW 51004, N'Internal catalog code belongs to a different product. No data was changed.', 1;
  IF EXISTS(SELECT 1 FROM dbo.categories c JOIN (SELECT DISTINCT category FROM @Products) s ON s.category=c.name WHERE c.active=0)
   THROW 51005, N'A required category is inactive. Review it before importing; this script does not reactivate user data.', 1;
  INSERT INTO dbo.categories(name,active)
   SELECT DISTINCT p.category,1 FROM @Products p WHERE NOT EXISTS(SELECT 1 FROM dbo.categories c WHERE c.name=p.category);
  DECLARE @Inserted TABLE(id bigint PRIMARY KEY,code nvarchar(40));
  INSERT INTO dbo.products(code,name,brand,price,description,image_url,hover_image_url,active,featured,category_id)
   OUTPUT inserted.id,inserted.code INTO @Inserted(id,code)
   SELECT p.code,p.name,p.brand,p.price,p.description,p.image_url,p.image_url,1,p.featured,c.id
   FROM @Products p JOIN dbo.categories c ON c.name=p.category
   WHERE NOT EXISTS(SELECT 1 FROM dbo.products existing WHERE existing.code=p.code);
  INSERT INTO dbo.product_variants(product_id,sku,color,shoe_size,price,stock,active)
   SELECT p.id,v.sku,v.color,v.shoe_size,v.price,v.stock,1 FROM @Inserted p JOIN @Variants v ON v.product_code=p.code;
  -- Existing catalog products are never restocked/repriced. A partial manual import must be reviewed.
  IF EXISTS(SELECT 1 FROM @Products c JOIN dbo.products p ON p.code=c.code WHERE NOT EXISTS(SELECT 1 FROM dbo.product_variants v WHERE v.product_id=p.id))
   THROW 51006, N'An existing catalog product has no variants. Import stopped without guessing inventory.', 1;
  INSERT INTO dbo.catalog_import_history(release_key,applied_at,product_count,variant_count,note)
   VALUES(${n(data.release)},SYSUTCDATETIME(),25,250,${n(data.notice)});
  PRINT N'Imported reference catalog. Existing orders, POS drafts, users and payment data were not modified.';
 END;
 COMMIT TRANSACTION;
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 THROW;
END CATCH;
SELECT p.code,p.name,p.brand,c.name AS category,p.price AS demo_price,COUNT(v.id) AS variant_count,SUM(v.stock) AS demo_stock
FROM dbo.products p JOIN dbo.categories c ON c.id=p.category_id LEFT JOIN dbo.product_variants v ON v.product_id=p.id
WHERE p.code LIKE N'F6S-RC-%'
GROUP BY p.code,p.name,p.brand,c.name,p.price ORDER BY p.code;
`
fs.writeFileSync(path.join(root, 'database/01_schema.sql'), schema)
fs.writeFileSync(path.join(root, 'database/02_real_products.sql'), seed)
const csv = (v) => '"' + String(v).replaceAll('"', '""') + '"'
fs.writeFileSync(
  path.join(root, 'database/catalog-products.csv'),
  '\ufeff' +
    [
      'code,name,brand,category,demo_price,photo_status,brand_reference',
      ...data.products.map((p) =>
        [p.code, p.name, p.brand, p.category, p.samplePrice, 'PENDING_VERIFICATION', p.brandUrl]
          .map(csv)
          .join(','),
      ),
    ].join('\n') +
    '\n',
)
console.log(
  `Built SQL Server schema + ${data.products.length} real-model references / ${variants.length} DEMO variants. Photos remain pending.`,
)
