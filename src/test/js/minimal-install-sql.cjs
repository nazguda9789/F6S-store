/* Fresh disposable database only. Each file uses a NEW connection, as separate phpMyAdmin imports do. */
const fs=require('node:fs'),{execFileSync,spawnSync}=require('node:child_process'),assert=require('node:assert/strict');
const container=process.env.F6S_MYSQL_CONTAINER||'f6s-mysql-review';
const db='f6s_minimal_'+Date.now()+'_demo';
const args=['exec','-i',container,'mysql','-uroot','-N'];
const query=s=>execFileSync('docker',args.concat(db),{input:s,encoding:'utf8'}).trim();
const files=['01_schema.sql','02_catalog_admin.sql','03_demo_optional.sql'].map(n=>fs.readFileSync('sql/install/'+n,'utf8').replaceAll('f6s_shop',db));
for(const sql of files){const clean=sql.replace(/--[^\n]*/g,'');assert.doesNotMatch(clean,/(?:^|;)\s*(CALL|DELIMITER|CREATE PROCEDURE|PREPARE|EXECUTE|SELECT\b|SIGNAL|DROP DATABASE|DROP TABLE|DELETE FROM)/i);assert.doesNotMatch(clean,/FOREIGN_KEY_CHECKS\s*=\s*(ON|OFF)/i);}
const run=s=>execFileSync('docker',args,{input:s,encoding:'utf8'}).trim();
// Resume the exact partial state reported from XAMPP; preserve existing banner rows.
const recoveryDb=db.replace('_demo','_recovery_demo');
const banner=files[0].match(/CREATE TABLE `banner` \([\s\S]*?;\n/)[0];
run(`CREATE DATABASE ${recoveryDb}; USE ${recoveryDb}; ${banner} INSERT INTO banner(id,title) VALUES(999,'keep existing banner');`);
const recovery=fs.readFileSync('sql/recovery/01_resume_banner_only.sql','utf8').replaceAll('f6s_shop',recoveryDb);
assert.equal(run(recovery),'');
assert.equal(run(`USE ${recoveryDb}; SELECT title FROM banner WHERE id=999;`),'keep existing banner');
assert.equal(Number(run(`SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${recoveryDb}'`)),38);
const createPart=files[0].split('-- All referenced tables now exist.')[0];
assert.doesNotMatch(createPart,/FOREIGN KEY/);
assert.ok((files[0].match(/ALTER TABLE .*ADD CONSTRAINT/g)||[]).length>10);
assert.equal(run(files[0]),'');assert.equal(Number(query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")),38);
// Missing catalog must fail BEFORE creating any demo accounts.
let error=spawnSync('docker',args,{input:files[2],encoding:'utf8'});assert.notEqual(error.status,0);assert.match(error.stderr,/foreign key constraint fails/i);assert.equal(Number(query('SELECT COUNT(*) FROM users')),0);
assert.equal(run(files[1]),'');assert.equal(Number(query('SELECT COUNT(*) FROM product')),25);assert.equal(Number(query('SELECT COUNT(*) FROM product_detail')),250);
assert.equal(Number(query('SELECT COUNT(*) FROM users')),1);assert.equal(Number(query('SELECT COUNT(*) FROM shoe_attribute WHERE code IS NULL')),0);
assert.equal(Number(query('SELECT SUM(stock) FROM product_detail')),2500);assert.equal(Number(query('SELECT COUNT(*) FROM invoice')),0);
assert.equal(run(files[2]),'');
for(const [table,n] of [['users',11],['voucher',5],['invoice',5],['invoice_detail',5],['order_draft',5],['order_detail_draft',5],['carts',5],['banner',5],['blog',5],['otp_codes',0],['pending_registration',0],['history_pay',0],['cloud_key',0],['key_telegram',0],['shopee_key',0]])assert.equal(Number(query('SELECT COUNT(*) FROM '+table)),n,table);
assert.equal(Number(query('SELECT COUNT(*) FROM product_detail WHERE color<>color_id OR size<>size_id')),0);assert.equal(Number(query('SELECT COUNT(DISTINCT sku) FROM product_detail')),250);
for(const file of [files[1],files[2]]){error=spawnSync('docker',args,{input:file,encoding:'utf8'});assert.notEqual(error.status,0);assert.match(error.stderr,/Duplicate entry/i);}
assert.equal(Number(query('SELECT COUNT(*) FROM product')),25);assert.equal(Number(query('SELECT COUNT(*) FROM users')),11);assert.equal(Number(query('SELECT SUM(stock) FROM product_detail')),2500);
assert.equal(Number(query("SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE()")),0);
console.log('PASS separate-file imports/no result sets, all 38 tables, 25 shoes/250 variants, optional fixtures, FK stage guard and duplicate-stage rejection; db='+db);
