const fs = require("node:fs"),
  path = require("node:path");
const root = path.resolve(__dirname, "..");
const columns = {
  orders: {
    stock_released_at: "datetimeoffset(6)",
    cancellation_status: "nvarchar(20)",
    cancellation_reason: "nvarchar(500)",
    cancellation_requested_at: "datetimeoffset(6)",
    cancellation_resolved_at: "datetimeoffset(6)",
    cancellation_resolved_by: "nvarchar(100)",
    cancellation_decision_reason: "nvarchar(500)",
    delivery_issue: "nvarchar(500)",
  },
  payment_attempts: {
    reconciled_at: "datetimeoffset(6)",
    reconciled_by: "nvarchar(100)",
    reconciliation_reference: "nvarchar(120)",
  },
};
const statements = [];
for (const [table, fields] of Object.entries(columns))
  for (const [name, type] of Object.entries(fields))
    statements.push(
      `IF COL_LENGTH(N'dbo.${table}',N'${name}') IS NULL\n ALTER TABLE dbo.${table} ADD ${name} ${type} NULL;`,
    );
for (const [table, name, fields] of [
  ["orders", "idx_orders_cancellation", "cancellation_status,created_at"],
  ["payment_attempts", "idx_payment_expiry", "expires_at,status"],
])
  statements.push(
    `IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.${table}') AND name=N'${name}')\n EXEC(N'CREATE INDEX ${name} ON dbo.${table}(${fields})');`,
  );
fs.writeFileSync(
  path.join(root, "database/05_upgrade_fulfillment.sql"),
  `-- Back up f6s_shop and stop backend. Existing Vue/SQL Server: run 03,04 first if needed.
-- No data reset, no fake historic stock-release timestamp, no changes to image files.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.payment_attempts',N'U') IS NULL OR OBJECT_ID(N'dbo.order_events',N'U') IS NULL THROW 51040,N'Run 03/04 migrations before 05.',1;
${statements.join("\n")}
 COMMIT TRANSACTION;
 PRINT N'Fulfillment and manual reconciliation upgrade ready. No payment or stock was changed.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
`,
);
