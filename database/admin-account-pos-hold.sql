IF DB_NAME() NOT IN (N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
-- Apply explicitly to the intended database. Verification uses shop_verify only.
IF COL_LENGTH('users','full_name') IS NULL ALTER TABLE users ADD full_name nvarchar(255) NULL;
IF COL_LENGTH('users','email') IS NULL ALTER TABLE users ADD email nvarchar(255) NULL;
IF COL_LENGTH('users','phone') IS NULL ALTER TABLE users ADD phone nvarchar(255) NULL;
IF COL_LENGTH('users','created_at') IS NULL ALTER TABLE users ADD created_at datetime2 NULL;
ALTER TABLE employees ALTER COLUMN full_name nvarchar(255) NOT NULL;
ALTER TABLE users ALTER COLUMN full_name nvarchar(255) NULL;
-- Historical creation dates are unknown: leave old records NULL rather than invent dates.
IF OBJECT_ID('pos_held_orders','U') IS NULL
CREATE TABLE pos_held_orders (
 id bigint IDENTITY PRIMARY KEY, code varchar(40) NOT NULL UNIQUE,
 staff_user_id int NOT NULL REFERENCES users(id), customer_id int NULL REFERENCES customers(id),
 customer_label nvarchar(255) NULL, voucher_code nvarchar(50) NULL,
 payment_method varchar(20) NOT NULL CHECK(payment_method IN ('CASH','TRANSFER')),
 note nvarchar(500) NULL, created_at datetime2 NOT NULL, expires_at datetime2 NOT NULL
);
IF OBJECT_ID('pos_held_items','U') IS NULL
CREATE TABLE pos_held_items (
 id bigint IDENTITY PRIMARY KEY, held_id bigint NOT NULL REFERENCES pos_held_orders(id),
 variant_id int NOT NULL REFERENCES product_variants(id), quantity int NOT NULL CHECK(quantity>0),
 CONSTRAINT uq_pos_hold_variant UNIQUE(held_id,variant_id)
);
ALTER TABLE pos_held_orders ALTER COLUMN customer_label nvarchar(255) NULL;
ALTER TABLE pos_held_orders ALTER COLUMN note nvarchar(500) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_pos_hold_staff_expiry' AND object_id=OBJECT_ID('pos_held_orders'))
CREATE INDEX ix_pos_hold_staff_expiry ON pos_held_orders(staff_user_id,expires_at);
-- Defaults: shop.timezone=Asia/Ho_Chi_Minh; shop.pos.hold.expiry-mode=END_OF_DAY;
-- shop.pos.hold.cleanup-interval=PT5M. HOURS_24 is the other supported expiry mode.
