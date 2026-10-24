-- OPTIONAL DEMO ONLY. Back up and stop the web. Run after nang_cap_f6s.sql.
-- Legacy sample set: refuse to reintroduce old shoes after the sports replacement.
DELIMITER $$
DROP PROCEDURE IF EXISTS check_legacy_catalog_seed$$
CREATE PROCEDURE check_legacy_catalog_seed()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='catalog_release') THEN
  IF EXISTS(SELECT 1 FROM catalog_release WHERE release_key='sports-20261024' AND state='APPLIED') THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Old catalog seed disabled after sports release. Use the new sports installer.';
  END IF;
 END IF;
END$$
CALL check_legacy_catalog_seed()$$
DROP PROCEDURE check_legacy_catalog_seed$$
DELIMITER ;
-- Adds demo records without deleting/reclassifying old products or resetting stock on rerun.
-- Serial imports only; do not run concurrently. No real orders, payments, accounts or secrets.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
START TRANSACTION;
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'BRAND','F6S',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='BRAND' AND name='F6S');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'BRAND','Nike',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='BRAND' AND name='Nike');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'BRAND','Adidas',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='BRAND' AND name='Adidas');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'BRAND','Puma',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='BRAND' AND name='Puma');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'BRAND','New Balance',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='BRAND' AND name='New Balance');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'SOLE','Cao su bám đường',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='SOLE' AND name='Cao su bám đường');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'SOLE','EVA',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='SOLE' AND name='EVA');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'SOLE','Phylon',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='SOLE' AND name='Phylon');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'SOLE','TPU',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='SOLE' AND name='TPU');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'SOLE','Cao su non',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='SOLE' AND name='Cao su non');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'MATERIAL','Lưới thoáng khí',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='MATERIAL' AND name='Lưới thoáng khí');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'MATERIAL','Da tổng hợp',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='MATERIAL' AND name='Da tổng hợp');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'MATERIAL','Vải knit',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='MATERIAL' AND name='Vải knit');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'MATERIAL','Vải canvas',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='MATERIAL' AND name='Vải canvas');
INSERT INTO shoe_attribute(kind,name,active,description) SELECT 'MATERIAL','Da lộn',b'1','Dữ liệu danh mục mẫu; quản trị viên cần kiểm tra trước khi kinh doanh.' WHERE NOT EXISTS(SELECT 1 FROM shoe_attribute WHERE kind='MATERIAL' AND name='Da lộn');
INSERT INTO size(name,deleted) SELECT '38',b'0' WHERE NOT EXISTS(SELECT 1 FROM size WHERE name='38');
INSERT INTO size(name,deleted) SELECT '39',b'0' WHERE NOT EXISTS(SELECT 1 FROM size WHERE name='39');
INSERT INTO size(name,deleted) SELECT '40',b'0' WHERE NOT EXISTS(SELECT 1 FROM size WHERE name='40');
INSERT INTO size(name,deleted) SELECT '41',b'0' WHERE NOT EXISTS(SELECT 1 FROM size WHERE name='41');
INSERT INTO size(name,deleted) SELECT '42',b'0' WHERE NOT EXISTS(SELECT 1 FROM size WHERE name='42');
INSERT INTO color(name,code,deleted) SELECT 'Đen','#111827',b'0' WHERE NOT EXISTS(SELECT 1 FROM color WHERE name='Đen');
INSERT INTO color(name,code,deleted) SELECT 'Trắng','#FFFFFF',b'0' WHERE NOT EXISTS(SELECT 1 FROM color WHERE name='Trắng');
INSERT INTO color(name,code,deleted) SELECT 'Xám','#6B7280',b'0' WHERE NOT EXISTS(SELECT 1 FROM color WHERE name='Xám');
INSERT INTO color(name,code,deleted) SELECT 'Xanh navy','#1E3A5F',b'0' WHERE NOT EXISTS(SELECT 1 FROM color WHERE name='Xanh navy');
INSERT INTO color(name,code,deleted) SELECT 'Đỏ','#DC2626',b'0' WHERE NOT EXISTS(SELECT 1 FROM color WHERE name='Đỏ');
INSERT INTO category(name,slug,deleted,is_parent,position,created_date,update_date,meta_description,image) SELECT 'Giày chạy bộ','f6s-demo-category-1',b'0',b'1',20,NOW(6),NOW(6),'Danh mục giày thể thao F6S.','/image/demo/catalog-1.svg' WHERE NOT EXISTS(SELECT 1 FROM category WHERE name='Giày chạy bộ');
INSERT INTO category(name,slug,deleted,is_parent,position,created_date,update_date,meta_description,image) SELECT 'Giày tập luyện','f6s-demo-category-2',b'0',b'1',21,NOW(6),NOW(6),'Danh mục giày thể thao F6S.','/image/demo/catalog-2.svg' WHERE NOT EXISTS(SELECT 1 FROM category WHERE name='Giày tập luyện');
INSERT INTO category(name,slug,deleted,is_parent,position,created_date,update_date,meta_description,image) SELECT 'Giày đi bộ','f6s-demo-category-3',b'0',b'1',22,NOW(6),NOW(6),'Danh mục giày thể thao F6S.','/image/demo/catalog-3.svg' WHERE NOT EXISTS(SELECT 1 FROM category WHERE name='Giày đi bộ');
INSERT INTO category(name,slug,deleted,is_parent,position,created_date,update_date,meta_description,image) SELECT 'Giày bóng rổ','f6s-demo-category-4',b'0',b'1',23,NOW(6),NOW(6),'Danh mục giày thể thao F6S.','/image/demo/catalog-4.svg' WHERE NOT EXISTS(SELECT 1 FROM category WHERE name='Giày bóng rổ');
INSERT INTO category(name,slug,deleted,is_parent,position,created_date,update_date,meta_description,image) SELECT 'Giày lifestyle','f6s-demo-category-5',b'0',b'1',24,NOW(6),NOW(6),'Danh mục giày thể thao F6S.','/image/demo/catalog-5.svg' WHERE NOT EXISTS(SELECT 1 FROM category WHERE name='Giày lifestyle');
UPDATE shoe_attribute SET code=CONCAT('F6S-',CASE kind WHEN 'BRAND' THEN 'TH' WHEN 'SOLE' THEN 'DE' ELSE 'CL' END,LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0')) WHERE code IS NULL;
DROP TEMPORARY TABLE IF EXISTS f6s_demo_input_20261024;
CREATE TEMPORARY TABLE f6s_demo_input_20261024(seed_key VARCHAR(64),slug VARCHAR(255),name VARCHAR(255),brand VARCHAR(100),sole VARCHAR(100),material VARCHAR(100),category_name VARCHAR(255),color_name VARCHAR(255),price DOUBLE,image VARCHAR(255)) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO f6s_demo_input_20261024 VALUES
('catalog-demo-1','f6s-demo-20261024-1','[DEMO] Giày chạy bộ F6S 01','F6S','Cao su bám đường','Lưới thoáng khí','Giày chạy bộ','Đen',590000,'/image/demo/catalog-1.svg'),
('catalog-demo-2','f6s-demo-20261024-2','[DEMO] Giày tập luyện F6S 02','Nike','EVA','Da tổng hợp','Giày tập luyện','Trắng',690000,'/image/demo/catalog-2.svg'),
('catalog-demo-3','f6s-demo-20261024-3','[DEMO] Giày đi bộ F6S 03','Adidas','Phylon','Vải knit','Giày đi bộ','Xám',790000,'/image/demo/catalog-3.svg'),
('catalog-demo-4','f6s-demo-20261024-4','[DEMO] Giày bóng rổ F6S 04','Puma','TPU','Vải canvas','Giày bóng rổ','Xanh navy',890000,'/image/demo/catalog-4.svg'),
('catalog-demo-5','f6s-demo-20261024-5','[DEMO] Giày lifestyle F6S 05','New Balance','Cao su non','Da lộn','Giày lifestyle','Đỏ',990000,'/image/demo/catalog-5.svg');
DROP TEMPORARY TABLE IF EXISTS f6s_demo_new_20261024;
CREATE TEMPORARY TABLE f6s_demo_new_20261024(seed_key VARCHAR(64) PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO f6s_demo_new_20261024 SELECT d.seed_key FROM f6s_demo_input_20261024 d WHERE NOT EXISTS(SELECT 1 FROM catalog_demo_seed r WHERE r.seed_key=d.seed_key);
INSERT INTO product(name,slug,brand_id,sole_id,material_id,price,ole_price,weight,active,sold,image,description,short_description,meta_title,meta_description,created_at,updated_at)
SELECT d.name,d.slug,b.id,s.id,m.id,d.price,d.price+100000,0.65,b'1',0,d.image,
'<p>DỮ LIỆU DEMO: dùng kiểm thử danh mục, màu, size và giỏ hàng. Không phải sản phẩm thương mại hay thông số được hãng xác nhận. Thay bằng dữ liệu thật trước khi công khai.</p>',
'Sản phẩm minh họa, không phải thông tin thương mại đã xác minh.','F6S DEMO CATALOG 20261024','Dữ liệu minh họa F6S, không phải thông tin sản phẩm thương mại.',NOW(6),NOW(6)
FROM f6s_demo_input_20261024 d
JOIN shoe_attribute b ON b.kind='BRAND' AND b.name=d.brand AND b.active=b'1'
JOIN shoe_attribute s ON s.kind='SOLE' AND s.name=d.sole AND s.active=b'1'
JOIN shoe_attribute m ON m.kind='MATERIAL' AND m.name=d.material AND m.active=b'1'
WHERE NOT EXISTS(SELECT 1 FROM product p WHERE p.slug=d.slug) AND d.seed_key IN (SELECT seed_key FROM f6s_demo_new_20261024);
INSERT INTO catalog_demo_seed(seed_key,product_id)
SELECT d.seed_key,p.id FROM f6s_demo_input_20261024 d JOIN product p ON p.slug=d.slug AND p.meta_title='F6S DEMO CATALOG 20261024'
WHERE NOT EXISTS(SELECT 1 FROM catalog_demo_seed r WHERE r.seed_key=d.seed_key);
UPDATE product p JOIN catalog_demo_seed r ON r.product_id=p.id SET p.code=CONCAT('F6S-SP',LPAD(CAST(p.id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(p.id AS CHAR))),'0')) WHERE p.code IS NULL;
INSERT INTO product_category(product_id,category_id)
SELECT r.product_id,c.id FROM f6s_demo_input_20261024 d JOIN catalog_demo_seed r ON r.seed_key=d.seed_key
JOIN category c ON c.id=(SELECT MIN(id) FROM category WHERE name=d.category_name AND deleted=b'0')
WHERE NOT EXISTS(SELECT 1 FROM product_category pc WHERE pc.product_id=r.product_id AND pc.category_id=c.id) AND r.seed_key IN (SELECT seed_key FROM f6s_demo_new_20261024);
INSERT INTO product_detail(product_id,color_id,color,size_id,size,stock,price,old_price,active)
SELECT r.product_id,c.id,c.id,s.id,s.id,20,d.price,d.price+100000,b'1'
FROM f6s_demo_input_20261024 d JOIN catalog_demo_seed r ON r.seed_key=d.seed_key
JOIN color c ON c.id=(SELECT MIN(id) FROM color WHERE name=d.color_name AND deleted=b'0')
JOIN size s ON s.name IN ('38','39','40','41','42') AND s.deleted=b'0'
 AND s.id=(SELECT MIN(s2.id) FROM size s2 WHERE s2.name=s.name AND s2.deleted=b'0')
WHERE NOT EXISTS(SELECT 1 FROM product_detail pd WHERE pd.product_id=r.product_id AND pd.color_id=c.id AND pd.size_id=s.id) AND r.seed_key IN (SELECT seed_key FROM f6s_demo_new_20261024);
UPDATE product_detail pd JOIN catalog_demo_seed r ON r.product_id=pd.product_id
SET pd.sku=CONCAT('F6S-SKU',LPAD(CAST(pd.id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(pd.id AS CHAR))),'0')) WHERE pd.sku IS NULL;
INSERT INTO product_color_image(product_id,color_id,image)
SELECT r.product_id,c.id,d.image FROM f6s_demo_input_20261024 d JOIN catalog_demo_seed r ON r.seed_key=d.seed_key
JOIN color c ON c.id=(SELECT MIN(id) FROM color WHERE name=d.color_name AND deleted=b'0')
WHERE NOT EXISTS(SELECT 1 FROM product_color_image pci WHERE pci.product_id=r.product_id AND pci.color_id=c.id) AND r.seed_key IN (SELECT seed_key FROM f6s_demo_new_20261024);
INSERT INTO product_image(product_id,link_image)
SELECT r.product_id,d.image FROM f6s_demo_input_20261024 d JOIN catalog_demo_seed r ON r.seed_key=d.seed_key
WHERE NOT EXISTS(SELECT 1 FROM product_image pi WHERE pi.product_id=r.product_id AND pi.link_image=d.image) AND r.seed_key IN (SELECT seed_key FROM f6s_demo_new_20261024);
COMMIT;
SELECT r.seed_key,p.code,p.name,COUNT(pd.id) AS variants FROM catalog_demo_seed r JOIN product p ON p.id=r.product_id LEFT JOIN product_detail pd ON pd.product_id=p.id GROUP BY r.seed_key,p.id,p.code,p.name;
SELECT kind,COUNT(*) AS attribute_count FROM shoe_attribute GROUP BY kind;
SELECT d.seed_key,'SKIPPED_REVIEW_INACTIVE_ATTRIBUTE_OR_SLUG_CONFLICT' AS warning FROM f6s_demo_input_20261024 d WHERE NOT EXISTS(SELECT 1 FROM catalog_demo_seed r WHERE r.seed_key=d.seed_key);
SELECT r.seed_key,'REVIEW_DEMO_VARIANT_COUNT' AS warning,COUNT(pd.id) AS variants FROM catalog_demo_seed r LEFT JOIN product_detail pd ON pd.product_id=r.product_id GROUP BY r.seed_key HAVING COUNT(pd.id)<>5;
DROP TEMPORARY TABLE f6s_demo_new_20261024;
DROP TEMPORARY TABLE f6s_demo_input_20261024;
