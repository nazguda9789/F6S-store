import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { sanitizeCart, cartTotal } from '../lib/cart'
export const useCart = defineStore('cart', () => {
  let saved = []
  try {
    saved = sanitizeCart(JSON.parse(localStorage.getItem('f6s-vue-cart') || '[]'))
  } catch {}
  const items = ref(saved),
    open = ref(false),
    notice = ref('')
  let timer
  function toast(message) {
    notice.value = message
    clearTimeout(timer)
    timer = setTimeout(() => (notice.value = ''), 4000)
  }
  watch(
    items,
    (value) => {
      try {
        localStorage.setItem('f6s-vue-cart', JSON.stringify(value))
      } catch {
        toast('Không thể lưu giỏ hàng trên thiết bị này.')
      }
    },
    { deep: true },
  )
  const total = computed(() => cartTotal(items.value)),
    count = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  function add(product, variant, quantity = 1) {
    const existing = items.value.find((i) => i.variantId === variant.id)
    if (
      !Number.isInteger(quantity) ||
      quantity < 1 ||
      (existing?.quantity || 0) + quantity > Math.min(99, variant.stock)
    ) {
      toast('Số lượng vượt quá tồn kho.')
      return
    }
    if (!existing && items.value.length >= 50) {
      toast('Giỏ hàng tối đa 50 biến thể.')
      return
    }
    if (existing) {
      existing.quantity += quantity
      existing.stock = variant.stock
      existing.price = product.price
    } else
      items.value.push({
        variantId: variant.id,
        productId: product.id,
        name: product.name,
        color: variant.color,
        size: variant.size,
        price: product.price,
        image: product.imageUrl,
        quantity,
        stock: variant.stock,
      })
    toast('Đã thêm sản phẩm vào giỏ hàng')
    open.value = true
  }
  function quantity(id, value) {
    const item = items.value.find((i) => i.variantId === id)
    if (item && Number.isInteger(value) && value >= 1 && value <= Math.min(99, item.stock))
      item.quantity = value
  }
  function remove(id) {
    items.value = items.value.filter((i) => i.variantId !== id)
  }
  function clear() {
    items.value = []
  }
  function consume(lines) {
    for (const line of lines) {
      const item = items.value.find((i) => i.variantId === line.variantId)
      if (!item) continue
      if (item.quantity <= line.quantity) remove(item.variantId)
      else item.quantity -= line.quantity
    }
  }
  return { items, open, notice, total, count, add, quantity, remove, clear, consume, toast }
})
