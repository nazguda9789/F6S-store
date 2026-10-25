import { test, expect } from '@playwright/test'

test('one entry switches customer and staff login without granting roles', async ({ page }) => {
  await page.goto('/login')
  await expect(page).toHaveURL(/\/account$/)
  await expect(page.getByLabel('Email đăng nhập', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Nhân viên / Quản trị', exact: true }).click()
  await expect(page.getByLabel('Tài khoản', { exact: true })).toHaveValue('')
  const password = page.getByLabel('Mật khẩu', { exact: true })
  await expect(password).toHaveAttribute('type', 'password')
  await page.getByRole('button', { name: 'Hiện mật khẩu', exact: true }).click()
  await expect(password).toHaveAttribute('type', 'text')
  await page.getByRole('button', { name: 'Khách hàng', exact: true }).click()
  await expect(page.getByLabel('Email đăng nhập', { exact: true })).toBeVisible()
  await page.goto('/admin/login')
  await expect(page).toHaveURL(/\/account\?mode=staff$/)
  await expect(
    page.getByRole('heading', { name: 'Đăng nhập nhân viên / quản trị', exact: true }),
  ).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
})

test('employee logs in through shared page and lands in POS', async ({ page, request }, info) => {
  test.skip(
    !process.env.E2E_ADMIN_PASSWORD,
    'Requires disposable DB and explicit admin credentials',
  )
  const username = 'staff' + Date.now() + info.project.name,
    password = 'staff-test-password-2026',
    authorization =
      'Basic ' +
      Buffer.from(
        (process.env.E2E_ADMIN_USERNAME || 'admin') + ':' + process.env.E2E_ADMIN_PASSWORD,
      ).toString('base64')
  const created = await request.post('/api/admin/staff', {
    headers: { Authorization: authorization },
    data: {
      username,
      password,
      profile: {
        fullName: 'Nhân viên thử',
        email: username + '@example.test',
        phone: '09' + String(Date.now()).slice(-8),
        gender: 'OTHER',
        birthDate: null,
        active: true,
      },
      address: '',
    },
  })
  expect(created.ok()).toBeTruthy()
  await page.goto('/account')
  await page.getByRole('button', { name: 'Nhân viên / Quản trị', exact: true }).click()
  await page.getByLabel('Tài khoản', { exact: true }).fill(username)
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page).toHaveURL(/\/admin\/pos$/)
  if (info.project.name === 'mobile')
    await page.getByRole('button', { name: 'Ẩn/hiện menu quản trị' }).click()
  await expect(
    page
      .getByRole('navigation', { name: 'Quản trị' })
      .getByRole('link', { name: 'Quản lý sản phẩm' }),
  ).toHaveCount(0)
  const denied = await request.get('/api/admin/staff', {
    headers: {
      Authorization: 'Basic ' + Buffer.from(username + ':' + password).toString('base64'),
    },
  })
  expect(denied.status()).toBe(403)
})
