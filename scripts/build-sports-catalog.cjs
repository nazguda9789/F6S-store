/* Generated SQL/images are build artifacts. Source configuration below is independent of old product data.
 * One-time schema extraction: node scripts/build-sports-catalog.cjs extract <isolated-review-db>
 * Rebuild offline: node scripts/build-sports-catalog.cjs build
 */
const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'..'),file=p=>path.join(root,p);
const tables=['banner','blog','carts','catalog_variant_merge','category','chatting','cloud_key','color','contact','discount_product','history_invoice_status','history_pay','invoice','invoice_detail','key_telegram','order_detail_draft','order_draft','otp_codes','parent_category','pending_registration','product','product_category','product_color_image','product_detail','product_image','product_view_history','province','shopee_key','shoe_attribute','size','users','voucher','wards'];
if(process.argv[2]==='extract'){
 const db=process.argv[3];if(!/^[a-zA-Z0-9_]+_demo$/.test(db))throw Error('Use a disposable _demo schema only');
 let schema=execFileSync('docker',['exec','f6s-mysql-review','mysqldump','-uroot','--no-data','--no-tablespaces','--skip-comments','--skip-add-drop-table',db,...tables],{encoding:'utf8'});
 schema=schema.replace(/ AUTO_INCREMENT=\d+/g,'');
 fs.writeFileSync(file('scripts/sql-templates/schema.sql'),'-- Generated schema only; no user, token, key, transaction or old product data.\n'+schema.trimEnd()+'\n');
 console.log('Extracted schema only: '+tables.length+' application tables');process.exit(0);
}
if(process.argv[2]!=='build')throw Error('Use extract or build');
const q=s=>s===null?'NULL':"'"+String(s).replace(/'/g,"''")+"'";
const groups=[['Chạy bộ','chay-bo','Velocity Run','#16a085'],['Tập luyện','tap-luyen','Power Train','#7257cf'],['Bóng rổ','bong-ro','Court Jump','#ea8b3d'],['Đi bộ','di-bo','Daily Walk','#2d85b7'],['Sneaker','sneaker','Urban Move','#cd547d']];
const brands=['Nike','Adidas','Puma','New Balance','Converse'];
const soles=['Cao su','EVA','Phylon','TPU','Foam'];
const materials=['Lưới Mesh','Da tổng hợp','Vải Knit','Canvas','Da lộn'];
const colors=[['Đen','#172033'],['Trắng','#F7F8FA'],['Xám','#8A94A6'],['Xanh navy','#203E69'],['Đỏ','#C73E4E']];
const sizes=['38','39','40','41','42'];
const release='sports-20261024';
const sql=[];const add=s=>sql.push(s);
add(`-- REPLACES THE CURRENT CATALOG. Stop web and back up first. Does NOT delete products, history, OTP, keys or users.\nSET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;`);
add(`SET @sports_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='blog' AND column_name='archived')=0,'ALTER TABLE blog ADD COLUMN archived BIT NOT NULL DEFAULT 0','SELECT 1'); PREPARE sports_stmt FROM @sports_ddl; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;`);
add(`CREATE TABLE IF NOT EXISTS catalog_release(release_key VARCHAR(64) PRIMARY KEY,applied_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),state VARCHAR(20) NOT NULL DEFAULT 'APPLIED') ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS catalog_release_item(release_key VARCHAR(64) NOT NULL,kind VARCHAR(32) NOT NULL,ordinal INT NOT NULL,record_id BIGINT NOT NULL,PRIMARY KEY(release_key,kind,ordinal),UNIQUE KEY uk_release_record(release_key,kind,record_id)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS catalog_release_backup(release_key VARCHAR(64) NOT NULL,table_name VARCHAR(32) NOT NULL,record_id BIGINT NOT NULL,old_flag BIT NULL,PRIMARY KEY(release_key,table_name,record_id)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS catalog_release_cart_archive LIKE carts;
DELIMITER $$
DROP PROCEDURE IF EXISTS apply_sports_catalog$$
CREATE PROCEDURE apply_sports_catalog()
main: BEGIN
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
START TRANSACTION;
INSERT IGNORE INTO catalog_release(release_key) VALUES('${release}');
IF ROW_COUNT()=0 THEN COMMIT; SELECT 'ALREADY_APPLIED_NO_CHANGES' AS result; LEAVE main; END IF;`);
const flags=[['product','active',0],['product_detail','active',0],['shoe_attribute','active',0],['category','deleted',1],['color','deleted',1],['size','deleted',1],['banner','active',0],['blog','archived',1]];
for(const [t,c,retire] of flags){add(`INSERT INTO catalog_release_backup SELECT '${release}','${t}',id,${c} FROM ${t};\nUPDATE ${t} SET ${c}=b'${retire}';`);}
add('INSERT INTO catalog_release_cart_archive SELECT * FROM carts;\nDELETE FROM carts;');
const register=(kind,n,expr)=>add(`INSERT INTO catalog_release_item VALUES('${release}','${kind}',${n},${expr});`);
for(const [kind,names] of [['BRAND',brands],['SOLE',soles],['MATERIAL',materials]])names.forEach((name,i)=>{
 add(`INSERT INTO shoe_attribute(kind,name,active,description) VALUES('${kind}',${q(name)},b'1','Thuộc tính catalog thể thao; dữ liệu trình diễn, cần xác minh hàng thật.') ON DUPLICATE KEY UPDATE active=b'1';`);
 register(kind,i+1,`(SELECT id FROM shoe_attribute WHERE kind='${kind}' AND name=${q(name)})`);
});
colors.forEach(([name,code],i)=>{add(`INSERT INTO color(name,code,deleted) VALUES(${q(name)},${q(code)},b'0');`);register('COLOR',i+1,'LAST_INSERT_ID()');});
sizes.forEach((name,i)=>{add(`INSERT INTO size(name,deleted) VALUES(${q(name)},b'0');`);register('SIZE',i+1,'LAST_INSERT_ID()');});
const id=(kind,n)=>`(SELECT record_id FROM catalog_release_item WHERE release_key='${release}' AND kind='${kind}' AND ordinal=${n})`;
fs.mkdirSync(file('src/main/webapp/image/sports'),{recursive:true});
groups.forEach(([name,slug,series,color],g)=>{
 const image='/image/sports/'+slug+'.svg';
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 620"><rect width="1000" height="620" rx="28" fill="#eef2f6"/><circle cx="730" cy="210" r="160" fill="${color}" opacity=".12"/><text x="58" y="90" font-family="Arial" font-size="44" font-weight="700" fill="#14263e">F6S / ${slug.toUpperCase()}</text><path d="M220 370 L280 224 Q300 200 325 235 L410 295 L600 325 Q650 335 695 375 L820 414 Q845 445 805 465 L246 465 Q186 452 220 370Z" fill="${color}"/><path d="M220 422 Q400 442 820 414 L825 460 Q800 493 240 486 Q200 480 220 422Z" fill="#fff" stroke="#25354a" stroke-width="7"/><path d="M340 290 L480 340 M365 270 L505 320 M390 290 L530 340" stroke="#fff" stroke-width="12"/><text x="58" y="565" font-family="Arial" font-size="23" fill="#546379">DEMO / Anh minh hoa — khong phai anh hang hoa thuc</text></svg>`;
 fs.writeFileSync(file('src/main/webapp'+image),svg+'\n');
 add(`INSERT INTO category(name,slug,is_parent,deleted,position,image,created_date,update_date,meta_description) VALUES(${q('Giày '+name.toLowerCase())},${q('sports-'+slug)},b'1',b'0',${g+1},${q(image)},NOW(6),NOW(6),'Danh mục giày thể thao mới.');`);register('CATEGORY',g+1,'LAST_INSERT_ID()');
 for(let n=1;n<=5;n++){
  const ordinal=g*5+n,price=590000+g*100000+(n-1)*50000,pname=`[MẪU] ${series} ${String(n).padStart(2,'0')}`;
  const pslug='sports-'+slug+'-'+n;
  add(`INSERT INTO product(name,slug,brand_id,sole_id,material_id,price,ole_price,weight,active,sold,image,description,short_description,meta_title,meta_description,created_at,updated_at) VALUES(${q(pname)},${q(pslug)},${id('BRAND',n)},${id('SOLE',g+1)},${id('MATERIAL',n)},${price},${price+100000},0.65,b'1',0,${q(image)},'<h2>Giày thể thao minh họa</h2><p>Catalog mới theo cấu trúc quản lý giày: thương hiệu, danh mục, đế, chất liệu, màu và size. Đây là dữ liệu DEMO, không phải mẫu giày thương mại được hãng xác nhận. Cần thay ảnh, thông số và tồn thực trước khi kinh doanh.</p>','Mẫu trình diễn, ảnh minh họa; không phải thông tin hàng hóa thật.','F6S SPORTS CATALOG 20261024','Mẫu giày thể thao trình diễn, không xác nhận hàng hóa thương mại.',NOW(6),NOW(6));`);register('PRODUCT',ordinal,'LAST_INSERT_ID()');
  add(`INSERT INTO product_category(product_id,category_id) VALUES(${id('PRODUCT',ordinal)},${id('CATEGORY',g+1)});`);
  for(const c of [g+1,(g+1)%5+1]){
   for(let s=1;s<=5;s++)add(`INSERT INTO product_detail(product_id,color_id,color,size_id,size,stock,price,old_price,active) VALUES(${id('PRODUCT',ordinal)},${id('COLOR',c)},${id('COLOR',c)},${id('SIZE',s)},${id('SIZE',s)},10,${price},${price+100000},b'1');`);
   add(`INSERT INTO product_color_image(product_id,color_id,image) VALUES(${id('PRODUCT',ordinal)},${id('COLOR',c)},${q(image)});`);
  }
  add(`INSERT INTO product_image(product_id,link_image) VALUES(${id('PRODUCT',ordinal)},${q(image)});`);
 }
 add(`INSERT INTO banner(title,image_url,link_url,page_name,banner_type,active,display_order,created_date,update_date) VALUES(${q('F6S — Giày '+name.toLowerCase())},${q(image)},${q('/category/sports-'+slug)},'/','SLIDER',b'1',${g+1},NOW(6),NOW(6));`);register('BANNER',g+1,'LAST_INSERT_ID()');
 add(`INSERT INTO blog(title,slug,description,content,image_banner,seo_title,meta_description,num_view,created_date,update_date,archived) VALUES(${q('Cẩm nang chọn giày '+name.toLowerCase())},${q('sports-cam-nang-'+slug)},'Hướng dẫn chọn giày phù hợp nhu cầu vận động.','<h2>Chọn giày theo hoạt động</h2><p>Kiểm tra độ vừa chân, bảng size riêng của hãng và mặt sân sử dụng. Nên thử cả hai chân, dành khoảng trống đầu ngón phù hợp.</p><p>Vệ sinh nhẹ nhàng, hong khô tự nhiên; không dùng thông số minh họa trong catalog để thay hướng dẫn của nhà sản xuất.</p>',${q(image)},${q('Cẩm nang giày '+name.toLowerCase())},'Cẩm nang thể thao F6S.',0,NOW(6),NOW(6),b'0');`);register('BLOG',g+1,'LAST_INSERT_ID()');
});
add(`UPDATE product p JOIN catalog_release_item r ON r.record_id=p.id AND r.kind='PRODUCT' AND r.release_key='${release}' SET p.code=CONCAT('F6S-SP',LPAD(CAST(p.id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(p.id AS CHAR))),'0'));
UPDATE product_detail d JOIN catalog_release_item r ON r.record_id=d.product_id AND r.kind='PRODUCT' AND r.release_key='${release}' SET d.sku=CONCAT('F6S-SKU',LPAD(CAST(d.id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(d.id AS CHAR))),'0'));
UPDATE shoe_attribute SET code=CONCAT('F6S-',CASE kind WHEN 'BRAND' THEN 'TH' WHEN 'SOLE' THEN 'DE' ELSE 'CL' END,LPAD(CAST(id AS CHAR),GREATEST(6,CHAR_LENGTH(CAST(id AS CHAR))),'0')) WHERE code IS NULL;
COMMIT;
SELECT 'APPLIED: 25 new shoes, 250 variants. Old catalog retired, NOT deleted.' AS result;
END$$
CALL apply_sports_catalog()$$
DROP PROCEDURE apply_sports_catalog$$
DELIMITER ;`);
fs.writeFileSync(file('sql/maintenance/02_replace_catalog.sql'),sql.join('\n')+'\n');
const undo=[`-- Restore prior visibility flags only. Stop web, back up first; no inventory/history is deleted.\nDELIMITER $$\nDROP PROCEDURE IF EXISTS revert_sports_catalog$$\nCREATE PROCEDURE revert_sports_catalog()\nmain: BEGIN\nDECLARE release_state VARCHAR(20);\nDECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;\nSTART TRANSACTION;\nSELECT state INTO release_state FROM catalog_release WHERE release_key='${release}' FOR UPDATE;\nIF release_state IS NULL OR release_state<>'APPLIED' THEN COMMIT; SELECT 'NOT_APPLIED_OR_ALREADY_REVERTED' AS result; LEAVE main; END IF;`];
for(const [t,c] of flags)undo.push(`UPDATE ${t} t JOIN catalog_release_backup b ON b.record_id=t.id AND b.table_name='${t}' AND b.release_key='${release}' SET t.${c}=b.old_flag;`);
const mappings=[['PRODUCT','product','active',0],['CATEGORY','category','deleted',1],['COLOR','color','deleted',1],['SIZE','size','deleted',1],['BANNER','banner','active',0],['BLOG','blog','archived',1],['BRAND','shoe_attribute','active',0],['SOLE','shoe_attribute','active',0],['MATERIAL','shoe_attribute','active',0]];
for(const [kind,t,c,hidden] of mappings)undo.push(`UPDATE ${t} t JOIN catalog_release_item r ON r.record_id=t.id AND r.kind='${kind}' AND r.release_key='${release}' SET t.${c}=b'${hidden}' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='${release}' AND b.table_name='${t}' AND b.record_id=t.id);`);
undo.push(`UPDATE product_detail d JOIN catalog_release_item r ON r.record_id=d.product_id AND r.kind='PRODUCT' AND r.release_key='${release}' SET d.active=b'0' WHERE NOT EXISTS(SELECT 1 FROM catalog_release_backup b WHERE b.release_key='${release}' AND b.table_name='product_detail' AND b.record_id=d.id);\nINSERT IGNORE INTO carts SELECT * FROM catalog_release_cart_archive;\nUPDATE catalog_release SET state='ROLLED_BACK' WHERE release_key='${release}';\nCOMMIT;\nEND$$\nCALL revert_sports_catalog()$$\nDROP PROCEDURE revert_sports_catalog$$\nDELIMITER ;`);
fs.writeFileSync(file('sql/maintenance/03_restore_previous_catalog.sql'),undo.join('\n')+'\n');
console.log('Built catalog maintenance scripts and five illustrative assets; build-minimal-sql.cjs generates the installer');
