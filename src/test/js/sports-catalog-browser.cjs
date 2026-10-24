/* Read-only verification of a clean sports demo installation. */
const {chromium}=require('playwright'),assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL||'http://127.0.0.1:18086';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});const page=await browser.newPage();
 try{
  assert.equal((await page.goto(base+'/')).status(),200);assert.match(await page.locator('#f6s-hero-title').innerText(),/Chuyển động/);
  const categories=await page.locator('#menu-main a[href^="/category/sports-"]').evaluateAll(xs=>[...new Set(xs.map(x=>x.getAttribute('href')))]);assert.equal(categories.length,5);
  assert.doesNotMatch(await page.locator('#menu-main').innerText(),/cỏ nhân tạo|cỏ tự nhiên|Futsal|Wika/);
  const products=await(await page.request.get(base+'/api/v1/products/search-by-param?search=&size=100')).json();assert.equal(products.content.length,25);assert.ok(products.content.every(p=>p.slug.startsWith('sports-')));
  for(const route of categories){assert.equal((await page.goto(base+route)).status(),200);await page.locator('a[href^="/product/sports-"]').first().waitFor();assert.ok(await page.locator('a[href^="/product/sports-"]').count()>0);}
  for(const product of products.content){assert.equal((await page.goto(base+'/product/'+product.slug)).status(),200);assert.equal((await page.request.get(base+product.image)).status(),200);assert.match(await page.locator('body').innerText(),/minh họa|trình diễn/);}
  assert.equal((await page.goto(base+'/blogs')).status(),200);assert.ok(await page.locator('a[href*="blog/sports-cam-nang-"]').count()>=5);
  assert.equal((await page.goto(base+'/gioi-thieu')).status(),200);assert.doesNotMatch(await page.locator('main').innerText(),/Mercurial|CR7|Ballon|Ronaldo|Messi/);assert.equal(await page.locator('#chinhsach-doitra,#chinhsach-bao-hanh,#chinhsach-bao-mat').count(),3);
  await page.goto(base+'/login');await page.locator('[name=username]').fill('admin@gmail.com');await page.locator('[name=password]').fill('123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  for(const product of products.content){const data=await(await page.request.get(base+'/adm-prj/qtht-001/api/product/find-by-id?id='+product.id)).json();assert.equal(data.productDetails.length,10);assert.equal(new Set(data.productDetails.map(d=>d.size.id)).size,5);assert.equal(new Set(data.productDetails.map(d=>d.color.id)).size,2);assert.equal(new Set(data.productDetails.map(d=>d.sku)).size,10);}
  for(const role of ['ROLE_EMPLOYEE','ROLE_USER']){assert.equal((await page.goto(base+'/adm-prj/qtht-001/user/list?role='+role+'&search=f6s-demo-&size=100')).status(),200);assert.equal(await page.locator('tbody tr').filter({hasText:'@example.invalid'}).count(),5);}
  assert.equal((await page.goto(base+'/adm-prj/qtht-001/product/list')).status(),200);assert.equal(await page.locator('#active').isChecked(),true);
  console.log('PASS clean sports store: five categories, 25 new shoes, 250 real variants/SKUs, new assets and articles, five employees/customers, default active-only admin list');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
