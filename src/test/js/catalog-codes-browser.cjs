/* Disposable demo DB only: creates and removes one product and two variants. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL||'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage(); let id;
 const api=base+'/adm-prj/qtht-001/api/product';
 try{
  await page.goto(base+'/login',{waitUntil:'domcontentloaded'});
  await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL||'admin@gmail.com');
  await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD||'123123');
  await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  await page.goto(base+'/adm-prj/qtht-001/product/save',{waitUntil:'domcontentloaded'});
  const sizes=await page.locator('.select-size').first().locator('option').evaluateAll(options=>options.map(o=>Number(o.value)).filter(Boolean));
  const colors=await page.locator('.select-color').first().locator('option').evaluateAll(options=>options.map(o=>Number(o.value)).filter(Boolean));
  assert.ok(sizes.length>=2&&colors.length>0);
  const body={name:'Catalog code test '+Date.now(),slug:'catalog-code-'+Date.now(),price:100000,oldPrice:120000,weight:0.3,active:true,image:'/image/f6s-placeholder.svg',categoryIds:[],imageLinks:[],details:[{colorId:colors[0],sizeId:sizes[0],stock:2,price:100000,sku:'FORGED'},{colorId:colors[0],sizeId:sizes[1],stock:3,price:110000,sku:'FORGED'}]};
  const created=await page.request.post(api+'/save',{data:body});assert.equal(created.status(),200);
  const product=await created.json();id=product.id;assert.equal(product.code,'F6S-SP'+String(id).padStart(6,'0'));
  let details=await(await page.request.get(api+'/find-by-id?id='+id)).json();
  assert.equal(details.productDetails.length,2);
  assert.ok(details.productDetails.every(d=>d.sku==='F6S-SKU'+String(d.id).padStart(6,'0')));
  assert.equal(new Set(details.productDetails.map(d=>d.sku)).size,2);
  const original=details.productDetails[0];
  const changed=await page.request.post(api+'/update-detail',{data:{...original,productId:999,sku:'CHANGED',stock:4,price:120000}});
  assert.equal(changed.status(),200);const edited=await changed.json();assert.equal(edited.productId,id);assert.equal(edited.sku,original.sku);
  const renamed=await page.request.post(api+'/update?id='+id,{data:{...body,name:'Renamed catalog item',details:[]}});assert.equal(renamed.status(),201);
  assert.equal((await renamed.json()).code,product.code);
  const filtered=await(await page.request.post(api+'/filter',{data:{search:original.sku}})).json();assert.ok(filtered.content.some(p=>p.id===id));
  const publicResult=await(await page.request.get(base+'/api/v1/products/search-by-param?search='+original.sku)).json();assert.ok(publicResult.content.some(p=>p.id===id));
  await page.goto(base+'/adm-prj/qtht-001/product/save?id='+id,{waitUntil:'domcontentloaded'});
  assert.equal(await page.locator('#sku'+original.id).getAttribute('readonly'),'');
  const duplicate=await page.request.post(api+'/save',{data:{...body,details:[body.details[0],body.details[0]]}});assert.equal(duplicate.status(),400);
  console.log('PASS server-generated product/variant codes, unique variants, rename stability, immutable parent/SKU, SKU search and duplicate combination rejection');
 }finally{
  if(id)assert.ok((await page.request.delete(api+'/delete-product?id='+id)).ok());
  await browser.close();
 }
})().catch(error=>{console.error(error.message);process.exitCode=1;});
