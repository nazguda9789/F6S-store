/* Optional browser smoke test. Run on an isolated seeded demo database.
 * F6S_BASE_URL=http://127.0.0.1:18080 NODE_PATH=<playwright node_modules> node src/test/js/workspace-browser.cjs
 * Credentials may be overridden with F6S_TEST_EMAIL / F6S_TEST_PASSWORD.
 */
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const base = process.env.F6S_BASE_URL || 'http://127.0.0.1:18080';
(async () => {
    const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || '/usr/bin/chromium', args: ['--no-sandbox'] });
    try {
        const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
        page.on('pageerror', error => console.error('Browser error:', error.stack));
        await page.goto(base + '/login');
        await page.locator('[name=username]').fill(process.env.F6S_TEST_EMAIL || 'admin@gmail.com');
        await page.locator('[name=password]').fill(process.env.F6S_TEST_PASSWORD || '123123');
        await page.locator('.btn-login-main').click();
        await page.waitForURL('**/adm-prj/**');
        for (const route of ['invoice/list', 'invoice/create', 'customer/list', 'product/list', 'banner/list', 'blog/list', 'contact/list', 'cloudkey/list', 'voucher/list']) {
            const response = await page.goto(base + '/adm-prj/qtht-001/' + route, { waitUntil: 'domcontentloaded' });
            assert.equal(response.status(), 200, route);
            await page.waitForSelector('.workspace-heading');
            assert.equal(await page.locator('.workspace-brand strong').innerText(), 'F6S');
            assert.equal(await page.locator('#collapseVendors a[href$="role=ROLE_USER"]').count(), 1);
            assert.equal(await page.locator('#collapseVendors a[href$="role=ROLE_EMPLOYEE"]').count(), 1);
            assert.equal(await page.locator('#collapseSupport').count(), 0);
            assert.equal(await page.locator('#collapseHeThong a[href*="/banner/"]').count(), 0);
            assert.equal(await page.locator('[data-navigation="bee"]').count(), 1);
            assert.equal(await page.locator('.workspace-brand').count(), 1);
            assert.equal(await page.locator('#new-orders-widget').count(), 0);
            assert.equal(await page.locator('.workspace-order-bell[href$="/invoice/list"]').count(), 1);
            assert.equal(await page.locator('#collapseVoucher a').allTextContents().then(v=>v.join('|')), 'Đợt giảm giá|Phiếu giảm giá');
            if (route === 'product/list') {
                assert.deepEqual((await page.locator('main table').first().locator('thead th').allTextContents()).map(t=>t.trim()), ['STT','Tên sản phẩm','Số lượng','Danh mục','Thương hiệu','Trạng thái','Thao tác']);
            }
            if (route === 'invoice/create') {
                assert.equal(await page.locator('.pos-draft-tabs').count(), 1);
                await page.locator('[data-bs-target="#pos-product-picker"]').click();
                await page.waitForSelector('#pos-product-picker.show');
                assert.equal(await page.locator('#productSearch').isVisible(), true);
                await page.locator('#pos-product-picker .btn-close').click();
                await page.waitForSelector('#pos-product-picker', { state: 'hidden' });
                await page.locator('#fullName').fill('Khách demo F6S');
                assert.match(await page.locator('.pos-summary').innerText(), /Khách demo F6S/);
                console.log('PASS POS product picker, persisted-draft tab strip and live customer summary');
            }
            console.log('PASS desktop: ' + route);
            if (route === 'invoice/list') {
                await page.locator('#search').fill('POS test');
                const filtered = page.waitForResponse(r => r.url().includes('/api/invoice/filter?') && r.request().method() === 'POST' && r.request().postDataJSON().invoiceType === 'TAI_QUAY');
                await page.locator('#invoiceTypeFilter').selectOption('TAI_QUAY');
                const response = await filtered;
                assert.equal(response.status(), 200);
                assert.equal(response.request().postDataJSON().search, 'POS test');
                const reset = page.waitForResponse(r => r.url().includes('/api/invoice/filter?') && r.request().method() === 'POST' && r.request().postDataJSON().invoiceType === null && r.request().postDataJSON().search === '');
                await page.locator('button[onclick="resetInvoiceFilters()"]').click();
                assert.equal((await reset).status(), 200);
                console.log('PASS transaction type + search filter and reset');
            }
        }
        if (process.env.F6S_MUTATING_SMOKE === '1') {
            // Explicit opt-in: only use a disposable local demo database.
            const api = base + '/adm-prj/qtht-001/api';
            const created = await page.request.post(api + '/order-draft/create', { data: {
                fullName: 'POS smoke test', phone: '', email: '', totalAmount: 0, discount: 0, shipCost: 0
            } });
            const result = await created.json();
            assert.equal(result.success, true);
            const id = result.data.id;
            try {
                await page.goto(base + '/adm-prj/qtht-001/invoice/create', { waitUntil: 'domcontentloaded' });
                await page.locator('.pos-draft-tab').filter({ hasText: 'Hóa đơn #' + id }).click();
                await page.waitForFunction(expected => document.querySelector('#id-order').value === String(expected), id);
                assert.equal(await page.locator('#fullName').inputValue(), 'POS smoke test');
                const products = await (await page.request.get(api + '/product/product-admin?page=0&size=5&search=')).json();
                const detail = products.content.flatMap(p => p.productDetails).find(d => d.stock > 0);
                assert.ok(detail);
                const added = await (await page.request.post(api + '/order-draft/add-to-order?detailId=' + detail.id + '&orderId=' + id)).json();
                assert.equal(added.success, true);
                await page.locator('.pos-draft-tab').filter({ hasText: 'Hóa đơn #' + id }).click();
                await page.waitForSelector('#orderItemsTableBody tr');
                assert.ok(await page.locator('#orderItemsTableBody tr').count() > 0);
                console.log('PASS real draft creation -> tab load -> add variant -> cart reload');
            } finally {
                const removed = await page.request.delete(api + '/order-draft/delete-order?orderId=' + id);
                assert.equal(removed.ok(), true);
            }
        }
        await page.locator('#sidebarToggle').click();
        assert.equal(await page.locator('#layoutSidenav_nav').isVisible(), false);
        await page.locator('#sidebarToggle').click();
        assert.equal(await page.locator('#layoutSidenav_nav').isVisible(), true);
        console.log('PASS desktop menu toggle');
        await page.setViewportSize({ width: 390, height: 844 });
        assert.equal(await page.locator('#layoutSidenav_nav').isVisible(), false);
        await page.locator('#sidebarToggle').click();
        assert.equal(await page.locator('#layoutSidenav_nav').isVisible(), true);
        await page.waitForFunction(() => document.querySelector('#sidebarToggle').getAttribute('aria-expanded') === 'true');
        await page.keyboard.press('Escape');
        assert.equal(await page.locator('#layoutSidenav_nav').isVisible(), false);
        console.log('PASS mobile menu open / Escape close');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
