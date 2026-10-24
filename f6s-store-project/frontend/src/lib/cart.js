export function sanitizeCart(value) {
  if (!Array.isArray(value)) return []
  const seen = new Set()
  return value
    .filter(
      (i) =>
        i &&
        Number.isSafeInteger(i.variantId) &&
        i.variantId > 0 &&
        Number.isSafeInteger(i.productId) &&
        i.productId > 0 &&
        Number.isInteger(i.quantity) &&
        i.quantity > 0 &&
        i.quantity <= 99 &&
        Number.isInteger(i.stock) &&
        i.stock >= i.quantity &&
        Number.isFinite(i.price) &&
        i.price > 0 &&
        ['name', 'color', 'size', 'image'].every((k) => typeof i[k] === 'string') &&
        !seen.has(i.variantId) &&
        seen.add(i.variantId),
    )
    .slice(0, 50)
    .map((i) => ({
      ...i,
      image: /^(\/|https:\/\/)/.test(i.image) ? i.image : '/images/shoe-0.svg',
    }))
}
export const cartTotal = (items) => items.reduce((sum, i) => sum + i.price * i.quantity, 0)
