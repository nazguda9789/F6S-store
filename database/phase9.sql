IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.invoices', N'U') IS NULL
CREATE TABLE dbo.invoices (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL UNIQUE REFERENCES dbo.orders(id),
    invoice_code VARCHAR(255) NOT NULL UNIQUE,
    issued_at DATETIME2 NOT NULL,
    customer_name NVARCHAR(255) NOT NULL,
    customer_phone NVARCHAR(255) NULL,
    employee_name NVARCHAR(255) NULL,
    cash_received DECIMAL(19,2) NULL,
    change_amount DECIMAL(19,2) NULL
);
INSERT INTO dbo.invoices(order_id,invoice_code,issued_at,customer_name,customer_phone,employee_name)
SELECT o.id, 'HD'+RIGHT('000000'+CAST(o.id AS VARCHAR(20)),6), COALESCE(o.created_at,SYSDATETIME()),
       COALESCE(c.full_name,o.receiver_name,N'Khách lẻ'),COALESCE(c.phone,o.phone_number),
       COALESCE(e.full_name,CASE WHEN o.order_type='ONLINE' THEN N'Đơn online' ELSE N'Không lưu người bán' END)
FROM dbo.orders o LEFT JOIN dbo.customers c ON c.id=o.customer_id LEFT JOIN dbo.employees e ON e.id=o.employee_id
WHERE NOT EXISTS (SELECT 1 FROM dbo.invoices i WHERE i.order_id=o.id);
COMMIT;
