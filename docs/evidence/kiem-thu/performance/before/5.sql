select c1_0.id,c1_0.name,c1_0.status from categories c1_0
select p1_0.id,p1_0.category_id,p1_0.code,p1_0.description,p1_0.image_url,p1_0.name,p1_0.status from products p1_0 where p1_0.status=1 and exists(select pv1_0.id from product_variants pv1_0 where pv1_0.product_id=p1_0.id and pv1_0.status=1) and (lower(p1_0.name) like ? escape '!' or lower(p1_0.code) like ? escape '!') order by p1_0.name,p1_0.id offset ? rows fetch first ? rows only
select count_big(p1_0.id) from products p1_0 where p1_0.status=1 and exists(select pv1_0.id from product_variants pv1_0 where pv1_0.product_id=p1_0.id and pv1_0.status=1) and (lower(p1_0.name) like ? escape '!' or lower(p1_0.code) like ? escape '!')
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 left join products p1_0 on p1_0.id=pv1_0.product_id where p1_0.id in (?,?,?,?,?,?,?,?,?,?,?,?) and pv1_0.status=1 and p1_0.status=1
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select c1_0.id,c1_0.name,c1_0.status from categories c1_0 order by c1_0.name,c1_0.id
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select p1_0.id,p1_0.discount_type,p1_0.discount_value,p1_0.end_date,p1_0.name,p1_0.start_date,p1_0.status from promotions p1_0 where p1_0.id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select p1_0.id,p1_0.discount_type,p1_0.discount_value,p1_0.end_date,p1_0.name,p1_0.start_date,p1_0.status from promotions p1_0 where p1_0.id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pho1_0.id,pho1_0.code,pho1_0.created_at,pho1_0.customer_id,pho1_0.customer_label,pho1_0.expires_at,pho1_0.note,pho1_0.payment_method,pho1_0.staff_user_id,pho1_0.voucher_code from pos_held_orders pho1_0 join users su1_0 on su1_0.id=pho1_0.staff_user_id where su1_0.username=? and pho1_0.expires_at>? order by pho1_0.created_at
SELECT COUNT_BIG(*) FROM customers WHERE status=1 AND (LOWER(full_name) LIKE ? ESCAPE '!' OR phone LIKE ? ESCAPE '!')
SELECT id,full_name,phone FROM customers WHERE status=1 AND (LOWER(full_name) LIKE ? ESCAPE '!' OR phone LIKE ? ESCAPE '!') ORDER BY full_name,id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
