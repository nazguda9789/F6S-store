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
