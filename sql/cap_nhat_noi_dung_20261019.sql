-- Select the intended f6s_shop database before import. Back up first.
-- Additive demo content: no DROP, no password/account/order changes.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO banner (active,created_date,display_order,image_url,link_url,page_name,title,update_date,banner_type)
SELECT b'1',NOW(),1,'/image/banners/source-banner1.jpg','/category/nike','/','Bộ sưu tập giày Nike',NOW(),'SLIDER'
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url='/image/banners/source-banner1.jpg' AND page_name='/');
INSERT INTO banner (active,created_date,display_order,image_url,link_url,page_name,title,update_date,banner_type)
SELECT b'1',NOW(),2,'/image/banners/source-banner2.jpg','/category/giay-co-tu-nhien','/','Chọn giày cho sân cỏ tự nhiên',NOW(),'SLIDER'
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url='/image/banners/source-banner2.jpg' AND page_name='/');
INSERT INTO banner (active,created_date,display_order,image_url,link_url,page_name,title,update_date,banner_type)
SELECT b'1',NOW(),3,'/image/banners/source-banner3.jpg','/category/zocker','/','Zocker Winner Energy',NOW(),'SLIDER'
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url='/image/banners/source-banner3.jpg' AND page_name='/');
INSERT INTO banner (active,created_date,display_order,image_url,link_url,page_name,title,update_date,banner_type)
SELECT b'1',NOW(),1,'/image/banners/source-banner4.jpg','/category/adidas','/','Khám phá giày Adidas',NOW(),'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url='/image/banners/source-banner4.jpg' AND page_name='/');
INSERT INTO banner (active,created_date,display_order,image_url,link_url,page_name,title,update_date,banner_type)
SELECT b'1',NOW(),2,'/image/banners/source-munich.jpg','/category/giay-futsal','/','Lựa chọn giày Futsal',NOW(),'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url='/image/banners/source-munich.jpg' AND page_name='/');

-- Broaden the label to avoid presenting unrelated Adidas models as F50 Messi.
-- Preserve the existing URL and any custom name chosen by the store owner.
UPDATE category SET name='Adidas nổi bật'
WHERE slug='adidas-f50-messi-' AND TRIM(name)='Adidas F50 Messi';

INSERT INTO product_category (category_id,product_id)
SELECT c.id,p.id FROM category c JOIN product p
  ON p.slug IN ('adidas','nemeziz')
WHERE c.slug='adidas-f50-messi-'
  AND NOT EXISTS (SELECT 1 FROM product_category pc WHERE pc.category_id=c.id AND pc.product_id=p.id);
INSERT INTO product_category (category_id,product_id)
SELECT c.id,p.id FROM category c JOIN product p
  ON p.slug IN ('nike-air-zoom-mercurial-vapor-16-elite-fg-mbappe-personal-edition-grand-purplepale-ivory-fq8683-500',
               'nike-air-zoom-mercurial-vapor-16-pro-fg-attack-racer-bluewhite-fq8685-446')
WHERE c.slug='nike-zoom-mercurial'
  AND NOT EXISTS (SELECT 1 FROM product_category pc WHERE pc.category_id=c.id AND pc.product_id=p.id);

COMMIT;

-- Expected on the bundled seed: no rows. Custom databases may need more content.
SELECT c.id,c.name,c.slug,COUNT(DISTINCT p.id) AS active_products
FROM category c LEFT JOIN product_category pc ON pc.category_id=c.id
LEFT JOIN product p ON p.id=pc.product_id AND p.active=b'1'
WHERE c.deleted=b'0'
GROUP BY c.id,c.name,c.slug HAVING COUNT(DISTINCT p.id)<3;
