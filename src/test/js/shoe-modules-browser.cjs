/* Isolated demo DB only: creates a test catalog attribute and product, deletes product afterwards.
 * NODE_PATH=<playwright node_modules> node src/test/js/shoe-modules-browser.cjs
 */
const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const base = process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async () => {
 const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
 let productId;
 const page = await browser.newPage({viewport:{width:1440,height:1000}});
 try {
  await page.goto(base+'/login');
  await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');
  await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');
  await page.locator('.btn-login-main').click(); await page.waitForURL('**/adm-prj/**');
  for (const route of ['shoe-attributes?kind=BRAND','shoe-attributes?kind=SOLE','shoe-attributes?kind=MATERIAL','image-library','product/save','user/list?role=ROLE_EMPLOYEE','user/list?role=ROLE_USER']) {
   const response = await page.goto(base+'/adm-prj/qtht-001/'+route,{waitUntil:'domcontentloaded'});
   assert.equal(response.status(),200,route);
   await page.waitForSelector('.workspace-brand');
   console.log('PASS module '+route);
  }
  await page.goto(base+'/adm-prj/qtht-001/shoe-attributes?kind=BRAND');
  const name='Test catalog '+Date.now();
  const form=page.locator('form[action$="shoe-attributes/save"]').first();
  await form.locator('[name=name]').fill(name); await form.locator('[name=description]').fill('Catalog smoke description'); await form.locator('button').click();
  await page.waitForSelector('.alert-success');
  const row=page.locator('tr').filter({has:page.locator(`input[value="${name}"]`)});
  const id=Number(await row.locator('[name=id]').inputValue());
  assert.ok(id>0);
  assert.match(await row.locator('code').innerText(),/^F6S-TH\d{6,}$/);
  assert.equal(await row.locator('[name=description]').inputValue(),'Catalog smoke description');
  const duplicate=await page.request.post(base+'/adm-prj/qtht-001/shoe-attributes/save',{form:{kind:'BRAND',name,active:'true'}});
  assert.match(await duplicate.text(),/Tên đã tồn tại/);
  const payload={name,slug:'test-catalog-'+id,image:'/image/banners/source-banner1.jpg',price:100000,oldPrice:100000,weight:0.3,active:true,brandId:id,soleId:null,materialId:null,description:'Test',shortDescription:'Test',categoryIds:[],details:[],imageLinks:['/image/banners/source-banner1.jpg']};
  const created=await page.request.post(base+'/adm-prj/qtht-001/api/product/save',{data:payload});
  assert.equal(created.status(),200); const product=await created.json(); productId=product.id;
  assert.equal(product.brandId,id);
  const fetched=await (await page.request.get(base+'/adm-prj/qtht-001/api/product/find-by-id?id='+productId)).json();
  assert.equal(fetched.product.brandId,id);
  await page.goto(base+'/adm-prj/qtht-001/product/save?id='+productId);
  assert.equal(await page.locator('#brandId').inputValue(),String(id));
  const filtered=await (await page.request.post(base+'/adm-prj/qtht-001/api/product/filter?page=0&size=5',{data:{brandId:id}})).json();
  assert.ok(filtered.content.some(p=>p.id===productId));
  assert.equal(filtered.content.find(p=>p.id===productId).brandName,name);
  await page.goto(base+'/adm-prj/qtht-001/image-library?search='+encodeURIComponent(name));
  assert.equal(await page.locator('img[alt="'+name+'"]').count(),1);
  await page.request.post(base+'/adm-prj/qtht-001/shoe-attributes/save',{form:{id,kind:'BRAND',name,active:'false'}});
  const invalid=await page.request.post(base+'/adm-prj/qtht-001/api/product/save',{data:payload});
  assert.equal(invalid.status(),400);
  await page.goto(base+'/adm-prj/qtht-001/shoe-attributes?kind=BRAND&active=false&search='+encodeURIComponent(name));
  assert.equal(await page.locator('tr').filter({has:page.locator(`input[value="${name}"]`)}).count(),1);
  assert.equal(await page.locator('tr').filter({has:page.locator(`input[value="${name}"]`)}).locator('[name=description]').inputValue(),'Catalog smoke description');
  console.log('PASS create / duplicate validation / product reference / brand filter / image search / disabled reference rejected');
  const guest=await browser.newContext();
  const denied=await guest.request.post(base+'/adm-prj/qtht-001/shoe-attributes/save',{form:{kind:'BRAND',name:'Unauthorized',active:'true'},maxRedirects:0});
  assert.ok([302,401,403].includes(denied.status()));
  await guest.close();
  console.log('PASS unauthenticated catalog write blocked');
 } finally {
  if (productId) {
   const removed=await page.request.delete(base+'/adm-prj/qtht-001/api/product/delete-product?id='+productId);
   assert.ok(removed.ok(),'Cleanup product');
  }
  await browser.close();
 }
})().catch(error=>{console.error(error.message);process.exitCode=1;});
