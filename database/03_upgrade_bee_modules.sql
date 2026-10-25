-- Back up f6s_shop and stop the backend first. No DROP, DELETE or stock reset.
-- Existing Vue/SQL Server installations only. Empty database: use 01 then 02 instead.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.products',N'U') IS NULL OR OBJECT_ID(N'dbo.pos_drafts',N'U') IS NULL
  THROW 51020, N'Missing core schema. Use 01_schema.sql on an empty database.',1;
IF OBJECT_ID(N'dbo.customer_addresses', N'U') IS NULL
 create table dbo.customer_addresses (id bigint identity not null, active bit not null, default_address bit not null, detail nvarchar(250) not null, district nvarchar(100) not null, phone nvarchar(15) not null, province nvarchar(100) not null, recipient_name nvarchar(120) not null, ward nvarchar(100) not null, customer_id bigint not null, primary key (id));
IF OBJECT_ID(N'dbo.customers', N'U') IS NULL
 create table dbo.customers (id bigint identity not null, active bit not null, birth_date date, code nvarchar(30) not null, created_at datetimeoffset(6) not null, email nvarchar(254), full_name nvarchar(120) not null, gender nvarchar(10), phone nvarchar(15) not null, primary key (id));
IF OBJECT_ID(N'dbo.media_assets', N'U') IS NULL
 create table dbo.media_assets (id nvarchar(36) not null, active bit not null, byte_size bigint not null, content_type nvarchar(20) not null, created_at datetimeoffset(6) not null, height int not null, original_name nvarchar(120) not null, sha256 nvarchar(64) not null, stored_name nvarchar(80) not null, uploaded_by nvarchar(100) not null, width int not null, primary key (id));
IF OBJECT_ID(N'dbo.order_events', N'U') IS NULL
 create table dbo.order_events (id bigint identity not null, actor nvarchar(100) not null, created_at datetimeoffset(6) not null, from_status nvarchar(30), note nvarchar(500) not null, to_status nvarchar(30) not null, order_id bigint not null, primary key (id));
IF OBJECT_ID(N'dbo.product_color_images', N'U') IS NULL
 create table dbo.product_color_images (id bigint identity not null, color nvarchar(60) not null, display_order int not null, media_id nvarchar(36) not null, product_id bigint not null, primary key (id));
IF OBJECT_ID(N'dbo.shoe_attributes', N'U') IS NULL
 create table dbo.shoe_attributes (id bigint identity not null, active bit not null, code nvarchar(50) not null, name nvarchar(100) not null, attribute_type nvarchar(15) not null, primary key (id));
IF OBJECT_ID(N'dbo.staff_accounts', N'U') IS NULL
 create table dbo.staff_accounts (id bigint identity not null, active bit not null, address nvarchar(500), birth_date date, code nvarchar(30) not null, created_at datetimeoffset(6) not null, email nvarchar(254) not null, full_name nvarchar(120) not null, gender nvarchar(10), password_hash nvarchar(100) not null, phone nvarchar(15) not null, username nvarchar(50) not null, primary key (id));
IF COL_LENGTH(N'dbo.products',N'brand_attribute_id') IS NULL
 ALTER TABLE dbo.products ADD brand_attribute_id bigint NULL;
IF COL_LENGTH(N'dbo.products',N'sole_attribute_id') IS NULL
 ALTER TABLE dbo.products ADD sole_attribute_id bigint NULL;
IF COL_LENGTH(N'dbo.products',N'material_attribute_id') IS NULL
 ALTER TABLE dbo.products ADD material_attribute_id bigint NULL;
IF COL_LENGTH(N'dbo.products',N'created_at') IS NULL
 ALTER TABLE dbo.products ADD created_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.products',N'updated_at') IS NULL
 ALTER TABLE dbo.products ADD updated_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.products',N'created_by') IS NULL
 ALTER TABLE dbo.products ADD created_by nvarchar(100) NULL;
IF COL_LENGTH(N'dbo.products',N'updated_by') IS NULL
 ALTER TABLE dbo.products ADD updated_by nvarchar(100) NULL;
IF COL_LENGTH(N'dbo.product_variants',N'weight_grams') IS NULL
 ALTER TABLE dbo.product_variants ADD weight_grams int NULL;
IF COL_LENGTH(N'dbo.orders',N'customer_id') IS NULL
 ALTER TABLE dbo.orders ADD customer_id bigint NULL;
IF COL_LENGTH(N'dbo.pos_drafts',N'customer_id') IS NULL
 ALTER TABLE dbo.pos_drafts ADD customer_id bigint NULL;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKmkwx1x9mthieapj92cpxq5msc' AND parent_object_id=OBJECT_ID(N'dbo.customers'))
 alter table dbo.customers add constraint UKmkwx1x9mthieapj92cpxq5msc unique (code);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKm3iom37efaxd5eucmxjqqcbe9' AND parent_object_id=OBJECT_ID(N'dbo.customers'))
 alter table dbo.customers add constraint UKm3iom37efaxd5eucmxjqqcbe9 unique (phone);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKabuiu4nkcx6ryvndd987bwdap' AND parent_object_id=OBJECT_ID(N'dbo.media_assets'))
 alter table dbo.media_assets add constraint UKabuiu4nkcx6ryvndd987bwdap unique (sha256);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UK9cw7nxj7rfyurrj8jkcrult5p' AND parent_object_id=OBJECT_ID(N'dbo.product_color_images'))
 alter table dbo.product_color_images add constraint UK9cw7nxj7rfyurrj8jkcrult5p unique (product_id, color, display_order);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKg96g129re2nisac07t1l2g62j' AND parent_object_id=OBJECT_ID(N'dbo.shoe_attributes'))
 alter table dbo.shoe_attributes add constraint UKg96g129re2nisac07t1l2g62j unique (attribute_type, name);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKe29e05xxioqol9uem3d70ayfy' AND parent_object_id=OBJECT_ID(N'dbo.shoe_attributes'))
 alter table dbo.shoe_attributes add constraint UKe29e05xxioqol9uem3d70ayfy unique (code);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UKdrcmypuiyfciyuna48awq8f7g' AND parent_object_id=OBJECT_ID(N'dbo.staff_accounts'))
 alter table dbo.staff_accounts add constraint UKdrcmypuiyfciyuna48awq8f7g unique (code);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UK7fh415uxfcvf7hrv64hf8laq4' AND parent_object_id=OBJECT_ID(N'dbo.staff_accounts'))
 alter table dbo.staff_accounts add constraint UK7fh415uxfcvf7hrv64hf8laq4 unique (email);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'UK6fb7onep8w4asmh3x7a8dufko' AND parent_object_id=OBJECT_ID(N'dbo.staff_accounts'))
 alter table dbo.staff_accounts add constraint UK6fb7onep8w4asmh3x7a8dufko unique (username);
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKrvr6wl9gll7u98cda18smugp4' AND parent_object_id=OBJECT_ID(N'dbo.customer_addresses'))
 alter table dbo.customer_addresses add constraint FKrvr6wl9gll7u98cda18smugp4 foreign key (customer_id) references dbo.customers;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKjkqdh6opohx4rx9719gyn31aa' AND parent_object_id=OBJECT_ID(N'dbo.order_events'))
 alter table dbo.order_events add constraint FKjkqdh6opohx4rx9719gyn31aa foreign key (order_id) references dbo.orders;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKpxtb8awmi0dk6smoh2vp1litg' AND parent_object_id=OBJECT_ID(N'dbo.orders'))
 alter table dbo.orders add constraint FKpxtb8awmi0dk6smoh2vp1litg foreign key (customer_id) references dbo.customers;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKjmmcvvhmk40exbrkxunh0ckud' AND parent_object_id=OBJECT_ID(N'dbo.pos_drafts'))
 alter table dbo.pos_drafts add constraint FKjmmcvvhmk40exbrkxunh0ckud foreign key (customer_id) references dbo.customers;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKbf7q36wlbhh3dhh2fbl7qrme4' AND parent_object_id=OBJECT_ID(N'dbo.product_color_images'))
 alter table dbo.product_color_images add constraint FKbf7q36wlbhh3dhh2fbl7qrme4 foreign key (media_id) references dbo.media_assets;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FK89befdlg42rvdht0jnon98wks' AND parent_object_id=OBJECT_ID(N'dbo.product_color_images'))
 alter table dbo.product_color_images add constraint FK89befdlg42rvdht0jnon98wks foreign key (product_id) references dbo.products;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKqio1svd4eqb8dp9owepvlkgrn' AND parent_object_id=OBJECT_ID(N'dbo.products'))
 alter table dbo.products add constraint FKqio1svd4eqb8dp9owepvlkgrn foreign key (brand_attribute_id) references dbo.shoe_attributes;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKlfj0iy3v2x8fvgrlthe908pjh' AND parent_object_id=OBJECT_ID(N'dbo.products'))
 alter table dbo.products add constraint FKlfj0iy3v2x8fvgrlthe908pjh foreign key (material_attribute_id) references dbo.shoe_attributes;
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name=N'FKe533eebgoetf4qi4c8cw65v62' AND parent_object_id=OBJECT_ID(N'dbo.products'))
 alter table dbo.products add constraint FKe533eebgoetf4qi4c8cw65v62 foreign key (sole_attribute_id) references dbo.shoe_attributes;
 COMMIT TRANSACTION;
 PRINT N'Bee modules upgraded. Historical events remain unknown; no history was fabricated.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
