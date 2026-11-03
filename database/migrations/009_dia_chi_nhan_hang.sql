-- SQL Server. Chọn database bằng sqlcmd -d; không tự chuyển database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shopbangiay_thu')
    AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.dia_chi_nhan_hang',N'U') IS NULL
        CREATE TABLE dbo.dia_chi_nhan_hang (
            id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dia_chi_nhan_hang PRIMARY KEY,
            customer_id INT NOT NULL,
            ten_nguoi_nhan NVARCHAR(100) NOT NULL,
            so_dien_thoai VARCHAR(20) NOT NULL,
            dia_chi NVARCHAR(255) NOT NULL,
            la_mac_dinh BIT NOT NULL CONSTRAINT DF_dia_chi_mac_dinh DEFAULT 0,
            thu_tu TINYINT NOT NULL,
            created_at DATETIME2 NOT NULL CONSTRAINT DF_dia_chi_created_at DEFAULT SYSDATETIME(),
            CONSTRAINT FK_dia_chi_customer FOREIGN KEY(customer_id) REFERENCES dbo.customers(id) ON DELETE CASCADE,
            CONSTRAINT CK_dia_chi_thu_tu CHECK(thu_tu BETWEEN 1 AND 3),
            CONSTRAINT UQ_dia_chi_customer_slot UNIQUE(customer_id,thu_tu)
        );
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.dia_chi_nhan_hang') AND name=N'UX_dia_chi_customer_default')
        CREATE UNIQUE INDEX UX_dia_chi_customer_default ON dbo.dia_chi_nhan_hang(customer_id) WHERE la_mac_dinh=1;
    -- Chỉ bù khách chưa có sổ; chạy lại không ghi đè địa chỉ đã được người dùng sửa.
    INSERT dbo.dia_chi_nhan_hang(customer_id,ten_nguoi_nhan,so_dien_thoai,dia_chi,la_mac_dinh,thu_tu,created_at)
        SELECT c.id,c.full_name,COALESCE(c.phone,''),c.address,1,1,COALESCE(c.created_at,SYSDATETIME())
        FROM dbo.customers c WITH(UPDLOCK,HOLDLOCK)
        WHERE NULLIF(LTRIM(RTRIM(c.address)),N'') IS NOT NULL
          AND NOT EXISTS(SELECT 1 FROM dbo.dia_chi_nhan_hang a WHERE a.customer_id=c.id);
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
