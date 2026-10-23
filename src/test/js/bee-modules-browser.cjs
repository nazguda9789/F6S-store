/* Read-only UI/filter checks on an isolated seeded database. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 const page=await browser.newPage();
 try {
  await page.goto(base+'/login');await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');
  await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');
  await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  const prefix=base+'/adm-prj/qtht-001';
  const html=await(await page.request.get(prefix+'/product/list')).text();
  for(const label of ['Quản lý giao dịch','Quản lý sản phẩm','Quản lý tài khoản','Đợt giảm giá','Phiếu giảm giá','Thư viện hình ảnh'])assert.ok(html.includes(label),'Server-rendered menu: '+label);
  for(const [route,heading] of [['user/list?role=ROLE_EMPLOYEE','Danh sách nhân viên'],['user/list?role=ROLE_USER','Danh sách khách hàng'],['voucher/list','Danh sách phiếu giảm giá'],['discount-product/list','Danh sách đợt giảm giá']]){
   const r=await page.goto(prefix+'/'+route,{waitUntil:'domcontentloaded'});assert.equal(r.status(),200);
   assert.equal(await page.locator('main h1').first().innerText(),heading);
  }
  await page.goto(prefix+'/user/list?role=ROLE_EMPLOYEE&active=true&size=1',{waitUntil:'domcontentloaded'});
  assert.equal(await page.locator('#account-active').inputValue(),'true');
  for(const href of await page.locator('main .pagination a').evaluateAll(a=>a.map(x=>x.href))){const u=new URL(href);assert.equal(u.searchParams.get('active'),'true');assert.equal(u.searchParams.get('role'),'ROLE_EMPLOYEE');}
  for(const query of ['active=false','from=2050-01-01','to=2000-01-01']){
   const r=await page.goto(prefix+'/user/list?role=ROLE_EMPLOYEE&'+query,{waitUntil:'domcontentloaded'});assert.equal(r.status(),200);
   assert.equal(await page.locator('main table tbody tr').count(),0);
   assert.equal(await page.locator('main .pagination a').count(),0);
  }
  await page.goto(prefix+'/user/create?role=ROLE_EMPLOYEE',{waitUntil:'domcontentloaded'});
  assert.equal(await page.locator('main h1').innerText(),'Thông tin nhân viên');
  assert.equal(await page.locator('select[name=role]').inputValue(),'ROLE_EMPLOYEE');
  assert.equal((await page.request.get(prefix+'/user/list?size=0')).status(),400);
  console.log('PASS server-rendered Bee module labels, contextual account forms, role/status filters, independent date bounds, pagination preservation and empty results');
  if(process.env.F6S_STAFF_EMAIL && process.env.F6S_STAFF_PASSWORD){
   const context=await browser.newContext();const staff=await context.newPage();
   try{
    await staff.goto(base+'/login');await staff.locator('[name=username]').fill(process.env.F6S_STAFF_EMAIL);await staff.locator('[name=password]').fill(process.env.F6S_STAFF_PASSWORD);
    await staff.locator('.btn-login-main').click();await staff.waitForURL('**/emp-prj/**');
    for(const route of ['invoice/list','invoice/create','product/list','customer/list']){
     const r=await staff.goto(base+'/emp-prj/qtht-001/'+route,{waitUntil:'domcontentloaded'});assert.equal(r.status(),200);
     assert.equal(await staff.locator('#collapseVendors').count(),0);assert.equal(await staff.locator('a[href*="shoe-attributes"]').count(),0);
     assert.equal(await staff.locator('[data-navigation="bee"]').count(),1);
    }
    await staff.goto(base+'/emp-prj/qtht-001/product/list',{waitUntil:'domcontentloaded'});await staff.waitForSelector('#listproduct tr');
    assert.ok((await staff.locator('#listproduct a[href*="/product/save"]').first().getAttribute('href')).startsWith('/emp-prj/'));
    assert.equal((await staff.request.get(base+'/adm-prj/qtht-001/user/list',{maxRedirects:0})).status(),403);
    console.log('PASS employee module visibility, four employee pages, role-correct product edit links and admin account route denied');
   }finally{await context.close();}
  }
  if(process.env.F6S_MUTATING_ACCOUNT_SMOKE === '1' && process.env.F6S_STAFF_EMAIL){
   // Only a synthetic employee fixture in a disposable DB; always restore status.
   await page.goto(prefix+'/user/list?role=ROLE_EMPLOYEE&search='+encodeURIComponent(process.env.F6S_STAFF_EMAIL));
   const row=page.locator('main table tbody tr').filter({hasText:process.env.F6S_STAFF_EMAIL});assert.equal(await row.count(),1);
   const edit=await row.locator('a[title="Chỉnh sửa"]').getAttribute('href');
   const editUrl=new URL(edit,page.url()).href;let original;
   try{
    await page.goto(editUrl);assert.equal(await page.locator('[name=password]').inputValue(),'','Editing account must never prefill stored password hash');
    original=await page.locator('[name=actived]').isChecked();
    await page.locator('[name=actived]').uncheck();await page.locator('button[type=submit]').click();await page.waitForURL('**/user/list?role=ROLE_EMPLOYEE');
    await page.goto(prefix+'/user/list?role=ROLE_EMPLOYEE&active=false&search='+encodeURIComponent(process.env.F6S_STAFF_EMAIL));
    assert.match(await page.locator('main table tbody').innerText(),/Đã nghỉ/);
    console.log('PASS employee status form actually persists and appears in inactive filter; password field empty');
   }finally{
    if(original !== undefined){await page.goto(editUrl);await page.locator('[name=actived]').setChecked(original);await page.locator('button[type=submit]').click();await page.waitForURL('**/user/list?role=ROLE_EMPLOYEE');}
   }
  }
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
