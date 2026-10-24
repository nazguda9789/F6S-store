-- Restore prior visibility flags only. Stop web, back up first; no inventory/history is deleted.
DELIMITER $$
DROP PROCEDURE IF EXISTS revert_sports_catalog$$
CREATE PROCEDURE revert_sports_catalog()
main: BEGIN
DECLARE release_state VARCHAR(20);
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
START TRANSACTION;
SELECT state INTO release_state FROM catalog_release WHERE release_key='sports-20261024' FOR UPDATE;
IF release_state IS NULL OR release_state<>'APPLIED' THEN COMMIT; SELECT 'NOT_APPLIED_OR_ALREADY_REVERTED' AS result; LEAVE main; END IF;
UPDATE product t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='product' AND b.release_key='sports-20261024' SET t.active=b.old_flag;
UPDATE product_detail t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='product_detail' AND b.release_key='sports-20261024' SET t.active=b.old_flag;
UPDATE shoe_attribute t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='shoe_attribute' AND b.release_key='sports-20261024' SET t.active=b.old_flag;
UPDATE category t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='category' AND b.release_key='sports-20261024' SET t.deleted=b.old_flag;
UPDATE color t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='color' AND b.release_key='sports-20261024' SET t.deleted=b.old_flag;
UPDATE size t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='size' AND b.release_key='sports-20261024' SET t.deleted=b.old_flag;
UPDATE banner t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='banner' AND b.release_key='sports-20261024' SET t.active=b.old_flag;
UPDATE blog t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='blog' AND b.release_key='sports-20261024' SET t.archived=b.old_flag;
UPDATE product t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='PRODUCT' AND r.release_key='sports-20261024' SET t.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='product' AND b.record_id=t.id);
UPDATE category t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='CATEGORY' AND r.release_key='sports-20261024' SET t.deleted=b'1' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='category' AND b.record_id=t.id);
UPDATE color t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='COLOR' AND r.release_key='sports-20261024' SET t.deleted=b'1' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='color' AND b.record_id=t.id);
UPDATE size t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='SIZE' AND r.release_key='sports-20261024' SET t.deleted=b'1' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='size' AND b.record_id=t.id);
UPDATE banner t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='BANNER' AND r.release_key='sports-20261024' SET t.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='banner' AND b.record_id=t.id);
UPDATE blog t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='BLOG' AND r.release_key='sports-20261024' SET t.archived=b'1' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='blog' AND b.record_id=t.id);
UPDATE shoe_attribute t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='BRAND' AND r.release_key='sports-20261024' SET t.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='shoe_attribute' AND b.record_id=t.id);
UPDATE shoe_attribute t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='SOLE' AND r.release_key='sports-20261024' SET t.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='shoe_attribute' AND b.record_id=t.id);
UPDATE shoe_attribute t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='MATERIAL' AND r.release_key='sports-20261024' SET t.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='shoe_attribute' AND b.record_id=t.id);
UPDATE product_detail d JOIN catalog_release_item r ON r.record_id=d.product_id AND r.kind='PRODUCT' AND r.release_key='sports-20261024' SET d.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='sports-20261024' AND b.table_name='product_detail' AND b.record_id=d.id);
INSERT IGNORE INTO carts SELECT * FROM catalog_release_cart_archive;
UPDATE catalog_release SET state='ROLLED_BACK' WHERE release_key='sports-20261024';
COMMIT;
END$$
CALL revert_sports_catalog()$$
DROP PROCEDURE revert_sports_catalog$$
DELIMITER ;
