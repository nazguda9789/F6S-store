// Integration test against an already imported, disposable MySQL demo database.
// Usage: node src/test/js/all-modules-sql.cjs
const {execFileSync,spawnSync}=require('node:child_process');
const fs=require('node:fs');const assert=require('node:assert/strict');
const db=process.env.F6S_DEMO_DATABASE || 'f6s_shop_demo';
const container=process.env.F6S_MYSQL_CONTAINER || 'f6s-mysql-review';
assert.match(db,/^[a-zA-Z0-9_]+_demo$/);
const args=['exec','-i',container,'mysql','-uroot','-N',db];
const query=sql=>execFileSync('docker',args,{input:sql,encoding:'utf8'}).trim();
const count=(table,where)=>Number(query(`SELECT COUNT(*) FROM ${table} WHERE ${where};`));
const expected=[
 ['users',"email LIKE 'f6s-demo-nv-%@example.invalid'",5],
 ['users',"email LIKE 'f6s-demo-kh-%@example.invalid'",5],
 ['voucher',"code LIKE 'F6S-DEMO-VC-%'",5],
 ['banner',"title LIKE '[DEMO]%'",5],
 ['blog',"slug LIKE 'f6s-demo-bai-viet-%'",5],
 ['contact',"email LIKE 'f6s-demo-kh-%@example.invalid'",5],
 ['chatting',"content LIKE '[DEMO]%'",5],
 ['product_view_history',"session_id LIKE 'F6S-DEMO-SESSION-%'",5],
 ['order_draft',"note LIKE 'F6S-DEMO-DRAFT-%'",5],
 ['order_detail_draft',"order_draft_id IN (SELECT id FROM order_draft WHERE note LIKE 'F6S-DEMO-DRAFT-%')",5],
 ['invoice',"code LIKE 'F6S-DEMO-HD-%'",5],
 ['invoice_detail',"id_invoice IN (SELECT id FROM invoice WHERE code LIKE 'F6S-DEMO-HD-%')",5],
 ['history_invoice_status',"invoice_id IN (SELECT id FROM invoice WHERE code LIKE 'F6S-DEMO-HD-%')",5],
 ['discount_product',"user_id IN (SELECT id FROM users WHERE email LIKE 'f6s-demo-nv-%@example.invalid')",5]
];
function check(){for(const [table,where,n] of expected)assert.equal(count(table,where),n,table);}
check();
assert.equal(count('users',"email LIKE 'f6s-demo-%@example.invalid' AND (actived<>b'0' OR activation_status<>b'0' OR password IS NOT NULL OR role='ROLE_ADMIN')"),0);
assert.equal(Number(query("SELECT COUNT(*) FROM invoice i JOIN (SELECT id_invoice,SUM(price*quantity) total FROM invoice_detail GROUP BY id_invoice) d ON d.id_invoice=i.id WHERE i.code LIKE 'F6S-DEMO-HD-%' AND ABS(i.total_amount-(d.total-i.discount+i.ship_cost))>0.001;")),0);
assert.equal(Number(query("SELECT COUNT(*) FROM order_draft o JOIN (SELECT order_draft_id,SUM(price*quantity) total FROM order_detail_draft GROUP BY order_draft_id) d ON d.order_draft_id=o.id WHERE o.note LIKE 'F6S-DEMO-DRAFT-%' AND ABS(o.total_amount-d.total)>0.001;")),0);
assert.equal(Number(query("SELECT COUNT(*) FROM banner WHERE title LIKE '[DEMO]%' AND (page_name<>'/' OR link_url NOT LIKE '/product/%');")),0);
assert.equal(Number(query("SELECT COUNT(*) FROM product_detail pd JOIN catalog_demo_seed s ON s.product_id=pd.product_id WHERE pd.stock<>20;")),0);
const tables=['users','invoice','invoice_detail','order_draft','order_detail_draft','voucher','history_pay','product_detail'];
const snapshot=()=>tables.map(t=>query(`SELECT COUNT(*) FROM ${t};`)).join(',');
const before=snapshot();
const result=execFileSync('docker',args,{input:fs.readFileSync('sql/du_lieu_mau_toan_bo_20261024.sql'),encoding:'utf8'});
assert.match(result,/ALREADY_INSTALLED_NO_CHANGES/);check();assert.equal(snapshot(),before);
// Wrong-database guard: separate disposable schema, never the live store.
execFileSync('docker',['exec',container,'mysql','-uroot','-e','CREATE DATABASE IF NOT EXISTS f6s_seed_guard_check']);
const denied=spawnSync('docker',['exec','-i',container,'mysql','-uroot','f6s_seed_guard_check'],{input:fs.readFileSync('sql/du_lieu_mau_toan_bo_20261024.sql'),encoding:'utf8'});
assert.notEqual(denied.status,0);assert.match(denied.stderr,/DEMO ONLY/);
console.log('PASS 14 fixture groups, disabled accounts, invoice/draft totals, banner routes, unchanged stock, idempotent import and non-demo rejection');
