<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { usePos } from '../../stores/pos'
import { api, money } from '../../lib/api'
import QrScanner from '../../components/QrScanner.vue'
import InvoiceReceipt from '../../components/InvoiceReceipt.vue'
const customerQuery = ref(''),
  customerOptions = ref([]),
  customerDialog = ref(null),
  customerError = ref('')
async function searchCustomers() {
  customerError.value = ''
  try {
    customerOptions.value = (
      await api('/admin/customers?active=true&q=' + encodeURIComponent(customerQuery.value))
    ).content
  } catch (e) {
    customerError.value = e.message
  }
}
function chooseCustomer() {
  customerDialog.value.showModal()
  searchCustomers()
}
async function setCustomer(id) {
  try {
    await pos.customer(id)
    customerDialog.value.close()
  } catch {}
}
const pos = usePos(),
  code = ref(''),
  q = ref(''),
  products = ref([]),
  promotionCode = ref(''),
  cash = ref(0),
  pickerError = ref(''),
  searching = ref(false)
const total = computed(() => pos.quote?.total ?? pos.active?.subtotal ?? 0),
  change = computed(() => Math.max(0, Number(cash.value) - total.value))
const canPay = computed(
  () =>
    pos.active?.lines.length &&
    !pos.busy &&
    pos.active.lines.every((l) => l.quantity <= l.stock) &&
    Number.isInteger(cash.value) &&
    cash.value >= total.value &&
    (!promotionCode.value.trim() ||
      pos.quote?.promotionCode === promotionCode.value.trim().toUpperCase()),
)
async function safely(action) {
  try {
    await action()
  } catch {}
}
async function search() {
  searching.value = true
  pickerError.value = ''
  try {
    products.value = (await api('/products?limit=48&q=' + encodeURIComponent(q.value))).content
  } catch (e) {
    pickerError.value = e.message
  } finally {
    searching.value = false
  }
}
async function scan(value = code.value) {
  if (!value.trim()) return
  try {
    await pos.scan(value)
    code.value = ''
  } catch {}
}
function quantity(id, event) {
  const value = Number(event.target.value)
  if (Number.isInteger(value) && value > 0) safely(() => pos.quantity(id, value))
}
function closeDraft() {
  if (confirm('Hủy hóa đơn tạm này?')) safely(() => pos.cancel())
}
watch(
  () => pos.activeId,
  () => {
    promotionCode.value = ''
    cash.value = 0
  },
)
watch(promotionCode, () => (pos.quote = null))
onMounted(() => {
  safely(() => pos.load())
  search()
})
</script>
<template>
  <div>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">SALES WORKSPACE</p>
        <h1>Bán hàng tại quầy</h1>
      </div>
      <button
        class="button"
        :disabled="pos.busy || pos.drafts.length >= 5"
        @click="safely(() => pos.create())"
      >
        + Tạo hóa đơn ({{ pos.drafts.length }}/5)
      </button>
    </div>
    <p v-if="pos.error" class="error-box mb-5" role="alert">{{ pos.error }}</p>
    <div class="pos-tabs" role="group" aria-label="Hóa đơn tạm">
      <button
        v-for="d in pos.drafts"
        :key="d.id"
        :class="{ selected: d.id === pos.activeId }"
        :disabled="pos.busy"
        @click="pos.select(d.id)"
      >
        Hóa đơn #{{ d.id }} <small>{{ d.lines.length }} dòng hàng</small>
      </button>
    </div>
    <div class="pos-layout">
      <section>
        <div class="admin-card">
          <div class="flex justify-between gap-4 items-center mb-5">
            <h2 class="font-semibold">
              Giỏ bán hàng <span class="text-stone-400">#{{ pos.activeId || '—' }}</span>
            </h2>
            <button
              class="text-sm underline"
              :disabled="pos.busy || !pos.active"
              @click="closeDraft"
            >
              Hủy hóa đơn
            </button>
          </div>
          <form class="scan-form" @submit.prevent="scan()">
            <label class="field flex-1 mb-0"
              >Quét QR / nhập SKU<input
                v-model.trim="code"
                placeholder="SKU-… hoặc F6S:VARIANT:SKU-…"
                maxlength="120"
                :disabled="pos.busy || !pos.active" /></label
            ><button class="button" :disabled="pos.busy || !pos.active || !code">Thêm</button>
          </form>
          <QrScanner v-if="pos.active && !pos.busy" @decoded="scan" />
          <div v-if="!pos.active?.lines.length" class="empty-state mt-5">
            {{
              pos.active
                ? 'Quét QR hoặc chọn sản phẩm bên dưới.'
                : 'Tạo hóa đơn để bắt đầu bán hàng.'
            }}
          </div>
          <div v-else class="table-scroll">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Tồn</th>
                  <th>Số lượng</th>
                  <th>Đơn giá</th>
                  <th>Thành tiền</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="line in pos.active.lines" :key="line.variantId">
                  <td>
                    <strong>{{ line.name }}</strong
                    ><small>{{ line.color }} / EU {{ line.size }}<br />{{ line.sku }}</small>
                  </td>
                  <td :class="{ 'text-red-700': line.stock < line.quantity }">{{ line.stock }}</td>
                  <td>
                    <input
                      aria-label="Số lượng bán"
                      class="quantity-input"
                      type="number"
                      min="1"
                      :max="Math.min(99, line.stock)"
                      :value="line.quantity"
                      :disabled="pos.busy"
                      @change="quantity(line.variantId, $event)"
                    />
                  </td>
                  <td>{{ money(line.unitPrice) }}</td>
                  <td>{{ money(line.subtotal) }}</td>
                  <td>
                    <button
                      class="text-red-700"
                      :disabled="pos.busy"
                      @click="safely(() => pos.remove(line.variantId))"
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="admin-card mt-5">
          <form class="flex gap-3 items-end mb-4" @submit.prevent="search">
            <label class="field flex-1 mb-0"
              >Tìm sản phẩm<input v-model.trim="q" placeholder="Tên giày…" maxlength="180" /></label
            ><button class="button button-outline" :disabled="searching">Tìm</button>
          </form>
          <p v-if="pickerError" role="alert" class="error-box">{{ pickerError }}</p>
          <div class="pos-picker">
            <article v-for="p in products" :key="p.id">
              <img :src="p.imageUrl" :alt="p.name" />
              <h3>{{ p.name }}</h3>
              <div class="flex flex-wrap gap-2 mt-3">
                <button
                  v-for="v in p.variants"
                  :key="v.id"
                  :disabled="!v.stock || !pos.active || pos.busy"
                  @click="safely(() => pos.scan(v.sku))"
                >
                  {{ v.color }} / {{ v.size }}<small>{{ money(v.price) }}</small>
                </button>
              </div>
            </article>
          </div>
          <p class="text-xs text-stone-500 mt-4">
            Hiển thị tối đa 48 sản phẩm mỗi lần tìm. Hóa đơn tạm không giữ chỗ tồn kho.
          </p>
        </div>
      </section>
      <aside class="admin-card pos-summary">
        <section class="pos-customer">
          <div class="flex justify-between items-center">
            <h2 class="font-semibold">Khách hàng</h2>
            <button class="text-link" :disabled="pos.busy || !pos.active" @click="chooseCustomer">
              Chọn khách
            </button>
          </div>
          <template v-if="pos.active?.customer"
            ><strong>{{ pos.active.customer.fullName }}</strong>
            <p>{{ pos.active.customer.phone }}</p>
            <p>{{ pos.active.customer.address || 'Chưa có địa chỉ' }}</p>
            <button
              class="text-sm underline mt-2"
              :disabled="pos.busy"
              @click="safely(() => pos.customer(null))"
            >
              Chuyển về khách lẻ
            </button></template
          >
          <p v-else class="text-stone-500 mt-3">Khách lẻ tại quầy</p>
        </section>
        <h2 class="text-xl font-semibold mb-5">Thanh toán tiền mặt</h2>
        <div class="flex justify-between mb-5">
          <span>Tiền hàng</span><b>{{ money(pos.active?.subtotal || 0) }}</b>
        </div>
        <form @submit.prevent="safely(() => pos.price(promotionCode))">
          <label class="field"
            >Mã giảm giá<input
              v-model.trim="promotionCode"
              maxlength="30"
              :disabled="pos.busy" /></label
          ><button
            class="button button-outline w-full"
            :disabled="pos.busy || !pos.active?.lines.length"
          >
            Kiểm tra mã / tính lại
          </button>
        </form>
        <div v-if="pos.quote" class="flex justify-between mt-5">
          <span>Giảm giá</span><b>− {{ money(pos.quote.discount) }}</b>
        </div>
        <div class="flex justify-between text-xl font-semibold border-t pt-5 mt-5">
          <span>Tổng tiền</span><span>{{ money(total) }}</span>
        </div>
        <label class="field mt-6"
          >Tiền khách đưa (₫)<input
            v-model.number="cash"
            type="number"
            min="0"
            step="1"
            :disabled="pos.busy"
        /></label>
        <div class="flex justify-between mb-6">
          <span>Tiền trả lại</span><strong>{{ money(change) }}</strong>
        </div>
        <button
          class="button w-full"
          :disabled="!canPay"
          @click="safely(() => pos.pay(cash, promotionCode))"
        >
          {{ pos.busy ? 'Đang xử lý…' : 'Xác nhận thanh toán' }}
        </button>
        <p class="text-xs text-stone-500 mt-4">
          Giá, khuyến mãi và tồn kho được kiểm tra lại khi chốt. Chưa hỗ trợ thanh toán VNPAY trong
          bản mới.
        </p>
        <button
          class="underline text-sm mt-4"
          :disabled="pos.busy"
          @click="safely(() => pos.load())"
        >
          Tải lại hóa đơn từ server
        </button>
      </aside>
    </div>
    <InvoiceReceipt v-if="pos.receipt" :invoice="pos.receipt" class="admin-card mt-5" />
    <dialog ref="customerDialog" class="media-dialog">
      <div class="flex justify-between mb-5">
        <h2 class="text-xl font-semibold">Chọn khách hàng</h2>
        <button class="icon-button" aria-label="Đóng chọn khách" @click="customerDialog.close()">
          ✕
        </button>
      </div>
      <form class="flex gap-3 mb-5" @submit.prevent="searchCustomers">
        <input
          v-model.trim="customerQuery"
          class="tool-input flex-1"
          placeholder="Tên hoặc số điện thoại…"
        /><button class="button button-outline">Tìm khách</button>
      </form>
      <p v-if="customerError" class="error-box" role="alert">{{ customerError }}</p>
      <div class="table-scroll">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Khách hàng</th>
              <th>Điện thoại</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in customerOptions" :key="c.id">
              <td>
                {{ c.profile.fullName }}<small>{{ c.code }}</small>
              </td>
              <td>{{ c.profile.phone }}</td>
              <td>
                <button
                  class="button button-outline"
                  :disabled="pos.busy"
                  @click="setCustomer(c.id)"
                >
                  Chọn
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!customerOptions.length" class="empty-state">
        Không tìm thấy khách. Tạo hồ sơ tại Quản lý tài khoản → Khách hàng.
      </p>
    </dialog>
  </div>
</template>
