select c1_0.id,c1_0.name,c1_0.status from categories c1_0
SELECT COUNT_BIG(*) FROM products p WHERE 1=1
SELECT p.*,c.name AS category_name,v.price,v.max_price,COALESCE(v.stock,0) AS stock FROM products p LEFT JOIN categories c ON c.id=p.category_id LEFT JOIN (SELECT product_id,MIN(price) AS price,MAX(price) AS max_price,SUM(CAST(quantity AS bigint)) AS stock FROM product_variants GROUP BY product_id) v ON v.product_id=p.id WHERE 1=1 ORDER BY p.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
SELECT DISTINCT v.product_id,m.ten FROM product_variants v JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE v.status=1 AND v.product_id IN (?,?,?,?,?,?,?,?,?,?,?,?) ORDER BY m.ten
select c1_0.id,c1_0.name,c1_0.status from categories c1_0
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0
