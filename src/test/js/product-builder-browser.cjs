/* Disposable demo DB: creates one product from the real form and removes it. */
const {chromium}=require('playwright'); const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:1440,height:1000}});let id;
 try{
  await page.goto(base+'/login');await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  const api=base+'/adm-prj/qtht-001/api/product';
  await page.goto(base+'/adm-prj/qtht-001/product/list',{waitUntil:'domcontentloaded'});
  await page.locator('.bee-product-nav-group > a').hover();assert.equal(await page.locator('#collapseProducts').isVisible(),true);
  await page.locator('[data-bs-target="#collapseProductAttributes"]').click();await page.locator('#collapseProductAttributes.show').waitFor({state:'visible'});
  await page.goto(base+'/adm-prj/qtht-001/product/save',{waitUntil:'domcontentloaded'});
  await page.waitForSelector('.bee-product-builder');
  assert.equal(await page.locator('#formadd').count(),1);
  await page.locator('#name').fill('Matrix test '+Date.now());await page.locator('#name').press('End');
  await page.locator('#giaban').fill('120000');await page.locator('#giacu').fill('150000');await page.locator('#weight').fill('0.3');await page.locator('#soluongtohop').fill('3');
  const values=async selector=>page.locator(selector+' option').evaluateAll(o=>o.map(x=>x.value).filter(Boolean).slice(0,2));
  const colors=await values('#colorList'),sizes=await values('#sizeList');assert.equal(colors.length,2);assert.equal(sizes.length,2);
  await page.locator('#colorList').selectOption(colors,{force:true});await page.locator('#sizeList').selectOption(sizes,{force:true});
  const sizePicker=page.locator('.bee-multi-picker').filter({has:page.locator('#sizeList')});
  if(await sizePicker.count()){
   assert.equal(await sizePicker.locator('.bee-selection-chip').count(),2);
   await sizePicker.locator('.bee-selection-chip button').first().click();
   assert.equal(await page.locator('#sizeList').evaluate(s=>s.selectedOptions.length),1);
   await page.locator('#sizeList').selectOption(sizes,{force:true});
  }
  await page.locator('button[onclick="setToHop()"]').click();
  await page.waitForFunction(()=>document.querySelectorAll('#ctnctsp .bee-variant-group').length===2);
  assert.equal(await page.locator('#ctnctsp .tr-chitietsanpham').count(),4);
  await page.locator('#ctnctsp .stock-detail').first().fill('7');await page.locator('button[onclick="setToHop()"]').click();
  assert.equal(await page.locator('#ctnctsp .tr-chitietsanpham').count(),4);assert.equal(await page.locator('#ctnctsp .stock-detail').first().inputValue(),'7');
  const details=await page.evaluate(()=>getProductDetailsFromUI());assert.equal(details.length,4,'Hidden template must not be submitted');
  if(process.env.F6S_SCREENSHOT) { await page.evaluate(()=>window.scrollTo(0,0)); await page.screenshot({path:process.env.F6S_SCREENSHOT,fullPage:true}); }
  assert.equal(new Set(details.map(d=>d.colorId+':'+d.sizeId)).size,4);assert.ok(details.every(d=>Number(d.price)===120000));
  await page.locator('#ctnctsp .tr-chitietsanpham button[onclick="deleteItem(this)"]').last().click();
  assert.equal(await page.locator('#ctnctsp .tr-chitietsanpham').count(),3);
  const saved=page.waitForResponse(r=>r.url()===api+'/save'&&r.request().method()==='POST');
  await page.locator('.bee-builder-submit').click();const response=await saved;assert.equal(response.status(),200);id=(await response.json()).id;
  const product=await(await page.request.get(api+'/find-by-id?id='+id)).json();assert.equal(product.productDetails.length,3);
  assert.ok(product.productDetails.every(d=>d.sku.startsWith('F6S-SKU')));
  await page.goto(base+'/adm-prj/qtht-001/product/save?id='+id,{waitUntil:'domcontentloaded'});await page.waitForSelector('.bee-variant-group');
  assert.equal(await page.locator('#ctnctsp .select-size').count(),3);assert.equal(await page.evaluate(()=>getProductDetailsFromUI().length),0,'Persisted rows must not be reinserted as new variants');
  console.log('PASS real form: 2 colors x 2 sizes, grouping, no duplicate/hidden template, preserves edits, row deletion, save and reload server SKUs');
 }finally{if(id)assert.ok((await page.request.delete(base+'/adm-prj/qtht-001/api/product/delete-product?id='+id)).ok());await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
