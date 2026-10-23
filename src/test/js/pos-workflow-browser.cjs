/* Isolated seeded demo DB only: creates and deletes one draft; requires a verified ROLE_USER fixture. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage();let id;
 page.setDefaultTimeout(15000);
 const api=base+'/adm-prj/qtht-001/api/order-draft';
 try{
  await page.goto(base+'/login',{waitUntil:'domcontentloaded'});await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');
  await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  const customers=await(await page.request.get(api+'/customers')).json();assert.ok(customers.length>0,'Need a verified customer fixture');
  assert.ok(customers.length<=20);assert.ok(customers.every(c=>!('password' in c)&&!('rememberKey' in c)&&!('role' in c)));
  console.log('PASS restricted customer search');
  await page.goto(base+'/adm-prj/qtht-001/invoice/create',{waitUntil:'domcontentloaded'});
  const created=page.waitForResponse(r=>r.url()===api+'/new'&&r.request().method()==='POST');
  await page.locator('#pos-new-draft').click();const result=await(await created).json();assert.equal(result.success,true);id=result.data.id;
  await page.waitForFunction(expected=>document.getElementById('id-order').value===String(expected),id);
  console.log('PASS quick draft creation');
  await page.locator('#pos-choose-customer').click();await page.locator('.pos-customer-row button').first().click();
  assert.equal(await page.locator('#email').inputValue(),customers[0].email);
  const saved=page.waitForResponse(r=>r.url()===api+'/create'&&r.request().method()==='POST');
  await page.locator('button.btn-warning[onclick="createDonTam()"]').click();assert.equal((await(await saved).json()).success,true);
  if(await page.locator('.sweet-alert .confirm').isVisible())await page.locator('.sweet-alert .confirm').click();
  const persisted=await(await page.request.post(api+'/find-detail-order?orderId='+id)).json();assert.equal(persisted.data.email,customers[0].email);
  await page.locator('[data-bs-target="#pos-product-picker"]').click();
  const added=page.waitForResponse(r=>r.url().includes('/order-draft/add-to-order?')&&r.request().method()==='POST');
  await page.locator('.variant-row').filter({has:page.locator('.in-stock')}).first().click();
  assert.equal((await(await added).json()).success,true);
  await page.locator('#pos-product-picker .btn-close').click();await page.waitForSelector('#orderItemsTableBody tr');
  const lines=await(await page.request.post(api+'/find-detail-by-order?orderId='+id)).json();
  assert.ok(lines.length>0);const lineId=lines[0].id;
  const bad=await(await page.request.post(api+'/change-quantity?detailId='+lineId+'&quantity=-1')).json();assert.equal(bad.success,false);
  const actual=await(await page.request.post(api+'/find-detail-by-order?orderId='+id)).json();assert.equal(actual[0].quantity,1);
  const tampered=await(await page.request.post(api+'/create',{data:{id,fullName:'POS test',totalAmount:1,discount:0,shipCost:0}})).json();assert.equal(tampered.success,true);
  const total=await(await page.request.post(api+'/find-detail-order?orderId='+id)).json();assert.equal(total.data.totalAmount,lines.reduce((n,line)=>n+line.price*line.quantity,0));
  console.log('PASS quick draft -> select customer -> save/reload -> pick variant -> cart; rejects negative quantity and ignores forged total; customer response excludes secrets');
  const guest=await browser.newContext();const denied=await guest.request.get(api+'/customers',{maxRedirects:0});assert.ok([302,401,403].includes(denied.status()));await guest.close();
  console.log('PASS unauthenticated customer search blocked');
 } finally{
  if(id)assert.ok((await page.request.delete(api+'/delete-order?orderId='+id)).ok());
  await browser.close();
 }
})().catch(error=>{console.error(error.message);process.exitCode=1;});
