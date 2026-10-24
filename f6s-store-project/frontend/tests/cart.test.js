import { test } from 'node:test'
import assert from 'node:assert/strict'
import { sanitizeCart, cartTotal } from '../src/lib/cart.js'
const item = {
  variantId: 1,
  productId: 1,
  quantity: 2,
  stock: 5,
  price: 100000,
  name: 'Shoe',
  color: 'Black',
  size: '40',
  image: '/images/shoe-0.svg',
}
test('restores valid cart, strips duplicates and computes total', () => {
  const items = sanitizeCart([item, item])
  assert.equal(items.length, 1)
  assert.equal(cartTotal(items), 200000)
})
test('rejects malformed, negative and oversold quantities', () => {
  for (const value of [
    null,
    {},
    [null],
    [{ ...item, quantity: -1 }],
    [{ ...item, quantity: 6 }],
    [{ ...item, price: NaN }],
    [{ ...item, variantId: '1' }],
  ])
    assert.deepEqual(sanitizeCart(value), [])
})
test('does not load unsafe image URL', () =>
  assert.equal(
    sanitizeCart([{ ...item, image: 'javascript:alert(1)' }])[0].image,
    '/images/shoe-0.svg',
  ))
