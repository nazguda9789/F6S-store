// Generate a non-destructive SQL Server upgrade from the tested entity DDL.
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const ddl = fs.readFileSync(path.join(root, 'backend/target/sqlserver-schema.sql'), 'utf8')
const added = new Set(['staff_accounts','customers','customer_addresses','shoe_attributes','media_assets','product_color_images','order_events'])
const statements = ddl.split(';').map(s => s.trim()).filter(Boolean)
const parts = []
for (const s of statements) {
  const m = s.match(/^create table dbo\.(\w+)/)
  if (m && added.has(m[1])) parts.push(`IF OBJECT_ID(N'dbo.${m[1]}', N'U') IS NULL\n ${s};`)
}
const columns = {
  products: {brand_attribute_id:'bigint',sole_attribute_id:'bigint',material_attribute_id:'bigint',created_at:'datetimeoffset(6)',updated_at:'datetimeoffset(6)',created_by:'nvarchar(100)',updated_by:'nvarchar(100)'},
  product_variants: {weight_grams:'int'}, orders:{customer_id:'bigint'}, pos_drafts:{customer_id:'bigint'},
}
for (const [table, fields] of Object.entries(columns)) for (const [name,type] of Object.entries(fields))
 parts.push(`IF COL_LENGTH(N'dbo.${table}',N'${name}') IS NULL\n ALTER TABLE dbo.${table} ADD ${name} ${type} NULL;`)
for (const s of statements) {
 const m=s.match(/^alter table dbo\.(\w+) add constraint (\w+) (.*)$/s)
 if (!m) continue
 const [,table,name,body]=m
 if (!added.has(table) && !/references dbo\.(customers|shoe_attributes)/.test(body)) continue
 parts.push(`IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'${name}' AND parent_object_id=OBJECT_ID(N'dbo.${table}'))\n ${s};`)
}
fs.writeFileSync(path.join(root,'database/03_upgrade_bee_modules.sql'),`-- Back up f6s_shop and stop the backend first. No DROP, DELETE or stock reset.
-- Existing Vue/SQL Server installations only. Empty database: use 01 then 02 instead.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.products',N'U') IS NULL OR OBJECT_ID(N'dbo.pos_drafts',N'U') IS NULL
  THROW 51020, N'Missing core schema. Use 01_schema.sql on an empty database.',1;
${parts.join('\n')}
 COMMIT TRANSACTION;
 PRINT N'Bee modules upgraded. Historical events remain unknown; no history was fabricated.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
`)
