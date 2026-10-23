-- F6S: NANG CAP database dang dung, KHONG DROP / TRUNCATE / reset mat khau.
-- Sao luu va chon dung schema truoc khi chay. Co the chay lai.
-- Nang cap OTP/thuoc tinh va chuan hoa ma SKU (luu ma cu vao legacy_sku).
-- DUNG WEB truoc khi chay. Khong tao san pham mau. Co buoc gop 245 -> 244 da duyet, co dieu kien/audit; khong xoa dong lich su.
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

-- Approved duplicate variant resolution. Historical rows remain intact.
-- Approved merge: source 245 -> target 244, only for product 31 / color 9 / size 12.
-- STOP WEB and back up first. Retain source row, SKU aliases and historical invoices.
-- Refuse to apply if the source is still referenced by carts/open drafts.
CREATE TABLE IF NOT EXISTS catalog_variant_merge (
    migration_key VARCHAR(64) NOT NULL PRIMARY KEY,
    source_variant_id BIGINT NOT NULL,
    target_variant_id BIGINT NOT NULL,
    source_stock_before INT NOT NULL,
    target_stock_before INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

START TRANSACTION;
SELECT id FROM product_detail WHERE id IN (244,245) ORDER BY id FOR UPDATE;
SET @f6s_merge_allowed = (
    SELECT COUNT(*) FROM product_detail target JOIN product_detail source ON source.id=245
    WHERE target.id=244 AND target.product_id=31 AND source.product_id=31
      AND target.color=9 AND source.color=9 AND target.size=12 AND source.size=12
      AND target.color_id=9 AND source.color_id=9 AND target.size_id=12 AND source.size_id=12
      AND target.active=b'1' AND (target.price <=> source.price)
      AND target.stock>=0 AND source.stock>=0 AND CAST(target.stock AS SIGNED)+CAST(source.stock AS SIGNED)<=2147483647
      AND NOT EXISTS (SELECT 1 FROM carts WHERE product_detail_id=245)
      AND NOT EXISTS (SELECT 1 FROM order_detail_draft WHERE id_product_detail=245)
      AND NOT EXISTS (SELECT 1 FROM catalog_variant_merge WHERE migration_key='merge-244-245-v1')
);
INSERT INTO catalog_variant_merge (migration_key,source_variant_id,target_variant_id,source_stock_before,target_stock_before)
SELECT 'merge-244-245-v1',source.id,target.id,source.stock,target.stock
FROM product_detail target JOIN product_detail source ON source.id=245
WHERE target.id=244 AND @f6s_merge_allowed=1;
SET @f6s_merge_apply = ROW_COUNT();
UPDATE product_detail target JOIN catalog_variant_merge audit ON audit.target_variant_id=target.id
SET target.stock=audit.target_stock_before+audit.source_stock_before
WHERE audit.migration_key='merge-244-245-v1' AND @f6s_merge_apply=1;
UPDATE product_detail SET stock=0,active=b'0' WHERE id=245 AND @f6s_merge_apply=1;
COMMIT;
SELECT CASE WHEN @f6s_merge_apply=1 THEN 'APPLIED'
            WHEN EXISTS(SELECT 1 FROM catalog_variant_merge WHERE migration_key='merge-244-245-v1') THEN 'ALREADY_APPLIED'
            ELSE 'SKIPPED_REVIEW_CART_DRAFT_OR_VARIANT_DATA' END AS merge_status;

-- Persist customer identity independently of the staff creator.
SET @f6s_customer_column = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='order_draft' AND column_name='customer_id')=0,
  'ALTER TABLE order_draft ADD COLUMN customer_id BIGINT NULL', 'SELECT 1');
PREPARE f6s_customer_stmt FROM @f6s_customer_column;
EXECUTE f6s_customer_stmt;
DEALLOCATE PREPARE f6s_customer_stmt;
-- F6S catalog metadata, 24/10/2026. STOP WEB, back up, select the existing schema.
-- No product, inventory, user or invoice is deleted/renamed/reset.
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shoe_attribute' AND column_name='code'),'SELECT 1','ALTER TABLE shoe_attribute ADD COLUMN code VARCHAR(32) NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shoe_attribute' AND column_name='description'),'SELECT 1','ALTER TABLE shoe_attribute ADD COLUMN description VARCHAR(500) NULL');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shoe_attribute' AND column_name='created_at'),'SELECT 1','ALTER TABLE shoe_attribute ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shoe_attribute' AND column_name='updated_at'),'SELECT 1','ALTER TABLE shoe_attribute ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE shoe_attribute SET code=CONCAT('F6S-',CASE kind WHEN 'BRAND' THEN 'TH' WHEN 'SOLE' THEN 'DE' ELSE 'CL' END,LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0')) WHERE code IS NULL OR TRIM(code)='';
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='shoe_attribute' AND index_name='uk_shoe_attribute_code'),'SELECT 1','CREATE UNIQUE INDEX uk_shoe_attribute_code ON shoe_attribute(code)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
CREATE TABLE IF NOT EXISTS catalog_demo_seed (
 seed_key VARCHAR(64) NOT NULL PRIMARY KEY,
 product_id BIGINT NOT NULL UNIQUE,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_catalog_demo_product FOREIGN KEY(product_id) REFERENCES product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Foreign keys are added only when legacy values have no orphan references.
SET @orphans = (SELECT COUNT(*) FROM product p LEFT JOIN shoe_attribute a ON a.id=p.brand_id WHERE p.brand_id IS NOT NULL AND a.id IS NULL);
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='product' AND constraint_name='fk_f6s_product_brand') OR @orphans>0,'SELECT 1','ALTER TABLE product ADD CONSTRAINT fk_f6s_product_brand FOREIGN KEY (brand_id) REFERENCES shoe_attribute(id)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT 'brand_id' AS checked_column,@orphans AS orphan_references_to_review;
SET @orphans = (SELECT COUNT(*) FROM product p LEFT JOIN shoe_attribute a ON a.id=p.sole_id WHERE p.sole_id IS NOT NULL AND a.id IS NULL);
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='product' AND constraint_name='fk_f6s_product_sole') OR @orphans>0,'SELECT 1','ALTER TABLE product ADD CONSTRAINT fk_f6s_product_sole FOREIGN KEY (sole_id) REFERENCES shoe_attribute(id)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT 'sole_id' AS checked_column,@orphans AS orphan_references_to_review;
SET @orphans = (SELECT COUNT(*) FROM product p LEFT JOIN shoe_attribute a ON a.id=p.material_id WHERE p.material_id IS NOT NULL AND a.id IS NULL);
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='product' AND constraint_name='fk_f6s_product_material') OR @orphans>0,'SELECT 1','ALTER TABLE product ADD CONSTRAINT fk_f6s_product_material FOREIGN KEY (material_id) REFERENCES shoe_attribute(id)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT 'material_id' AS checked_column,@orphans AS orphan_references_to_review;
SET @f6s_customer_column = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='invoice' AND column_name='customer_id')=0,
  'ALTER TABLE invoice ADD COLUMN customer_id BIGINT NULL', 'SELECT 1');
PREPARE f6s_customer_stmt FROM @f6s_customer_column;
EXECUTE f6s_customer_stmt;
DEALLOCATE PREPARE f6s_customer_stmt;
