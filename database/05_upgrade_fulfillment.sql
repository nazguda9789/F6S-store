-- Back up f6s_shop and stop backend. Existing Vue/SQL Server: run 03,04 first if needed.
-- No data reset, no fake historic stock-release timestamp, no changes to image files.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.payment_attempts',N'U') IS NULL OR OBJECT_ID(N'dbo.order_events',N'U') IS NULL THROW 51040,N'Run 03/04 migrations before 05.',1;
IF COL_LENGTH(N'dbo.orders',N'stock_released_at') IS NULL
 ALTER TABLE dbo.orders ADD stock_released_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_status') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_status nvarchar(20) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_reason') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_reason nvarchar(500) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_requested_at') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_requested_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_resolved_at') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_resolved_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_resolved_by') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_resolved_by nvarchar(100) NULL;
IF COL_LENGTH(N'dbo.orders',N'cancellation_decision_reason') IS NULL
 ALTER TABLE dbo.orders ADD cancellation_decision_reason nvarchar(500) NULL;
IF COL_LENGTH(N'dbo.orders',N'delivery_issue') IS NULL
 ALTER TABLE dbo.orders ADD delivery_issue nvarchar(500) NULL;
IF COL_LENGTH(N'dbo.payment_attempts',N'reconciled_at') IS NULL
 ALTER TABLE dbo.payment_attempts ADD reconciled_at datetimeoffset(6) NULL;
IF COL_LENGTH(N'dbo.payment_attempts',N'reconciled_by') IS NULL
 ALTER TABLE dbo.payment_attempts ADD reconciled_by nvarchar(100) NULL;
IF COL_LENGTH(N'dbo.payment_attempts',N'reconciliation_reference') IS NULL
 ALTER TABLE dbo.payment_attempts ADD reconciliation_reference nvarchar(120) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.orders') AND name=N'idx_orders_cancellation')
 EXEC(N'CREATE INDEX idx_orders_cancellation ON dbo.orders(cancellation_status,created_at)');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.payment_attempts') AND name=N'idx_payment_expiry')
 EXEC(N'CREATE INDEX idx_payment_expiry ON dbo.payment_attempts(expires_at,status)');
 COMMIT TRANSACTION;
 PRINT N'Fulfillment and manual reconciliation upgrade ready. No payment or stock was changed.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
