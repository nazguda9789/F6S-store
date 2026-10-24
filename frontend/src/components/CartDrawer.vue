<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useCart } from '../stores/cart'
import { money } from '../lib/api'
const cart = useCart(),
  dialog = ref(null)
watch(
  () => cart.open,
  (value) => {
    if (value && !dialog.value.open) dialog.value.showModal()
    else if (!value && dialog.value.open) dialog.value.close()
    document.body.style.overflow = value ? 'hidden' : ''
  },
)
onBeforeUnmount(() => (document.body.style.overflow = ''))
</script>
<template>
  <dialog
    ref="dialog"
    class="cart-dialog"
    aria-labelledby="cart-title"
    @cancel="cart.open = false"
    @close="cart.open = false"
    @click="
      (e) => {
        if (e.target === dialog) cart.open = false
      }
    "
  >
    <section class="cart-panel">
      <div class="flex justify-between items-center border-b pb-5">
        <h2 id="cart-title" class="text-2xl font-semibold">
          Giỏ hàng <span class="text-stone-400">({{ cart.count }})</span>
        </h2>
        <button class="icon-button" aria-label="Đóng giỏ hàng" @click="cart.open = false">✕</button>
      </div>
      <div v-if="!cart.items.length" class="py-20 text-center">
        <p class="text-5xl mb-5"><ArrowIcon /></p>
        <h3 class="text-xl">Bắt đầu hành trình của bạn</h3>
        <p class="text-stone-500 mt-2">Chọn đôi giày đầu tiên cho giỏ hàng.</p>
        <RouterLink to="/catalog" class="button mt-6" @click="cart.open = false"
          >Khám phá sản phẩm</RouterLink
        >
      </div>
      <div v-else class="cart-lines">
        <article v-for="item in cart.items" :key="item.variantId" class="cart-line">
          <img
            :src="item.image"
            :alt="item.name"
            class="w-24 h-24 object-contain bg-stone-100 rounded-lg"
          />
          <div class="flex-1">
            <RouterLink
              :to="`/products/${item.productId}`"
              class="font-semibold"
              @click="cart.open = false"
              >{{ item.name }}</RouterLink
            >
            <p class="text-sm text-stone-500">{{ item.color }} / EU {{ item.size }}</p>
            <p class="mt-2">{{ money(item.price) }}</p>
            <div class="flex items-center justify-between mt-2">
              <div class="stepper">
                <button
                  :disabled="item.quantity <= 1"
                  aria-label="Giảm số lượng"
                  @click="cart.quantity(item.variantId, item.quantity - 1)"
                >
                  −</button
                ><span>{{ item.quantity }}</span
                ><button
                  :disabled="item.quantity >= Math.min(99, item.stock)"
                  aria-label="Tăng số lượng"
                  @click="cart.quantity(item.variantId, item.quantity + 1)"
                >
                  +
                </button>
              </div>
              <button class="text-sm underline" @click="cart.remove(item.variantId)">Xóa</button>
            </div>
          </div>
        </article>
      </div>
      <div v-if="cart.items.length" class="mt-auto border-t pt-5">
        <div class="flex justify-between text-xl font-semibold">
          <span>Tạm tính</span><span>{{ money(cart.total) }}</span>
        </div>
        <p class="text-sm text-stone-500 my-3">Giá và tồn kho được xác nhận lại khi đặt hàng.</p>
        <RouterLink to="/checkout" class="button w-full" @click="cart.open = false"
          >Tiến hành thanh toán <ArrowIcon
        /></RouterLink>
      </div>
    </section>
  </dialog>
</template>
