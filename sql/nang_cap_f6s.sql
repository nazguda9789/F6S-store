-- F6S: NANG CAP database dang dung, KHONG DROP / TRUNCATE / reset mat khau.
-- Sao luu va chon dung schema truoc khi chay. Co the chay lai.
-- Chi cap nhat cau truc OTP va module giay. Khong them/noi dung mau.
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
