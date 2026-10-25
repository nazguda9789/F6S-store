const fs = require("node:fs"),
  path = require("node:path");
const root = path.resolve(__dirname, ".."),
  ddl = fs.readFileSync(
    path.join(root, "backend/target/sqlserver-schema.sql"),
    "utf8",
  ),
  tables = new Set([
    "payment_attempts",
    "email_challenges",
    "order_refunds",
    "shop_accounts",
    "shop_sessions",
  ]),
  statements = ddl
    .split(";")
    .map((s) => s.trim())
    .filter(Boolean),
  parts = [];
for (const s of statements) {
  const m = s.match(/^create table dbo\.(\w+)/);
  if (m && tables.has(m[1]))
    parts.push(`IF OBJECT_ID(N'dbo.${m[1]}',N'U') IS NULL\n ${s};`);
}
for (const [name, type] of Object.entries({
  payment_status: "nvarchar(20)",
  shipping_fee: "numeric(19,0)",
  carrier: "nvarchar(60)",
  tracking_code: "nvarchar(100)",
  shipped_at: "datetimeoffset(6)",
  refunded_amount: "numeric(19,0)",
  shop_account_id: "nvarchar(36)",
}))
  parts.push(
    `IF COL_LENGTH(N'dbo.orders',N'${name}') IS NULL\n ALTER TABLE dbo.orders ADD ${name} ${type} NULL;`,
  );
for (const s of statements) {
  const m = s.match(/^alter table dbo\.(\w+) add constraint (\w+) (.*)$/s);
  if (m && (tables.has(m[1]) || /references dbo\.shop_accounts/.test(m[3])))
    parts.push(
      `IF NOT EXISTS(SELECT 1 FROM sys.objects WHERE name=N'${m[2]}' AND parent_object_id=OBJECT_ID(N'dbo.${m[1]}'))\n ${s};`,
    );
}
fs.writeFileSync(
  path.join(root, "database/04_upgrade_integrations.sql"),
  `-- Stop backend and back up f6s_shop first. Existing Vue/SQL Server only.
-- Run 03 first on old installations; new empty database uses latest 01 then 02.
-- No secret, OTP, fake payment or stock reset is inserted.
USE f6s_shop;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.order_events',N'U') IS NULL THROW 51030,N'Run 03_upgrade_bee_modules.sql first.',1;
${parts.join("\n")}
 COMMIT TRANSACTION;
 PRINT N'Payment, email login, refund and manual shipping schema ready. Configure services separately.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
`,
);
