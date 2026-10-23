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
