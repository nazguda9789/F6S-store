-- F6S Sports / 03 OPTIONAL BUSINESS DEMO
-- FRESH INSTALL ONLY. Use a new database; stop on any error. Never use mysql --force.
-- No OTP, service keys or VNPAY payment receipts are fabricated.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE f6s_shop_clean_demo;
-- Optional: five staff/customers, vouchers, carts and synthetic invoices. Never use real-store reports.
-- Accounts are inactive, with no shared password. No banking/payment proof is inserted.
START TRANSACTION;
INSERT INTO demo_dataset_install(dataset_key,description,release_key) VALUES('all-modules-sports-20261024','Synthetic business fixtures, not real orders/payments','sports-20261024');
  DROP TEMPORARY TABLE IF EXISTS f6s_seed_rows;
  CREATE TEMPORARY TABLE f6s_seed_rows(n INT PRIMARY KEY,person_name VARCHAR(80),topic VARCHAR(150)) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO f6s_seed_rows VALUES
    (1,'An','Chọn size giày chạy bộ'),(2,'Bình','Bảo quản giày thể thao'),
    (3,'Chi','Chọn đế phù hợp mặt sân'),(4,'Dũng','Phân biệt giày tập luyện và đi bộ'),(5,'Hà','Vệ sinh giày đúng cách');
  INSERT INTO users(email,full_name,phone,address,role,user_type,actived,deleted,activation_status,account_non_locked,failed_attempts,is_google_user,created_date)
  SELECT CONCAT('f6s-demo-nv-',n,'@example.invalid'),CONCAT('[DEMO] Nhân viên ',person_name),CONCAT('000000010',n),'Địa chỉ minh họa, không giao hàng','ROLE_EMPLOYEE','NORMAL',b'0',b'0',b'1',b'1',0,b'0',NOW(6) FROM f6s_seed_rows;
  INSERT INTO users(email,full_name,phone,address,role,user_type,actived,deleted,activation_status,account_non_locked,failed_attempts,is_google_user,created_date)
  SELECT CONCAT('f6s-demo-kh-',n,'@example.invalid'),CONCAT('[DEMO] Khách hàng ',person_name),CONCAT('000000020',n),'Địa chỉ minh họa, không giao hàng','ROLE_USER','NORMAL',b'0',b'0',b'1',b'1',0,b'0',NOW(6) FROM f6s_seed_rows;
  DROP TEMPORARY TABLE IF EXISTS f6s_seed_links;
  CREATE TEMPORARY TABLE f6s_seed_links AS
  SELECT r.n,r.person_name,r.topic,u.id customer_id,e.id staff_id,p.id product_id,p.price,p.image,p.name product_name,
    pd.id detail_id,pd.sku,c.name color_name,s.name size_name
  FROM f6s_seed_rows r JOIN users u ON u.email=CONCAT('f6s-demo-kh-',r.n,'@example.invalid')
  JOIN users e ON e.email=CONCAT('f6s-demo-nv-',r.n,'@example.invalid')
  JOIN catalog_release_item ds ON ds.release_key='sports-20261024' AND ds.kind='PRODUCT' AND ds.ordinal=r.n
  JOIN product p ON p.id=ds.record_id
  JOIN product_detail pd ON pd.id=(SELECT MIN(x.id) FROM product_detail x WHERE x.product_id=p.id AND x.active=b'1')
  JOIN color c ON c.id=pd.color JOIN size s ON s.id=pd.size;
  INSERT INTO voucher(code,name,discount,min_amount,block,created_date,update_date)
  SELECT CONCAT('F6S-DEMO-VC-',n),CONCAT('[DEMO] Phiếu giảm ',n*10000,' đồng'),n*10000,500000,b'0',NOW(6),NOW(6) FROM f6s_seed_links;
  INSERT INTO discount_product(product_id,user_id,discount_product_type,discount_value,max_discount_value,start_date,end_date,created_date)
  SELECT product_id,staff_id,'PERCENT',5,50000,NOW(6),DATE_ADD(NOW(6),INTERVAL 30 DAY),NOW(6) FROM f6s_seed_links;
  INSERT INTO carts(user_id,product_detail_id,quantity,created_date,update_date)
  SELECT customer_id,detail_id,1,NOW(6),NOW(6) FROM f6s_seed_links;
  INSERT INTO contact(full_name,email,phone,content,ip_address,created_date)
  SELECT CONCAT('[DEMO] Khách hàng ',person_name),CONCAT('f6s-demo-kh-',n,'@example.invalid'),CONCAT('000000020',n),CONCAT('[DEMO] Cần tư vấn: ',topic),'127.0.0.1',NOW(6) FROM f6s_seed_links;
  INSERT INTO chatting(user_id,type,content,created_date)
  SELECT customer_id,'USER',CONCAT('[DEMO] Xin tư vấn ',topic),NOW(6) FROM f6s_seed_links;
  INSERT INTO product_view_history(product_id,user_id,session_id,view_count,created_at,last_viewed_at)
  SELECT product_id,customer_id,CONCAT('F6S-DEMO-SESSION-',n),n,NOW(6),NOW(6) FROM f6s_seed_links;
  INSERT INTO order_draft(customer_id,created_by_id,full_name,phone,email,address_detail,note,total_amount,discount,ship_cost,created_date,update_date)
  SELECT customer_id,staff_id,CONCAT('[DEMO] Khách hàng ',person_name),CONCAT('000000020',n),CONCAT('f6s-demo-kh-',n,'@example.invalid'),
    'Địa chỉ minh họa, không giao hàng',CONCAT('F6S-DEMO-DRAFT-',n),price,0,0,NOW(6),NOW(6) FROM f6s_seed_links;
  INSERT INTO order_detail_draft(order_draft_id,id_product_detail,price,quantity,product_name,product_image,color_name,size_name,sku,weight)
  SELECT d.id,l.detail_id,l.price,1,l.product_name,l.image,l.color_name,l.size_name,l.sku,0.65 FROM f6s_seed_links l JOIN order_draft d ON d.note=CONCAT('F6S-DEMO-DRAFT-',l.n);
  -- Scenarios: counter complete, online waiting, online shipped, online complete, cancelled.
  -- Opening stock is not decremented: these are historical fixtures, not newly placed orders.
  INSERT INTO invoice(code,customer_id,user_id,full_name,phone,email,address_detail,note,total_amount,discount,ship_cost,weight,invoice_type,invoice_status,pay_type,created_date,update_date,completed_date,token)
  SELECT CONCAT('F6S-DEMO-HD-',n),customer_id,IF(n=1,staff_id,customer_id),CONCAT('[DEMO] Khách hàng ',person_name),CONCAT('000000020',n),CONCAT('f6s-demo-kh-',n,'@example.invalid'),
    'Địa chỉ minh họa, không giao hàng','[DEMO] Hóa đơn minh họa; không gửi vận chuyển hoặc coi là doanh thu thật.',price,0,0,0.65,
    IF(n=1,'TAI_QUAY','ONLINE'),CASE n WHEN 1 THEN 'HOAN_THANH' WHEN 2 THEN 'DANG_CHO_VAN_CHUYEN' WHEN 3 THEN 'DA_VAN_CHUYEN' WHEN 4 THEN 'HOAN_THANH' ELSE 'DA_HUY' END,
    'COD',NOW(6),NOW(6),IF(n IN (1,4),NOW(6),NULL),UUID() FROM f6s_seed_links;
  INSERT INTO invoice_detail(id_invoice,id_product_detail,price,quantity,product_name,product_image,color_name,size_name,sku)
  SELECT i.id,l.detail_id,l.price,1,l.product_name,l.image,l.color_name,l.size_name,l.sku FROM f6s_seed_links l JOIN invoice i ON i.code=CONCAT('F6S-DEMO-HD-',l.n);
  INSERT INTO history_invoice_status(invoice_id,user_id,created_by,email,full_name,note,invoice_status,created_date)
  SELECT i.id,l.staff_id,'DEMO FIXTURE',CONCAT('f6s-demo-nv-',l.n,'@example.invalid'),CONCAT('[DEMO] Nhân viên ',l.person_name),'[DEMO] Trạng thái khởi tạo của dữ liệu mô phỏng',i.invoice_status,NOW(6)
  FROM f6s_seed_links l JOIN invoice i ON i.code=CONCAT('F6S-DEMO-HD-',l.n);
  -- Do not forge VNPAY receipts in history_pay: it is also used for callback deduplication.
  DROP TEMPORARY TABLE f6s_seed_links;
  DROP TEMPORARY TABLE f6s_seed_rows;

COMMIT;
