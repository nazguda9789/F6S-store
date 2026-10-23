-- F6S: NANG CAP database dang dung, KHONG DROP / TRUNCATE / reset mat khau.
-- Sao luu va chon dung schema truoc khi chay. Co the chay lai.
-- Nang cap OTP/thuoc tinh va chuan hoa ma SKU (luu ma cu vao legacy_sku).
-- DUNG WEB truoc khi chay. Khong tao san pham mau, khong gop/xoa bien the.
-- Additive schema update only. Select the intended database first; back up first.
-- No users are deleted, activated, unlocked or assigned a password by this script.
CREATE TABLE IF NOT EXISTS pending_registration (
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    created_date DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chay tren database F6S hien tai. KHONG DROP database/table, KHONG sua users/invoices.
-- Tuong thich MySQL 8 va MariaDB: them cot qua information_schema, chay lai an toan.
CREATE TABLE IF NOT EXISTS shoe_attribute (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    CONSTRAINT uk_shoe_attribute_kind_name UNIQUE (kind, name)
);
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product' AND column_name='brand_id'), 'SELECT 1', 'ALTER TABLE product ADD COLUMN brand_id BIGINT NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product' AND column_name='sole_id'), 'SELECT 1', 'ALTER TABLE product ADD COLUMN sole_id BIGINT NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product' AND column_name='material_id'), 'SELECT 1', 'ALTER TABLE product ADD COLUMN material_id BIGINT NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- Khong doan thuong hieu/chat lieu/de giay cua san pham cu. Quan tri vien phan loai sau.

-- Consolidated stable catalog identifiers, 23/10/2026.
-- F6S catalog codes. Back up first; run before starting the new application.
-- No product/variant IDs, stock, cart references or historical invoice codes are changed.
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product' AND column_name='code'), 'SELECT 1', 'ALTER TABLE product ADD COLUMN code VARCHAR(32) NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product_detail' AND column_name='legacy_sku'), 'SELECT 1', 'ALTER TABLE product_detail ADD COLUMN legacy_sku VARCHAR(255) NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE product SET code=CONCAT('F6S-SP',LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0')) WHERE code IS NULL OR TRIM(code)='';
UPDATE product_detail SET legacy_sku=sku
WHERE legacy_sku IS NULL AND sku IS NOT NULL
  AND BINARY sku<>BINARY CONCAT('F6S-SKU',LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0'));
UPDATE product_detail SET sku=CONCAT('F6S-SKU',LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0'));

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='product' AND index_name='uk_f6s_product_code'), 'SELECT 1', 'CREATE UNIQUE INDEX uk_f6s_product_code ON product(code)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='product_detail' AND index_name='uk_f6s_variant_sku'), 'SELECT 1', 'CREATE UNIQUE INDEX uk_f6s_variant_sku ON product_detail(sku)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- REPORT ONLY: do not merge variants automatically; existing invoice/cart references need review.
SELECT product_id,color,size,COUNT(*) AS duplicate_variants
FROM product_detail GROUP BY product_id,color,size HAVING COUNT(*)>1;
