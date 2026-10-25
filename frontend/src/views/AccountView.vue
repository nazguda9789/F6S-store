<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useCustomer } from '../stores/customer'
import { api, money } from '../lib/api'
import { orderLabels, paymentLabels } from '../lib/order'
const customer = useCustomer(),
  email = ref(''),
  code = ref(''),
  challenge = ref(null),
  busy = ref(false),
  error = ref(''),
  notice = ref(''),
  enabled = ref(false),
  orders = ref([])
const profile = reactive({ fullName: '', phone: '', address: '' })
async function run(work) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await work()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function send() {
  await run(async () => {
    challenge.value = await api('/auth/otp', {
      method: 'POST',
      body: JSON.stringify({ email: email.value }),
    })
    notice.value = 'Mã đã gửi đến email. Kiểm tra cả mục thư rác; chờ 60 giây trước khi gửi lại.'
  })
}
async function verify() {
  await run(async () => {
    await customer.login(challenge.value.challengeId, code.value)
    code.value = ''
    await load()
  })
}
async function load() {
  Object.assign(profile, {
    fullName: customer.profile?.fullName || '',
    phone: customer.profile?.phone || '',
    address: customer.profile?.address || '',
  })
  orders.value = await api('/me/orders')
}
async function save() {
  await run(async () => {
    await customer.save(profile)
    notice.value = 'Đã lưu hồ sơ'
  })
}
onMounted(() =>
  run(async () => {
    enabled.value = (await api('/integrations')).emailOtp
    if (customer.profile) await load()
  }),
)
</script>
<template>
  <div class="shell section max-w-4xl">
    <h1 class="page-title mb-8">Tài khoản F6S</h1>
    <p v-if="error" role="alert" class="error-box mb-5">{{ error }}</p>
    <p v-if="notice" role="status" class="success-panel mb-5">{{ notice }}</p>
    <section v-if="!customer.profile" class="admin-card">
      <h2 class="text-xl font-semibold mb-4">Đăng ký / Đăng nhập bằng email OTP</h2>
      <p class="text-sm mb-5">
        Lần đầu xác minh email sẽ tạo tài khoản. Không cần mật khẩu. Mã hết hạn sau 5 phút, tối đa 5
        lần nhập.
      </p>
      <p v-if="!enabled" class="error-box">
        Đăng nhập email chưa bật. Quản trị cần cấu hình SMTP và OTP_SECRET; bạn vẫn có thể đặt hàng
        COD không cần tài khoản.
      </p>
      <form @submit.prevent="send">
        <label class="field"
          >Email đăng nhập<input
            v-model.trim="email"
            type="email"
            maxlength="254"
            required
            :disabled="busy" /></label
        ><button class="button" :disabled="busy || !enabled">
          {{ challenge ? 'Gửi lại mã' : 'Gửi mã OTP' }}
        </button>
      </form>
      <form v-if="challenge" @submit.prevent="verify" class="mt-6">
        <label class="field"
          >Mã OTP<input
            v-model.trim="code"
            inputmode="numeric"
            pattern="[0-9]{6}"
            minlength="6"
            maxlength="6"
            autocomplete="one-time-code"
            required /></label
        ><button class="button" :disabled="busy">Xác minh và đăng nhập</button>
      </form>
    </section>
    <section v-else>
      <div class="admin-card mb-6">
        <p class="font-semibold mb-5">{{ customer.profile.email }}</p>
        <form @submit.prevent="save">
          <label class="field"
            >Họ và tên<input v-model.trim="profile.fullName" required maxlength="120" /></label
          ><label class="field"
            >Số điện thoại<input
              v-model.trim="profile.phone"
              required
              pattern="0[35789][0-9]{8}"
              maxlength="10" /></label
          ><label class="field"
            >Địa chỉ nhận hàng<textarea
              v-model.trim="profile.address"
              required
              minlength="10"
              maxlength="500"
            /></label
          ><button class="button" :disabled="busy">Lưu hồ sơ</button
          ><button
            type="button"
            class="button button-outline ml-3"
            :disabled="busy"
            @click="run(() => customer.logout())"
          >
            Đăng xuất
          </button>
        </form>
      </div>
      <h2 class="text-2xl font-semibold mb-5">Đơn hàng của tôi</h2>
      <p class="text-sm text-stone-500 mb-5">
        Hiển thị tối đa 50 đơn đặt khi đã đăng nhập. Đơn khách vãng lai không tự ghép theo email.
      </p>
      <article v-for="order in orders" :key="order.code" class="admin-card mb-4">
        <h3 class="font-semibold">{{ order.code }}</h3>
        <p class="mt-2">
          {{ orderLabels[order.status] || order.status }} · {{ money(order.total) }} ·
          {{ order.paymentMethod }} /
          {{ paymentLabels[order.paymentStatus] || order.paymentStatus }}
        </p>
        <ul class="mt-3 text-sm">
          <li v-for="item in order.items" :key="item.sku">
            {{ item.productName }} · {{ item.color }} / {{ item.size }} × {{ item.quantity }}
          </li>
        </ul>
      </article>
      <p v-if="!orders.length">Chưa có đơn hàng.</p>
    </section>
    <p class="text-xs text-stone-500 mt-6">
      Phiên khách giữ trong bộ nhớ; tải lại trang sẽ cần đăng nhập lại. Không chia sẻ OTP hoặc liên
      kết thanh toán.
    </p>
  </div>
</template>
