/* Read-only verification of the optional five-product demo dataset. */
const {chromium}=require('playwright');const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});const page=await browser.newPage();
 try{
  await page.goto(base+'/login');await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  const api=base+'/adm-prj/qtht-001/api/product';const response=await page.request.post(api+'/filter?size=10',{data:{search:'[DEMO]'}});assert.equal(response.status(),200);const products=(await response.json()).content;assert.equal(products.length,5);
  for(const p of products){
   assert.ok(p.code.startsWith('F6S-SP'));assert.ok(p.brandName);assert.ok(p.brandId&&p.soleId&&p.materialId);assert.equal(p.stock,100);
   assert.equal((await page.request.get(base+p.image)).status(),200);
   const details=await(await page.request.get(api+'/find-by-id?id='+p.id)).json();assert.equal(details.productDetails.length,5);
   assert.deepEqual(details.productDetails.map(d=>d.size.name).sort(),['38','39','40','41','42']);
   assert.equal(new Set(details.productDetails.map(d=>d.sku)).size,5);assert.ok(details.productDetails.every(d=>d.color.id===d.colorId&&d.size.id===d.sizeId));
  }
  const storefront=await(await page.request.get(base+'/api/v1/products/search-by-param?search='+encodeURIComponent('[DEMO]'))).json();assert.equal(storefront.content.length,5);
  for(const route of ['size/list','color/list','category/list','category/save']){const r=await page.goto(base+'/adm-prj/qtht-001/'+route,{waitUntil:'domcontentloaded'});assert.equal(r.status(),200);}
  console.log('PASS 5 demo shoes, 25 EU variants, real attribute references, consistent color/size IDs, unique SKUs, 5 SVG assets, storefront search and lookup pages');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
