-- Stop backend and back up f6s_shop first. Existing Vue/SQL Server only.
-- Run 03 first on old installations; new empty database uses latest 01 then 02.
-- No secret, OTP, fake payment or stock reset is inserted.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.order_events',N'U') IS NULL THROW 51030,N'Run 03_upgrade_bee_modules.sql first.',1;
IF OBJECT_ID(N'dbo.email_challenges',N'U') IS NULL
 create table dbo.email_challenges (id nvarchar(36) not null, attempts int not null, code_hash nvarchar(64) not null, consumed_at datetimeoffset(6), created_at datetimeoffset(6) not null, email nvarchar(254) not null, expires_at datetimeoffset(6) not null, grant_hash nvarchar(64), verified_at datetimeoffset(6), primary key (id));
IF OBJECT_ID(N'dbo.order_refunds',N'U') IS NULL
 create table dbo.order_refunds (id bigint identity not null, amount numeric(19,0) not null, created_at datetimeoffset(6) not null, reason nvarchar(500) not null, requested_by nvarchar(100) not null, resolved_at datetimeoffset(6), resolved_by nvarchar(100), restocked bit not null, settlement_reference nvarchar(120), status nvarchar(20) not null, order_id bigint not null, primary key (id));
IF OBJECT_ID(N'dbo.payment_attempts',N'U') IS NULL
 create table dbo.payment_attempts (id nvarchar(36) not null, amount numeric(19,0) not null, created_at datetimeoffset(6) not null, expires_at datetimeoffset(6) not null, provider_transaction nvarchar(40), settled_at datetimeoffset(6), status nvarchar(20) not null, order_id bigint not null, primary key (id));
IF OBJECT_ID(N'dbo.shop_accounts',N'U') IS NULL
 create table dbo.shop_accounts (id nvarchar(36) not null, active bit not null, address nvarchar(500), created_at datetimeoffset(6) not null, email nvarchar(254) not null, full_name nvarchar(120) not null, phone nvarchar(15), primary key (id));
IF OBJECT_ID(N'dbo.shop_sessions',N'U') IS NULL
 create table dbo.shop_sessions (token_hash nvarchar(64) not null, expires_at datetimeoffset(6) not null, account_id nvarchar(36) not null, primary key (token_hash));
IF COL_LENGTH(N'dbo.orders',N'payment_status') IS NULL
 ALTER TABLE dbo.orders ADD payment_status nvarchar(20) NULL;
IF COL_LENGTH(N'dbo.orders',N'shipping_fee') IS NULL
 ALTER TABLE dbo.orders ADD shipping_fee numeric(19,0) NULL;
IF COL_LENGTH(N'dbo.orders',N'carrier') IS NULL
 ALTER TABLE dbo.orders ADD carrier nvarchar(60) NULL;
IF COL_LENGTH(N'dbo.orders',N'tracking_code') IS NULL
 ALTER TABLE dbo.orders ADD tracking_code nvarchar(100) NULL;
IF COL_LENGTH(N'dbo.orders',N'shipped_at') IS NULL
 ALTER TABLE dbo.orders ADD shipped_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.orders',N'refunded_amount') IS NULL
 ALTER TABLE dbo.orders ADD refunded_amount numeric(19,0) NULL;
IF COL_LENGTH(N'dbo.orders',N'shop_account_id') IS NULL
 ALTER TABLE dbo.orders ADD shop_account_id nvarchar(36) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'UKawun3i7dq5g954aenqlv71lxq' AND parent_object_id=OBJECT_ID(N'dbo.order_refunds'))
 alter table dbo.order_refunds add constraint UKawun3i7dq5g954aenqlv71lxq unique (order_id);
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'UK4ujyoprdn2iwdfw154fitp87n' AND parent_object_id=OBJECT_ID(N'dbo.payment_attempts'))
 alter table dbo.payment_attempts add constraint UK4ujyoprdn2iwdfw154fitp87n unique (order_id);
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'UK3bntq8fs8tbd9l4tx8tqhi0wf' AND parent_object_id=OBJECT_ID(N'dbo.shop_accounts'))
 alter table dbo.shop_accounts add constraint UK3bntq8fs8tbd9l4tx8tqhi0wf unique (email);
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'FKmc53i8eq1lukxtbdulobibrek' AND parent_object_id=OBJECT_ID(N'dbo.order_refunds'))
 alter table dbo.order_refunds add constraint FKmc53i8eq1lukxtbdulobibrek foreign key (order_id) references dbo.orders;
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'FKjlobcqigxkvvs64c335t4by3j' AND parent_object_id=OBJECT_ID(N'dbo.orders'))
 alter table dbo.orders add constraint FKjlobcqigxkvvs64c335t4by3j foreign key (shop_account_id) references dbo.shop_accounts;
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'FKkgs2arrxop1qax0kjc0a7gx7n' AND parent_object_id=OBJECT_ID(N'dbo.payment_attempts'))
 alter table dbo.payment_attempts add constraint FKkgs2arrxop1qax0kjc0a7gx7n foreign key (order_id) references dbo.orders;
IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'FKofv8b6n5wmqpomqd8di1tply' AND parent_object_id=OBJECT_ID(N'dbo.shop_sessions'))
 alter table dbo.shop_sessions add constraint FKofv8b6n5wmqpomqd8di1tply foreign key (account_id) references dbo.shop_accounts;
 COMMIT TRANSACTION;
 PRINT N'Payment, email login, refund and manual shipping schema ready. Configure services separately.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
