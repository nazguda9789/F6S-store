import { test, expect } from '@playwright/test'

test('email OTP registers a customer using local SMTP inbox', async ({ page, request }, info) => {
  test.skip(!process.env.E2E_MAILPIT_URL, 'Requires local Mailpit, OTP enabled and disposable DB')
  const email = `buyer-${info.project.name}-${Date.now()}@example.test`
  await page.goto('/account')
  await page.getByLabel('Email đăng nhập', { exact: true }).fill(email)
  const send = page.getByRole('button', { name: 'Gửi mã OTP', exact: true })
  await expect(send).toBeEnabled()
  await send.click()
  await expect(page.getByLabel('Mã OTP', { exact: true })).toBeVisible({ timeout: 15000 })
  let id
  await expect
    .poll(async () => {
      const inbox = await (
        await request.get(process.env.E2E_MAILPIT_URL + '/api/v1/messages')
      ).json()
      const message = inbox.messages?.find((m) => m.To?.some((to) => to.Address === email))
      id = message?.ID
      return !!id
    })
    .toBeTruthy()
  const mail = await (
    await request.get(process.env.E2E_MAILPIT_URL + '/api/v1/message/' + id)
  ).json()
  const code = mail.Text.match(/\b[0-9]{6}\b/)[0]
  await page.getByLabel('Mã OTP', { exact: true }).fill(code)
  await page.getByRole('button', { name: 'Xác minh và đăng nhập', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Đơn hàng của tôi', exact: true })).toBeVisible()
  await page.getByLabel('Họ và tên', { exact: true }).fill('Khách hàng demo')
  await page.getByLabel('Số điện thoại', { exact: true }).fill('0912345678')
  await page.getByLabel('Địa chỉ nhận hàng', { exact: true }).fill('123 Phố kiểm thử, Hà Nội')
  await page.getByRole('button', { name: 'Lưu hồ sơ', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('Đã lưu hồ sơ')
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
  await expect(page.getByLabel('Email đăng nhập', { exact: true })).toBeVisible()
  if (process.env.E2E_ADMIN_PASSWORD) {
    await page.goto('/admin/login')
    await page
      .getByLabel('Tài khoản', { exact: true })
      .fill(process.env.E2E_ADMIN_USERNAME || 'admin')
    await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Tổng quan kinh doanh' })).toBeVisible()
    if (info.project.name === 'mobile')
      await page.getByRole('button', { name: 'Ẩn/hiện menu quản trị' }).click()
    await page
      .getByRole('navigation', { name: 'Quản trị' })
      .getByRole('link', { name: 'Tài khoản online', exact: true })
      .click()
    await page.getByLabel('Tìm email hoặc tên', { exact: true }).fill(email)
    await page.getByRole('button', { name: 'Tìm kiếm', exact: true }).click()
    const row = page.getByRole('row').filter({ hasText: email })
    page.once('dialog', (dialog) => dialog.accept())
    await row.getByRole('button', { name: 'Ngừng hoạt động', exact: true }).click()
    await expect(row.getByRole('button', { name: 'Bật lại', exact: true })).toBeVisible()
  }
})

test('online delivery and full return use manual waybill and refund evidence', async ({
  page,
  request,
}, info) => {
  test.skip(!process.env.E2E_ADMIN_PASSWORD, 'Requires explicit admin and disposable DB')
  const authorization =
    'Basic ' +
    Buffer.from(
      (process.env.E2E_ADMIN_USERNAME || 'admin') + ':' + process.env.E2E_ADMIN_PASSWORD,
    ).toString('base64')
  const list = await (await request.get('/api/products?limit=48')).json()
  const product = list.content.find((p) => p.variants.some((v) => v.stock > 0)),
    variant = product.variants.find((v) => v.stock > 0)
  const created = await request.post('/api/orders', {
    data: {
      requestKey: crypto.randomUUID(),
      customerName: 'Giao hàng E2E',
      email: 'delivery@example.test',
      phone: '0912345678',
      address: '123 Phố kiểm thử, Hà Nội',
      note: '',
      paymentMethod: 'COD',
      items: [{ variantId: variant.id, quantity: 1 }],
    },
  })
  expect(created.ok()).toBeTruthy()
  const order = await created.json()
  await page.goto('/admin/login')
  await page
    .getByLabel('Tài khoản', { exact: true })
    .fill(process.env.E2E_ADMIN_USERNAME || 'admin')
  await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Tổng quan kinh doanh' })).toBeVisible()
  if (info.project.name === 'mobile')
    await page.getByRole('button', { name: 'Ẩn/hiện menu quản trị' }).click()
  await page
    .getByRole('navigation', { name: 'Quản trị' })
    .getByRole('link', { name: 'Danh sách hóa đơn', exact: true })
    .click()
  await page.getByLabel('Mã đơn / Khách hàng / Số điện thoại', { exact: true }).fill(order.code)
  await page.getByRole('button', { name: 'Lọc hóa đơn', exact: true }).click()
  const row = page.getByRole('row').filter({ hasText: order.code })
  page.on('dialog', (dialog) => dialog.accept())
  await row.getByRole('button', { name: 'Xác nhận đơn', exact: true }).click()
  await row.getByRole('button', { name: 'Chuẩn bị xong', exact: true }).click()
  await row.getByRole('button', { name: 'Chi tiết / In', exact: true }).click()
  await page.getByLabel('Mã vận đơn', { exact: true }).fill('DEMO-' + Date.now())
  await page.getByRole('button', { name: 'Lưu vận đơn', exact: true }).click()
  await row.getByRole('button', { name: 'Giao vận chuyển', exact: true }).click()
  await row.getByRole('button', { name: 'Xác nhận đã giao / thu tiền', exact: true }).click()
  await page.getByLabel('Lý do hoàn', { exact: true }).fill('Khách trả toàn bộ hàng kiểm thử')
  await page.getByRole('button', { name: 'Mở yêu cầu hoàn', exact: true }).click()
  await page.getByLabel('Mã chứng từ đã hoàn tiền', { exact: true }).fill('DEMO-REF-' + Date.now())
  await page.getByLabel('Đã nhận và kiểm tra toàn bộ hàng trả về kho', { exact: true }).check()
  await page.getByRole('button', { name: 'Xác nhận đã hoàn tiền', exact: true }).click()
  await expect(page.getByText('Chứng từ:', { exact: false })).toBeVisible()
  const admin = await (
    await request.get('/api/admin/orders?' + new URLSearchParams({ q: order.code }), {
      headers: { Authorization: authorization },
    })
  ).json()
  expect(admin.content[0].order.paymentStatus).toBe('REFUNDED')
})
