<script setup>
import { ref, reactive } from 'vue'
import { useCart } from '../stores/cart'
import { api, money } from '../lib/api'
const cart = useCart(),
  form = reactive({ customerName: '', email: '', phone: '', address: '', note: '' }),
  busy = ref(false),
  error = ref(''),
  order = ref(null)
let requestKey = '',
  fingerprint = ''
async function submit() {
  if (busy.value || !cart.items.length) return
  busy.value = true
  error.value = ''
  const body = {
    ...form,
    items: cart.items.map((i) => ({ variantId: i.variantId, quantity: i.quantity })),
  }
  const signature = JSON.stringify(body)
  if (signature !== fingerprint) {
    requestKey = crypto.randomUUID()
    fingerprint = signature
  }
  try {
    order.value = await api('/orders', {
      method: 'POST',
      body: JSON.stringify({ ...body, requestKey }),
    })
    cart.consume(body.items)
    cart.toast('Đơn hàng đã được ghi nhận')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div class="shell section">
    <p class="eyebrow">HOME / CHECKOUT</p>
    <h1 class="page-title mt-4 mb-10">Hoàn tất hành trình.</h1>
    <section v-if="order" class="success-panel" role="status">
      <span class="text-5xl">✓</span>
      <h2 class="text-3xl font-semibold mt-5">Cảm ơn bạn đã chọn F6S.</h2>
      <p class="mt-4">
        Đơn hàng <strong>{{ order.code }}</strong> đã được ghi nhận.
      </p>
      <p class="mt-2">Tổng tiền: {{ money(order.total) }} · Thanh toán khi nhận hàng (COD).</p>
      <p class="text-sm text-stone-500 mt-3">
        Đây là xác nhận trên website; bản mới chưa gửi email xác nhận.
      </p>
      <RouterLink to="/catalog" class="button mt-8">Tiếp tục khám phá <ArrowIcon /></RouterLink>
    </section>
    <div v-else-if="!cart.items.length" class="empty-state">
      <p>Giỏ hàng của bạn đang trống.</p>
      <RouterLink to="/catalog" class="button mt-5">Chọn sản phẩm <ArrowIcon /></RouterLink>
    </div>
    <div v-else class="checkout-grid">
      <form @submit.prevent="submit" class="checkout-form">
        <h2 class="text-xl font-semibold mb-6">01 / Thông tin giao hàng</h2>
        <fieldset :disabled="busy">
          <label class="field"
            >Họ và tên<input
              v-model.trim="form.customerName"
              autocomplete="name"
              required
              maxlength="120"
              placeholder="Nguyễn Văn An"
          /></label>
          <div class="grid sm:grid-cols-2 gap-4">
            <label class="field"
              >Email<input
                v-model.trim="form.email"
                autocomplete="email"
                type="email"
                required
                maxlength="254"
                placeholder="ban@example.com" /></label
            ><label class="field"
              >Số điện thoại<input
                v-model.trim="form.phone"
                autocomplete="tel"
                type="tel"
                required
                pattern="0[35789][0-9]{8}"
                maxlength="10"
                title="Số di động Việt Nam gồm 10 chữ số"
                placeholder="09xxxxxxxx"
            /></label>
          </div>
          <label class="field"
            >Địa chỉ đầy đủ<textarea
              v-model.trim="form.address"
              autocomplete="street-address"
              required
              minlength="10"
              maxlength="500"
              rows="3"
              placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
            /></label
          ><label class="field"
            >Ghi chú (không bắt buộc)<textarea
              v-model.trim="form.note"
              maxlength="500"
              rows="2"
              placeholder="Thông tin thêm cho đơn hàng"
            />
          </label>
          <h2 class="text-xl font-semibold my-6">02 / Phương thức thanh toán</h2>
          <div class="payment-option">◉ Thanh toán khi nhận hàng (COD)</div>
          <p class="text-sm text-stone-500 mt-3">
            Bản kiến trúc mới chưa tích hợp OTP và VNPAY. Cấu hình bản cũ được giữ nguyên ở dự án
            cũ.
          </p>
        </fieldset>
        <div v-if="error" role="alert" class="error-box mt-5">
          {{ error }}
          <p class="text-sm mt-2">
            Nếu mất kết nối, bấm lại mà không đổi thông tin để dùng cùng mã yêu cầu. Nếu hết hàng,
            hãy điều chỉnh giỏ hàng.
          </p>
        </div>
        <button :disabled="busy" class="button w-full mt-7">
          {{busy?'Đang xác nhận…':'Xác nhận đặt hàng <ArrowIcon />'}}
        </button>
      </form>
      <aside class="order-summary">
        <h2 class="text-xl font-semibold mb-5">Đơn hàng của bạn</h2>
        <article v-for="item in cart.items" :key="item.variantId" class="summary-line">
          <img :src="item.image" :alt="item.name" class="w-20 h-20 bg-white rounded-lg" />
          <div class="flex-1">
            <h3 class="font-semibold text-sm">{{ item.name }}</h3>
            <p class="text-sm text-stone-500">
              {{ item.color }} / {{ item.size }} × {{ item.quantity }}
            </p>
            <p class="text-sm mt-2">{{ money(item.price * item.quantity) }}</p>
          </div>
        </article>
        <div class="flex justify-between mt-6 pt-5 border-t">
          <span>Tạm tính</span><span>{{ money(cart.total) }}</span>
        </div>
        <div class="flex justify-between my-3 text-sm">
          <span>Vận chuyển (demo)</span><span>Miễn phí</span>
        </div>
        <div class="flex justify-between pt-4 border-t text-xl font-semibold">
          <span>Tổng cộng</span><span>{{ money(cart.total) }}</span>
        </div>
        <button class="text-sm underline mt-5" :disabled="busy" @click="cart.open = true">
          Chỉnh sửa giỏ hàng
        </button>
        <p class="text-xs text-stone-500 mt-4">
          Tổng tiền chính thức được tính lại từ giá sản phẩm trên hệ thống khi đặt hàng.
        </p>
      </aside>
    </div>
  </div>
</template>
