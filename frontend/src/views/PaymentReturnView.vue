<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../lib/api'
import { orderLabels } from '../lib/order'
const result = ref(null),
  error = ref(''),
  busy = ref(false)
async function check() {
  busy.value = true
  error.value = ''
  try {
    result.value = await api('/payments/vnpay/return' + window.location.search)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
onMounted(check)
</script>
<template>
  <div class="shell section max-w-3xl">
    <h1 class="page-title mb-8">Kết quả thanh toán</h1>
    <p v-if="error" class="error-box" role="alert">{{ error }}</p>
    <section v-if="result" class="success-panel">
      <h2 class="text-xl font-semibold">{{ result.code }}</h2>
      <p class="mt-4">
        {{
          result.paymentStatus === 'PAID'
            ? 'Máy chủ đã xác nhận thu tiền.'
            : result.paymentStatus === 'FAILED'
              ? 'Giao dịch không thành công.'
              : 'Đang chờ IPN xác nhận từ VNPAY; chưa thể coi là đã thanh toán.'
        }}
      </p>
      <p class="mt-3">Trạng thái đơn: {{ orderLabels[result.status] || result.status }}</p>
      <p v-if="result.status === 'CANCELLED' && result.paymentStatus === 'PAID'" class="mt-3">
        Đơn đã hủy nhưng tiền về sau; liên hệ cửa hàng để đối soát và hoàn tiền.
      </p>
    </section>
    <button class="button mt-6" :disabled="busy" @click="check">Kiểm tra lại</button
    ><RouterLink class="button button-outline ml-3" to="/catalog">Về cửa hàng</RouterLink>
  </div>
</template>
