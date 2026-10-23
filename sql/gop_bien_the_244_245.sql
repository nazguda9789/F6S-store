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
