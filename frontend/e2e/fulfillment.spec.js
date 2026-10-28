import { test, expect } from '@playwright/test'
const adminHeader = () => ({
  Authorization:
    'Basic ' +
    Buffer.from(
      (process.env.E2E_ADMIN_USERNAME || 'admin') + ':' + process.env.E2E_ADMIN_PASSWORD,
    ).toString('base64'),
})
async function place(request, email, headers = {}) {
  const products = await (await request.get('/api/products?limit=48')).json(),
    product = products.content.find((p) => p.variants.some((v) => v.stock > 1)),
    variant = product.variants.find((v) => v.stock > 1)
  const response = await request.post('/api/orders', {
    headers,
    data: {
      requestKey: crypto.randomUUID(),
      customerName: 'Khách kiểm thử giao hàng',
      email,
      phone: '0912345678',
      address: '123 Phố kiểm thử, Hà Nội',
      items: [{ variantId: variant.id, quantity: 1 }],
      paymentMethod: 'COD',
    },
  })
  expect(response.ok()).toBeTruthy()
  const order = await response.json()
  const found = await (
    await request.get('/api/admin/orders?' + new URLSearchParams({ q: order.code }), {
      headers: adminHeader(),
    })
  ).json()
  return {
    order,
    id: found.content[0].id,
    productId: product.id,
    variantId: variant.id,
    stock: variant.stock,
  }
}
async function loginCustomer(page, request, email) {
  await page.goto('/account')
  await page.getByLabel('Email đăng nhập', { exact: true }).fill(email)
  await expect(page.getByRole('button', { name: 'Gửi mã OTP', exact: true })).toBeEnabled()
  await page.getByRole('button', { name: 'Gửi mã OTP', exact: true }).click()
  await expect(page.getByLabel('Mã OTP', { exact: true })).toBeVisible({ timeout: 15000 })
  let messageId
  await expect
    .poll(async () => {
      const inbox = await (
        await request.get(process.env.E2E_MAILPIT_URL + '/api/v1/messages')
      ).json()
      messageId = inbox.messages?.find((m) => m.To?.some((to) => to.Address === email))?.ID
      return !!messageId
    })
    .toBeTruthy()
  const mail = await (
    await request.get(process.env.E2E_MAILPIT_URL + '/api/v1/message/' + messageId)
  ).json()
  await page.getByLabel('Mã OTP', { exact: true }).fill(mail.Text.match(/\b[0-9]{6}\b/)[0])
  const pending = page.waitForResponse(
    (r) => r.url().endsWith('/api/auth/verify') && r.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Xác minh và đăng nhập', exact: true }).click()
  const login = await (await pending).json()
  await expect(page.getByRole('heading', { name: 'Đơn hàng của tôi', exact: true })).toBeVisible()
  return login.token
}
test('customer asks to cancel, stock waits for administrator approval', async ({
  page,
  request,
}, info) => {
  test.skip(
    !process.env.E2E_MAILPIT_URL || !process.env.E2E_ADMIN_PASSWORD,
    'Requires local mail and disposable DB',
  )
  const email = 'cancel-' + info.project.name + '-' + Date.now() + '@example.test',
    token = await loginCustomer(page, request, email),
    sale = await place(request, email, { Authorization: 'Bearer ' + token })
  await page.getByRole('button', { name: 'Tải lại đơn hàng', exact: true }).click()
  const card = page.getByRole('article').filter({ hasText: sale.order.code })
  await expect(card).toContainText('Giao hàng:')
  await expect(card).toContainText('Thanh toán:')
  await card.getByLabel('Lý do yêu cầu hủy', { exact: true }).fill('Khách muốn đổi mẫu giày')
  await card.getByRole('button', { name: 'Gửi yêu cầu hủy đơn', exact: true }).click()
  await expect(card).toContainText('Chờ quản trị duyệt')
  const before = await (await request.get('/api/products/' + sale.productId)).json()
  expect(before.variants.find((v) => v.id === sale.variantId).stock).toBe(sale.stock - 1)
  const denied = await request.post('/api/me/orders/' + sale.order.code + '/cancellation', {
    data: { reason: 'Không được truy cập' },
  })
  expect(denied.status()).toBe(401)
  const decision = await request.put('/api/admin/orders/' + sale.id + '/cancellation', {
    headers: adminHeader(),
    data: { approve: true, reason: 'Duyệt theo yêu cầu khách', waybillCancelled: false },
  })
  expect(decision.ok()).toBeTruthy()
  await page.getByRole('button', { name: 'Tải lại đơn hàng', exact: true }).click()
  await expect(card).toContainText('Đã duyệt hủy')
  await expect(card).toContainText('Chưa thanh toán')
  const after = await (await request.get('/api/products/' + sale.productId)).json()
  expect(after.variants.find((v) => v.id === sale.variantId).stock).toBe(sale.stock)
})
test('failed delivery returns stock only after physical return confirmation', async ({
  page,
  request,
}, info) => {
  test.skip(!process.env.E2E_ADMIN_PASSWORD, 'Requires disposable DB and explicit admin')
  const sale = await place(request, 'return-delivery@example.test'),
    base = '/api/admin/orders/' + sale.id
  for (const status of ['CONFIRMED', 'PACKING'])
    expect(
      (await request.put(base + '/status', { headers: adminHeader(), data: { status } })).ok(),
    ).toBeTruthy()
  expect(
    (
      await request.put(base + '/shipment', {
        headers: adminHeader(),
        data: { carrier: 'Giao demo', trackingCode: 'DEMO-' + Date.now() },
      })
    ).ok(),
  ).toBeTruthy()
  expect(
    (
      await request.put(base + '/status', { headers: adminHeader(), data: { status: 'SHIPPING' } })
    ).ok(),
  ).toBeTruthy()
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
  await page
    .getByLabel('Mã đơn / Khách hàng / Số điện thoại', { exact: true })
    .fill(sale.order.code)
  await page.getByRole('button', { name: 'Lọc hóa đơn', exact: true }).click()
  await page
    .getByRole('row')
    .filter({ hasText: sale.order.code })
    .getByRole('button', { name: 'Chi tiết / In', exact: true })
    .click()
  await page.getByLabel('Lý do xử lý giao hàng', { exact: true }).fill('Khách từ chối nhận hàng')
  await page.getByRole('button', { name: 'Ghi nhận giao thất bại', exact: true }).click()
  await page.getByRole('button', { name: 'Chuyển sang đang hoàn về', exact: true }).click()
  const before = await (await request.get('/api/products/' + sale.productId)).json()
  expect(before.variants.find((v) => v.id === sale.variantId).stock).toBe(sale.stock - 1)
  await page
    .getByLabel('Đã nhận toàn bộ hàng về kho, kiểm tra đủ và có thể bán lại', { exact: true })
    .check()
  await page.getByRole('button', { name: 'Xác nhận đã nhận hàng hoàn', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: sale.order.code })).toContainText(
    'Đã nhận hàng hoàn',
  )
  const after = await (await request.get('/api/products/' + sale.productId)).json()
  expect(after.variants.find((v) => v.id === sale.variantId).stock).toBe(sale.stock)
})
