/* Fresh phpMyAdmin installer: ordinary SQL only, no CALL/multiple stored-procedure result sets. */
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8');
const destination=path.join(root,'sql/install');fs.mkdirSync(destination,{recursive:true});
const db='f6s_shop';
// Create parents and children before installing any FK; do not depend on session FK_CHECKS.
function deferForeignKeys(blocks){
 const alters=[];
 const tables=blocks.map(block=>{
  const table=block.match(/CREATE TABLE (?:IF NOT EXISTS )?`?(\w+)`?/)[1];
  return block.replace(/^  CONSTRAINT ([^\n]+)\n/gm,(_,clause)=>{
   assert.match(clause,/FOREIGN KEY/);
   alters.push(`ALTER TABLE \`${table}\` ADD CONSTRAINT ${clause.replace(/,$/,'')};`);
   return '';
  }).replace(/,\n\)/g,'\n)');
 });
 return tables.join('\n')+'\n-- All referenced tables now exist.\n'+alters.join('\n');
}
const header=stage=>`-- F6S Sports / ${stage}\n-- FRESH INSTALL ONLY. Use a new database; stop on any error. Never use mysql --force.\n-- No OTP, service keys or VNPAY payment receipts are fabricated.\nSET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;\nUSE ${db};\n`;
const schema=read('scripts/sql-templates/schema.sql').match(/^CREATE TABLE `[\s\S]*?;\n/gm);
assert.equal(schema.length,33,'Expected all 33 application tables');
const catalog=read('sql/maintenance/02_replace_catalog.sql');
const technical=catalog.split('\n').filter(l=>l.startsWith('CREATE TABLE ')).join('\n');
const business=read('scripts/sql-templates/business-demo.sql');
const demoTable=business.match(/CREATE TABLE IF NOT EXISTS demo_dataset_install \([\s\S]*?ENGINE=InnoDB;/)[0]
 .replace('description VARCHAR(500) NOT NULL','description VARCHAR(500) NOT NULL,\n  release_key VARCHAR(64) NOT NULL,\n  CONSTRAINT fk_demo_release FOREIGN KEY(release_key) REFERENCES catalog_release(release_key)');
const first=`-- F6S Sports / 01 CREATE NEW DATABASE AND TABLES\n-- Creates ${db}. Does NOT drop or overwrite an existing database.\n-- If it exists, STOP: use a new name in ALL THREE files, not IF NOT EXISTS or --force.\nSET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;\nCREATE DATABASE ${db} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\nUSE ${db};\nSET FOREIGN_KEY_CHECKS = 1;\n${deferForeignKeys([...schema,technical,demoTable])}\nSET FOREIGN_KEY_CHECKS = 1;\n`;
const start=catalog.indexOf('END IF;')+'END IF;'.length,end=catalog.indexOf("COMMIT;\nSELECT 'APPLIED:");assert.ok(start>7&&end>start);
let seed=catalog.slice(start,end).trim().split('\n').filter(l=>!/^INSERT INTO catalog_release_backup |^UPDATE \w+ SET (active|deleted|archived)=b'[01]';|^INSERT INTO catalog_release_cart_archive |^DELETE FROM carts;/.test(l)).join('\n');
const admin="INSERT INTO users(email,full_name,password,role,user_type,actived,deleted,activation_status,account_non_locked,failed_attempts,is_google_user,created_date) VALUES('admin@gmail.com','Quản trị F6S','$2a$10$.VIihhdGDaZOJY9xzjwzVeepgVxwx2EkZaoMrCVoWtBIbltUJouWa','ROLE_ADMIN','NORMAL',b'1',b'0',b'1',b'1',0,b'0',NOW(6));";
const second=header('02 CATALOG + LOCAL ADMIN')+`-- Run once AFTER 01. Admin: admin@gmail.com / 123123; change before public use.\nSTART TRANSACTION;\nINSERT INTO catalog_release(release_key) VALUES('sports-20261024');\n${admin}\n${seed}\nCOMMIT;\n`;
const demoStart=business.indexOf('  DROP TEMPORARY TABLE IF EXISTS f6s_seed_rows;'),demoEnd=business.indexOf("  COMMIT;\n  SELECT 'INSTALLED:");assert.ok(demoStart>0&&demoEnd>demoStart);
let demo=business.slice(demoStart,demoEnd).replace(/  IF \(SELECT COUNT\(\*\) FROM f6s_seed_links\) <> 5 THEN[\s\S]*?  END IF;\n/,'');
const third=header('03 OPTIONAL BUSINESS DEMO')+`-- Optional: five staff/customers, vouchers, carts and synthetic invoices. Never use real-store reports.\n-- Accounts are inactive, with no shared password. No banking/payment proof is inserted.\nSTART TRANSACTION;\nINSERT INTO demo_dataset_install(dataset_key,description,release_key) VALUES('all-modules-sports-20261024','Synthetic business fixtures, not real orders/payments','sports-20261024');\n${demo}\nCOMMIT;\n`;
for(const [name,sql] of [['01_schema.sql',first],['02_catalog_admin.sql',second],['03_demo_optional.sql',third]]){
 const clean=sql.replace(/--[^\n]*/g,'');
 assert.doesNotMatch(clean,/(?:^|;)\s*(DELIMITER|CALL|PREPARE|EXECUTE|SIGNAL|SELECT\b|IF\s|DECLARE|CREATE PROCEDURE|DELETE FROM|DROP DATABASE|DROP TABLE)/i,name);
 fs.writeFileSync(path.join(destination,name),sql);
}
// Recovery for the reported interrupted import: ONLY banner exists. No existing table is altered.
const recoveryDir=path.join(root,'sql/recovery');fs.mkdirSync(recoveryDir,{recursive:true});
const recovery=first.replace(/^--[^\n]*\n/gm,'')
 .replace(/^CREATE DATABASE [^\n]+\n/m,'')
 .replace(/CREATE TABLE `banner` \([\s\S]*?;\n/,'');
fs.writeFileSync(path.join(recoveryDir,'01_resume_banner_only.sql'),
 '-- ONLY for interrupted installs where SHOW TABLES lists banner and nothing else.\n-- Backup first. Do NOT run 01_schema.sql as well. Then import 02 and optionally 03.\n'+recovery);
console.log('Built fresh installer and banner-only recovery; all tables precede foreign keys');
