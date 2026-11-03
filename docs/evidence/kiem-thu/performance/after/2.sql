select c1_0.id,c1_0.name,c1_0.status from categories c1_0
select p1_0.id,c1_0.id,c1_0.name,c1_0.status,p1_0.code,p1_0.description,p1_0.image_url,p1_0.name,p1_0.status from products p1_0 left join categories c1_0 on c1_0.id=p1_0.category_id where p1_0.id=?
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 where pv1_0.product_id=? and pv1_0.status=1
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select p1_0.id,p1_0.category_id,p1_0.code,p1_0.description,p1_0.image_url,p1_0.name,p1_0.status from products p1_0 where p1_0.status=1
select pi1_0.id,pi1_0.la_anh_chinh,pi1_0.duong_dan,pi1_0.thu_tu,pi1_0.product_id from product_images pi1_0 where pi1_0.product_id=? order by pi1_0.thu_tu,pi1_0.id
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 where pv1_0.product_id=? and pv1_0.status=1
select m1_0.id,m1_0.ten,m1_0.trang_thai from chat_lieu m1_0 where m1_0.id=?
select s1_0.id,s1_0.size_value from sizes s1_0 where s1_0.id=?
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select p1_0.id,p1_0.discount_type,p1_0.discount_value,p1_0.end_date,p1_0.name,p1_0.start_date,p1_0.status from promotions p1_0 where p1_0.id=?
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 where pv1_0.product_id=? and pv1_0.status=1
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 where pv1_0.product_id=? and pv1_0.status=1
select pv1_0.id,pv1_0.chat_lieu_id,pv1_0.price,pv1_0.product_id,pv1_0.quantity,pv1_0.size_id,pv1_0.status from product_variants pv1_0 where pv1_0.product_id=? and pv1_0.status=1
select pd1_0.id,pd1_0.product_id,pd1_0.promotion_id from promotion_details pd1_0 where pd1_0.product_id=?
