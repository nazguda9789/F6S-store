/* Isolated DB only: commits cash sales. Never run against a real store. */
const {chromium}=require('playwright');const assert=require('node:assert/strict');const {execFileSync}=require('node:child_process');
const base=process.env.F6S_BASE_URL || 'http://127.0.0.1:18085';
const db=process.env.F6S_DEMO_DATABASE || 'f6s_pos_full_20261024_demo';assert.match(db,/^[a-zA-Z0-9_]+_demo$/);
const sql=s=>execFileSync('docker',['exec','-i','f6s-mysql-review','mysql','-uroot','-N',db],{input:s,encoding:'utf8'}).trim();
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});const page=await browser.newPage();
 try{
  await page.goto(base+'/login');await page.locator('[name=username]').fill('admin@gmail.com');await page.locator('[name=password]').fill('123123');await page.locator('.btn-login-main').click();await page.waitForURL('**/adm-prj/**');
  const api=base+'/adm-prj/qtht-001/api/order-draft';const post=async(path,data)=>{const r=await page.request.post(api+path,data?{data}:{});assert.equal(r.status(),200);return r.json();};
  const stamp=Date.now(),code='TEST-POS-'+stamp;
  sql(`INSERT INTO voucher(code,name,discount,min_amount,block,created_date) VALUES('${code}','[TEST] POS',25000,100000,b'0',NOW());`);
  const [color,size]=sql('SELECT color,size FROM product_detail WHERE active=1 LIMIT 1;').split('\t').map(Number);
  const productResponse=await page.request.post(base+'/adm-prj/qtht-001/api/product/save',{data:{name:'[TEST POS] '+stamp,slug:'test-pos-'+stamp,price:200000,oldPrice:220000,weight:0.4,active:true,image:'/image/f6s-placeholder.svg',categoryIds:[],imageLinks:[],details:[{colorId:color,sizeId:size,stock:10,price:200000,active:true}]}});
  assert.equal(productResponse.status(),200,await productResponse.text());const product=(await productResponse.json()).id;
  const variant=Number(sql('SELECT id FROM product_detail WHERE product_id='+product+' LIMIT 1;'));
  const id=(await post('/new')).data.id;assert.equal((await post('/add-to-order?orderId='+id+'&detailId='+variant)).success,true);
  // The client deliberately supplies the wrong discount and subtotal.
  assert.equal((await post('/create',{id,fullName:'Khách lẻ',totalAmount:1,discount:1,shipCost:0,voucherCode:code})).success,true);
  const quote=await(await page.request.get(api+'/quote?orderId='+id)).json();assert.equal(quote.success,true);assert.equal(quote.data.price.merchandise,200000);assert.equal(quote.data.price.discount,25000);assert.equal(quote.data.price.payable,175000);
  assert.equal(quote.data.vouchers.some(v=>v.code===code),true);
  const checkout='/save-order-to-invoice?orderId='+id+'&invoiceType=TAI_QUAY&invoiceStatus=HOAN_THANH';
  for(const suffix of ['', '&cashReceived=1','&cashReceived=175000.5'])assert.equal((await post(checkout+suffix)).success,false);
  assert.equal(Number(sql('SELECT stock FROM product_detail WHERE id='+variant)),10);
  sql(`UPDATE voucher SET block=b'1' WHERE code='${code}'`);assert.equal((await post(checkout+'&cashReceived=200000')).success,false);sql(`UPDATE voucher SET block=b'0' WHERE code='${code}'`);
  const results=await Promise.all([post(checkout+'&cashReceived=200000'),post(checkout+'&cashReceived=200000')]);assert.equal(results.filter(r=>r.success).length,1);
  const invoice=results.find(r=>r.success).data.invoiceId;assert.ok(invoice);assert.equal((await post(checkout+'&cashReceived=200000')).success,false);
  assert.equal(sql('SELECT cash_received,cash_change,discount FROM invoice WHERE id='+invoice),'200000\t25000\t25000');
  assert.equal(Number(sql('SELECT stock FROM product_detail WHERE id='+variant)),9);assert.equal(Number(sql('SELECT sold FROM product WHERE id='+product)),1);
  const detail=await page.goto(base+'/adm-prj/qtht-001/invoice/detail?id='+invoice);assert.equal(detail.status(),200);assert.equal(await page.locator('#receipt-cash-change').innerText(),'25.000 đ');assert.equal(await page.locator('.invoice-timeline li').count(),1);
  await page.emulateMedia({media:'print'});assert.equal(await page.locator('#invoice-receipt .no-print').first().isVisible(),false);await page.pdf({path:'/tmp/f6s-cash-receipt.pdf',format:'A4'});await page.emulateMedia({media:'screen'});
  // Returning to complete after an exchange request must not count another sale.
  const status=async s=>(await page.request.post(base+'/adm-prj/qtht-001/api/invoice/change-status?id='+invoice+'&invoiceStatus='+s)).json();
  assert.equal((await status('YEU_CAU_DOI_HANG')).success,true);assert.equal((await status('HOAN_THANH')).success,true);assert.equal(Number(sql('SELECT sold FROM product WHERE id='+product)),1);
  // Actual modal flow with server quote and cash tender.
  const second=(await post('/new')).data.id;await post('/add-to-order?orderId='+second+'&detailId='+variant);
  await page.goto(base+'/adm-prj/qtht-001/invoice/create');await page.locator('#select-invoice').selectOption(String(second));await page.waitForFunction(()=>document.getElementById('totalAmount').value==='200000');
  await page.locator('[data-bs-target="#saveModal"]').click();await page.locator('#pos-server-total').filter({hasText:'Phải thu'}).waitFor();
  assert.equal(await page.locator('#pos-voucher-suggestions button').count()>0,true);
  await page.locator('#pos-cash-received').fill('250000');await page.locator('#pos-cash-change').filter({hasText:'50.000'}).waitFor();
  page.on('dialog',d=>d.accept());const saved=page.waitForResponse(r=>r.url().includes('/save-order-to-invoice?'));await page.locator('#saveModal .modal-footer button').click();const savedResult=await(await saved).json();assert.equal(savedResult.success,true);
  assert.equal(Number(sql('SELECT stock FROM product_detail WHERE id='+variant)),8);
  const sku=sql('SELECT sku FROM product_detail WHERE id='+variant);
  const third=(await post('/new')).data.id;
  await page.goto(base+'/adm-prj/qtht-001/invoice/create');await page.locator('#select-invoice').selectOption(String(third));
  await page.waitForFunction(id=>document.getElementById('id-order').value===String(id),third);await page.locator('#fullName').fill('Khách quét SKU');
  for(let n=0;n<2;n++){
   await page.locator('#pos-sku-form input').fill(sku);const scanned=page.waitForResponse(r=>r.url().includes('/add-by-sku?'));await page.locator('#pos-sku-form button').click();assert.equal((await(await scanned).json()).success,true);await page.waitForFunction(()=>document.querySelector('#pos-sku-form input').value==='');
  }
  const lines=await post('/find-detail-by-order?orderId='+third);assert.equal(lines.length,1);assert.equal(lines[0].quantity,2);
  assert.equal((await post('/find-detail-order?orderId='+third)).data.fullName,'Khách quét SKU');
  assert.equal((await post('/add-by-sku?orderId='+third+'&sku=NOT-A-SKU')).success,false);assert.equal(Number(sql('SELECT stock FROM product_detail WHERE id='+variant)),8);
  await page.request.delete(api+'/delete-order?orderId='+third);
  sql("UPDATE users SET password=(SELECT p FROM (SELECT password p FROM users WHERE email='admin@gmail.com' LIMIT 1) s),actived=b'1',activation_status=b'1' WHERE email='f6s-demo-nv-5@example.invalid';");
  const staff=await browser.newContext();const worker=await staff.newPage();await worker.goto(base+'/login');await worker.locator('[name=username]').fill('f6s-demo-nv-5@example.invalid');await worker.locator('[name=password]').fill('123123');await worker.locator('.btn-login-main').click();await worker.waitForURL('**/emp-prj/**');
  assert.equal((await worker.goto(base+'/emp-prj/qtht-001/invoice/create')).status(),200);assert.equal(await worker.locator('#pos-cash-received').count(),1);assert.equal(await worker.locator('#pos-sku-form').count(),1);
  assert.equal((await worker.goto(base+'/emp-prj/qtht-001/invoice/detail?id='+invoice)).status(),200);assert.equal(await worker.locator('#receipt-cash-change').count(),1);assert.equal(await worker.locator('a[href="create"]').count()>0,true);
  assert.equal((await staff.request.get(base+'/adm-prj/qtht-001/invoice/create')).status(),403);await staff.close();
  console.log('PASS server voucher quote/tamper rejection; blocked voucher; missing/insufficient/fractional cash; concurrent exactly-once checkout; cash receipt/PDF/timeline; no repeated sold count; real POS modal checkout');
 }finally{sql("UPDATE users SET password=NULL,actived=b'0',activation_status=b'0' WHERE email='f6s-demo-nv-5@example.invalid';");await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
