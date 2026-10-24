import { test, expect } from '@playwright/test'

// Run only against a disposable seeded DB: this test places real COD orders in that DB.
test('home, filters, sold-out size, cart persistence and COD checkout', async ({
  page,
}, testInfo) => {
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toContainText('chuyển động')
  await expect(page.locator('.product-card')).toHaveCount(5)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.screenshot({ path: testInfo.outputPath('home.png'), fullPage: true })
  await page.goto('/catalog')
  await expect(page.locator('.product-card')).toHaveCount(12)
  if (testInfo.project.name === 'mobile') await page.getByRole('button', { name: 'Bộ lọc' }).click()
  await page.getByRole('combobox', { name: 'Thương hiệu', exact: true }).selectOption('Nike')
  await page.getByRole('button', { name: 'Áp dụng bộ lọc' }).click()
  await expect(page.locator('.product-card')).toHaveCount(5)
  await page.locator('.product-card').first().click()
  await expect(page.getByRole('button', { name: '42', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: '40', exact: true }).click()
  await page.getByRole('button', { name: 'Thêm vào giỏ' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: 'Đóng giỏ hàng' }).click()
  await page.reload()
  await page.getByRole('button', { name: 'Giỏ hàng' }).click()
  await expect(page.locator('.cart-line')).toHaveCount(1)
  await page.getByRole('link', { name: 'Tiến hành thanh toán' }).click()
  await page.getByLabel('Họ và tên').fill('Nguyễn Test Vue')
  await page.getByLabel('Email', { exact: true }).fill('vue-test@example.com')
  await page.getByLabel('Số điện thoại').fill('0912345678')
  await page.getByLabel('Địa chỉ đầy đủ').fill('123 Nguyễn Trãi, Thanh Xuân, Hà Nội')
  const response = page.waitForResponse(
    (r) => r.url().endsWith('/api/orders') && r.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Xác nhận đặt hàng' }).click()
  expect((await response).status()).toBe(201)
  await expect(page.getByRole('heading', { name: 'Cảm ơn bạn đã chọn F6S.' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Giỏ hàng 0' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  expect(errors).toEqual([])
})
