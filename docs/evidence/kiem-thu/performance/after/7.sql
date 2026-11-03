select c1_0.id,c1_0.name,c1_0.status from categories c1_0
SELECT COUNT_BIG(*) FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE 1=1
SELECT v.id,v.product_id,v.size_id,v.chat_lieu_id,p.code,p.name,s.size_value,m.ten AS material_name,v.price,v.quantity,v.status FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE 1=1 ORDER BY p.id DESC,s.size_value,v.id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0
SELECT COUNT_BIG(*) FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE 1=1 
SELECT l.*,p.name,s.size_value,m.ten AS material_name FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE 1=1  ORDER BY l.created_at DESC,l.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
