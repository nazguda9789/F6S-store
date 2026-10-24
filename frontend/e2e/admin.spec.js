import { test, expect } from '@playwright/test'
test('matrix, QR labels, product-scoped promotion, POS and invoice print', async ({
  page,
}, testInfo) => {
  test.skip(
    !process.env.E2E_ADMIN_PASSWORD,
    'Requires explicit credentials and a disposable seeded DB',
  )
  const password = process.env.E2E_ADMIN_PASSWORD,
    user = process.env.E2E_ADMIN_USERNAME || 'admin'
  const authorization = 'Basic ' + Buffer.from(user + ':' + password).toString('base64')
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  const name = 'Matrix E2E ' + testInfo.project.name + ' ' + Date.now(),
    promo = 'E2E_' + Date.now().toString().slice(-10)
  async function nav(label) {
    if (testInfo.project.name === 'mobile')
      await page.getByRole('button', { name: 'Ẩn/hiện menu quản trị' }).click()
    await page
      .getByRole('navigation', { name: 'Quản trị' })
      .getByRole('link', { name: label })
      .click()
  }
  await page.goto('/admin/login')
  await page.getByLabel('Tài khoản', { exact: true }).fill(user)
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Tổng quan kinh doanh' })).toBeVisible()
  await nav('Quản lý sản phẩm')
  await page.getByRole('button', { name: 'Thêm sản phẩm' }).click()
  await page.getByLabel('Tên sản phẩm', { exact: true }).fill(name)
  await page
    .getByLabel('Mô tả', { exact: true })
    .fill('Giày kiểm thử ma trận, giá riêng, tồn kho và QR.')
  await page.getByLabel('Size EU (cách nhau bằng dấu phẩy)').fill('40, 41')
  await page.getByLabel('Tồn mặc định').fill('5')
  await page.getByLabel('Giá mặc định (₫)').fill('100000')
  await page.getByRole('button', { name: 'Sinh / cập nhật tổ hợp' }).click()
  await expect(page.getByLabel('Tồn biến thể', { exact: true })).toHaveCount(4)
  await page.getByLabel('Giá biến thể', { exact: true }).first().fill('120000')
  await page.getByLabel('Tồn biến thể', { exact: true }).nth(1).fill('0')
  await page.getByRole('button', { name: 'Lưu sản phẩm' }).click()
  await expect(page.getByRole('status')).toContainText('4 biến thể')
  const row = page.getByRole('row').filter({ hasText: name })
  await row.getByRole('button', { name: 'Mã QR' }).click()
  await expect(page.getByRole('dialog').locator('.qr-labels img')).toHaveCount(4)
  await page.getByRole('button', { name: 'Đóng tem QR' }).click()
  const p = (await (await page.request.get('/api/products?q=' + encodeURIComponent(name))).json())
    .content[0]
  await nav('Khuyến mãi')
  await page.getByRole('button', { name: 'Tạo khuyến mãi' }).click()
  await page.getByLabel('Mã', { exact: true }).fill(promo)
  await page.getByLabel('Tên khuyến mãi', { exact: true }).fill('Promo E2E')
  await page.getByRole('checkbox', { name: new RegExp(name) }).check()
  await page.getByRole('button', { name: 'Lưu khuyến mãi' }).click()
  await expect(page.getByRole('row').filter({ hasText: promo })).toBeVisible()
  await nav('Bán hàng tại quầy')
  await page.getByRole('button', { name: 'Tạo hóa đơn' }).click()
  await page.getByLabel('Quét QR / nhập SKU').fill(p.variants[0].qrPayload)
  await page.getByRole('button', { name: 'Thêm', exact: true }).click()
  await expect(page.getByRole('cell', { name: p.variants[0].sku, exact: false })).toBeVisible()
  await page.getByLabel('Mã giảm giá', { exact: true }).fill(promo)
  await page.getByRole('button', { name: 'Kiểm tra mã / tính lại' }).click()
  await page.getByLabel('Tiền khách đưa (₫)').fill('200000')
  const response = page.waitForResponse(
    (r) => r.url().endsWith('/checkout') && r.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Xác nhận thanh toán', exact: true }).click()
  const payment = await response
  expect(payment.status()).toBe(200)
  const invoice = await payment.json()
  expect(invoice.total).toBe(108000)
  expect(invoice.cashChange).toBe(92000)
  await expect(page.getByRole('heading', { name: 'Hóa đơn đã ghi nhận' })).toBeVisible()
  await page.evaluate(
    () =>
      (window.print = () => {
        window.f6sPrintCalled = true
      }),
  )
  await page.getByRole('button', { name: 'In hóa đơn', exact: true }).click()
  expect(await page.evaluate(() => window.f6sPrintCalled)).toBeTruthy()
  await page.screenshot({ path: testInfo.outputPath('pos.png'), fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  const latest = await (await page.request.get('/api/products/' + p.id)).json()
  expect(latest.variants[0].stock).toBe(4)
  const dashboard = await page.request.get('/api/admin/dashboard?from=2026-01-01&to=2026-12-31', {
    headers: { Authorization: authorization },
  })
  expect(dashboard.status()).toBe(200)
  expect(errors).toEqual([])
})
