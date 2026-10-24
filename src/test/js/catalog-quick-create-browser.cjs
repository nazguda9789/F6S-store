/* Disposable demo database only. Creates six attributes and one product using the real UI. */
const {chromium}=require('playwright');const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18084';
const db=process.env.F6S_DEMO_DATABASE || 'f6s_shop_demo';assert.match(db,/^[a-zA-Z0-9_]+_demo$/);
const sql=s=>execFileSync('docker',['exec','-i','f6s-mysql-review','mysql','-uroot','-N',db],{input:s,encoding:'utf8'});
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage();let productId;
 const url=base+'/adm-prj/qtht-001/shoe-attributes/quick-create';
 try{
  const anonymous=await browser.newContext();const denied=await anonymous.request.post(url,{data:{group:'SIZE',name:'123'},maxRedirects:0});assert.ok([302,401,403].includes(denied.status()));await anonymous.close();
  await page.goto(base+'/login');await page.locator('[name=username]').fill('admin@gmail.com');await page.locator('[name=password]').fill('123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  await page.goto(base+'/adm-prj/qtht-001/product/save');await page.waitForSelector('[data-quick-group="BRAND"]');
  assert.equal(await page.locator('.catalog-quick-add').count(),6);
  const stamp=Date.now();await page.locator('#name').fill('Quick-create shoe '+stamp);await page.locator('#giaban').fill('200000');await page.locator('#giacu').fill('250000');await page.locator('#weight').fill('0.4');await page.locator('#soluongtohop').fill('2');
  const options={};
  for(const [group,control] of [['CATEGORY','categoryIds'],['BRAND','brandId'],['SOLE','soleId'],['MATERIAL','materialId'],['COLOR','colorList'],['SIZE','sizeList']]){
   await page.locator('[data-quick-group="'+group+'"]').click();const dialog=page.locator('.catalog-quick-dialog');await dialog.locator('[name=name]').fill(group==='SIZE'?'45.'+stamp:'Quick '+group+' '+stamp);
   if(group==='CATEGORY')await dialog.locator('[name=slug]').fill('quick-category-'+stamp);
   if(group==='COLOR')await dialog.locator('[name=code]').fill('#123abc');
   const saved=page.waitForResponse(r=>r.url()===url && r.request().method()==='POST');await dialog.locator('[type=submit]').click();const r=await saved;assert.equal(r.status(),201,group+': '+await r.text());options[group]=await r.json();
   await dialog.waitFor({state:'hidden'});assert.equal(await page.locator('#'+control).evaluate(x=>x.selectedOptions[x.selectedOptions.length-1].value),String(options[group].id));
   assert.equal(await page.locator('#name').inputValue(),'Quick-create shoe '+stamp);assert.equal(await page.locator('#giaban').inputValue(),'200000');
  }
  // Duplicate validation leaves modal and product draft in place.
  await page.locator('[data-quick-group="BRAND"]').click();await page.locator('.catalog-quick-dialog [name=name]').fill(options.BRAND.name);
  const rejected=page.waitForResponse(r=>r.url()===url);await page.locator('.catalog-quick-dialog [type=submit]').click();assert.equal((await rejected).status(),400);
  await page.locator('.catalog-quick-dialog [data-error]').filter({hasText:'tồn tại'}).waitFor();await page.locator('.catalog-quick-dialog [data-cancel]').click();
  // Newly created root is available as parent without reloading.
  await page.locator('[data-quick-group="CATEGORY"]').click();assert.equal(await page.locator('.catalog-quick-dialog [name=parentId] option[value="'+options.CATEGORY.id+'"]').count(),1);await page.locator('.catalog-quick-dialog [data-cancel]').click();
  const invalid=await page.request.post(url,{data:{group:'COLOR',name:'Bad '+stamp,code:'javascript:bad'}});assert.equal(invalid.status(),400);
  assert.equal((await page.request.post(url,{data:{group:'INVALID',name:'Bad'}})).status(),400);
  await page.locator('button[onclick="setToHop()"]').click();await page.waitForSelector('#ctnctsp .tr-chitietsanpham');
  assert.equal(await page.locator('#ctnctsp .select-color').inputValue(),String(options.COLOR.id));assert.equal(await page.locator('#ctnctsp .select-size').inputValue(),String(options.SIZE.id));
  const savedProduct=page.waitForResponse(r=>r.url().endsWith('/api/product/save')&&r.request().method()==='POST');await page.locator('.bee-builder-submit').click();const r=await savedProduct;assert.equal(r.status(),200,await r.text());productId=(await r.json()).id;
  const result=await(await page.request.get(base+'/adm-prj/qtht-001/api/product/find-by-id?id='+productId)).json();
  assert.equal(result.product.brandId,options.BRAND.id);assert.equal(result.product.soleId,options.SOLE.id);assert.equal(result.product.materialId,options.MATERIAL.id);assert.equal(result.productDetails.length,1);assert.match(result.productDetails[0].sku,/^F6S-SKU/);
  assert.equal(result.product.slug,'quick-create-shoe-'+stamp);
  await page.goto(base+'/adm-prj/qtht-001/product/save?id='+productId);await page.waitForSelector('.bee-product-builder');
  await page.locator('#name').fill('Tên giày sửa lại');assert.equal(await page.locator('#slug').inputValue(),result.product.slug,'Rename must preserve an existing product URL');
  // Existing security rules: employee may edit a product, not create catalog master data.
  sql("UPDATE users SET password=(SELECT p FROM (SELECT password p FROM users WHERE email='admin@gmail.com' LIMIT 1) s),actived=b'1',activation_status=b'1' WHERE email='f6s-demo-nv-5@example.invalid';");
  const staff=await browser.newContext();const staffPage=await staff.newPage();await staffPage.goto(base+'/login');await staffPage.locator('[name=username]').fill('f6s-demo-nv-5@example.invalid');await staffPage.locator('[name=password]').fill('123123');await staffPage.locator('.btn-login-main').click();await staffPage.waitForURL('**/emp-prj/**');
  assert.equal((await staff.request.post(url,{data:{group:'SIZE',name:'123'},maxRedirects:0})).status(),403);await staff.close();
  console.log('PASS six quick-create groups, draft preservation, live selections/template, duplicate/HEX validation, real product save/SKU, anonymous and employee authorization');
 }finally{
  if(productId)await page.request.delete(base+'/adm-prj/qtht-001/api/product/delete-product?id='+productId);
  sql("UPDATE users SET password=NULL,actived=b'0',activation_status=b'0' WHERE email='f6s-demo-nv-5@example.invalid';");
  await browser.close();
 }
})().catch(e=>{console.error(e);process.exitCode=1;});
