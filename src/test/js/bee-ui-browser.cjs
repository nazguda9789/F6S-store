/* Read-only UI checks except a temporary session cart; no checkout or email sent. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
 try{
  const page=await browser.newPage({viewport:{width:1440,height:1000}});
  page.setDefaultTimeout(15000);
  await page.goto(base+'/',{waitUntil:'domcontentloaded'});
  const product=await page.locator('a[href*="product/"]').first().getAttribute('href');
  const category=await page.locator('.bee-category-chips a[href*="/category/"]').first().getAttribute('href');
  const blog=await page.locator('a[href*="blog/"]').first().getAttribute('href');
  for(const route of ['/',category,product,'/cart','/login','/register','/forgot','/verify-otp?email=demo%40example.test','/blogs',blog,'/contact','/gioi-thieu','/tra-cuu-don-hang']){
   const response=await page.goto(new URL(route,base).href,{waitUntil:'domcontentloaded'});
   assert.equal(response.status(),200,route);
   assert.equal(await page.locator('html').evaluate(e=>e.classList.contains('f6s-bee-ui')),true);
   assert.equal(await page.locator('.bee-header-search').isVisible(),true);
   await page.waitForFunction(()=>parseInt(getComputedStyle(document.documentElement).getPropertyValue('--bee-header-height'))>0);
   console.log('PASS desktop '+new URL(page.url()).pathname);
   if(route==='/')await page.screenshot({path:'/tmp/f6s-bee-home.png',fullPage:false});
   if(route==='/login')await page.screenshot({path:'/tmp/f6s-bee-login.png',fullPage:false});
  }
  await page.goto(base+'/',{waitUntil:'domcontentloaded'});
  await page.locator('#bee-search-input').fill('Wika');
  const responsePromise=page.waitForResponse(r=>r.url().includes('/api/v1/products/search-by-param')&&r.url().includes('Wika'));
  await page.locator('.bee-header-search button').click();assert.equal((await responsePromise).status(),200);
  await page.waitForSelector('#searchProductModal.show');await page.locator('#searchProductModal .btn-close').click();
  console.log('PASS header search opens real product results');
  await page.goto(base+'/login',{waitUntil:'domcontentloaded'});
  await page.locator('#bee-search-input').fill('Wika');
  await page.locator('.bee-header-search button').click();
  await page.waitForSelector('#bee-standalone-search .bee-search-result');
  await page.locator('#bee-standalone-search .btn-close').click();
  console.log('PASS search also works on authentication pages');
  const added=await page.request.post(base+'/api/v1/cart/add',{data:{id:Number(process.env.F6S_TEST_VARIANT || 239),quantity:1}});assert.equal(added.status(),200);
  const cart=await(await page.request.get(base+'/api/v1/cart/my-cart')).json();assert.ok(cart.cartDetails.length>0);
  const checkout='/checkout?detail='+cart.cartDetails[0].dataId;
  assert.equal((await page.goto(base+checkout,{waitUntil:'domcontentloaded'})).status(),200);
  assert.equal(new URL(page.url()).pathname,'/checkout');
  assert.equal(await page.locator('.place-order-btn-desktop').isVisible(),true);
  await page.screenshot({path:'/tmp/f6s-bee-checkout.png',fullPage:false});
  console.log('PASS checkout with real session cart (no order submitted)');
  await page.setViewportSize({width:390,height:844});
  for(const route of ['/',category,product,'/cart',checkout,'/login','/register']){
   await page.goto(new URL(route,base).href,{waitUntil:'domcontentloaded'});
   await page.waitForSelector('.bee-header-search');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),'Horizontal overflow '+route);
   console.log('PASS mobile '+new URL(page.url()).pathname);
  }
  await page.goto(base+'/',{waitUntil:'domcontentloaded'});
  await page.locator('.navbar-toggler').click();await page.waitForSelector('#navbarSupportedContent.show');
  assert.equal(await page.locator('#navbarSupportedContent').isVisible(),true);
  await page.screenshot({path:'/tmp/f6s-bee-mobile.png',fullPage:false});
  console.log('PASS mobile expanded navigation');
 }finally{await browser.close();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
