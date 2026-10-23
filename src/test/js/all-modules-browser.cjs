// Read-only checks for the isolated all-module demo installer.
const {chromium}=require('playwright');const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18083';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 try{
  const page=await browser.newPage();
  await page.goto(base+'/login');await page.locator('[name=username]').fill('admin@gmail.com');await page.locator('[name=password]').fill('123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  for(const role of ['ROLE_EMPLOYEE','ROLE_USER']){
   const r=await page.goto(base+'/adm-prj/qtht-001/user/list?role='+role+'&search=f6s-demo-&size=100');assert.equal(r.status(),200);
   assert.equal(await page.locator('tbody tr').filter({hasText:'@example.invalid'}).count(),5,role);
  }
  for(const route of ['invoice/list','invoice/create','customer/list','voucher/list','discount-product/list','banner/list','blog/list','contact/list']){
   const r=await page.goto(base+'/adm-prj/qtht-001/'+route,{waitUntil:'domcontentloaded'});assert.equal(r.status(),200,route);
   assert.doesNotMatch(await page.locator('body').innerText(),/Whitelabel Error Page|Internal Server Error/);
  }
  const ids=execFileSync('docker',['exec','f6s-mysql-review','mysql','-uroot','-N','f6s_shop_demo','-e',"SELECT id FROM invoice WHERE code LIKE 'F6S-DEMO-HD-%' ORDER BY id"],{encoding:'utf8'}).trim().split('\n');assert.equal(ids.length,5);
  for(const id of ids){const r=await page.goto(base+'/adm-prj/qtht-001/invoice/detail?id='+id);assert.equal(r.status(),200);assert.match(await page.locator('body').innerText(),/F6S-DEMO-HD-/);}
  const home=await page.goto(base+'/');assert.equal(home.status(),200);
  const links=await page.locator('a[href^="/product/f6s-demo-20261024-"]').evaluateAll(xs=>[...new Set(xs.map(x=>x.getAttribute('href')))]);assert.equal(links.length,5);
  for(const href of links){assert.equal((await page.goto(base+href)).status(),200);}
  console.log('PASS five employees and customers visible; 8 admin modules; 5 invoice details; 5 homepage banner/product routes');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
