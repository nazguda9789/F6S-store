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
