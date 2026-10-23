/* Run ONLY against a disposable database: commits one sale and changes stock.
 * Intentionally keeps the invoice for inspection. No real payment or email. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL;
if (!base || process.env.F6S_DISPOSABLE_DB !== '1') throw new Error('Set F6S_BASE_URL and F6S_DISPOSABLE_DB=1 for an isolated fixture database.');
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage();
 try {
  await page.goto(base+'/login');
  await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');
  await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');
  await page.locator('.btn-login-main').click(); await page.waitForURL('**/adm-prj/**');
  const api=base+'/adm-prj/qtht-001/api/order-draft';
  const post=async(path,data)=>{const r=await page.request.post(api+path,data ? {data}:{});assert.equal(r.status(),200);return r.json();};
  const created=await post('/new'); assert.equal(created.success,true);const id=created.data.id;
  assert.equal((await post('/add-to-order?orderId='+id+'&detailId=239')).success,true);
  const url='/save-order-to-invoice?orderId='+id+'&invoiceType=TAI_QUAY&invoiceStatus=HOAN_THANH';
  const results=await Promise.all([post(url),post(url)]);
  assert.equal(results.filter(r=>r.success).length,1,'Concurrent requests must produce exactly one invoice');
  assert.equal((await post(url)).success,false,'Repeated checkout must not produce another invoice');
  assert.equal((await post('/find-detail-order?orderId='+id)).success,false);
  assert.equal((await post('/add-to-order?orderId='+id+'&detailId=239')).success,false);
  console.log('PASS concurrent checkout exactly once, draft closed, repeated checkout and modification rejected; draft='+id);
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
