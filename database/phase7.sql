IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
-- Phase 7: additive account/profile integrity. Run on the existing shopbangiay DB.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM employees WHERE user_id IS NULL)
    THROW 51000, 'Employee without User: resolve the existing identity before migration.', 1;
IF EXISTS (SELECT 1 FROM employees GROUP BY user_id HAVING COUNT(*)>1)
    THROW 51001, 'Multiple Employees reference one User.', 1;
IF EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id WHERE r.name='ROLE_ADMIN' AND NOT EXISTS(SELECT 1 FROM employees e WHERE e.user_id=u.id))
    THROW 51002, 'ROLE_ADMIN User without Employee: resolve before migration.', 1;
IF EXISTS (SELECT 1 FROM customers WHERE email IS NOT NULL AND email<>'' GROUP BY LOWER(email) HAVING COUNT(*)>1)
    THROW 51003, 'Duplicate customer email: resolve before migration.', 1;
IF EXISTS (SELECT 1 FROM employees WHERE email IS NOT NULL AND email<>'' GROUP BY LOWER(email) HAVING COUNT(*)>1)
    THROW 51004, 'Duplicate employee email: resolve before migration.', 1;
ALTER TABLE employees ALTER COLUMN user_id INT NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('employees') AND name='UX_employees_user_phase7')
    CREATE UNIQUE INDEX UX_employees_user_phase7 ON employees(user_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('employees') AND name='UX_employees_email_phase7')
    CREATE UNIQUE INDEX UX_employees_email_phase7 ON employees(email) WHERE email IS NOT NULL AND email<>'';
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('customers') AND name='UX_customers_email_phase7')
    CREATE UNIQUE INDEX UX_customers_email_phase7 ON customers(email) WHERE email IS NOT NULL AND email<>'';
COMMIT TRANSACTION;
