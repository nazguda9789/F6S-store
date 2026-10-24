<script setup>
import { ref, reactive, onMounted } from 'vue'
import { api, money } from '../../lib/api'
const promos = ref([]),
  products = ref([]),
  busy = ref(false),
  error = ref(''),
  editing = ref(null),
  showForm = ref(false)
function localDate(date) {
  return new Date(new Date(date).getTime() - new Date(date).getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16)
}
const form = reactive({
  code: '',
  name: '',
  type: 'PERCENT',
  value: 10,
  minSubtotal: 0,
  maxDiscount: '',
  maxUses: 100,
  startsAt: localDate(Date.now() - 60000),
  endsAt: localDate(Date.now() + 7 * 86400000),
  active: true,
  productIds: [],
})
function promotionStatus(p) {
  if (!p.active) return 'Ngừng hoạt động'
  if (Date.now() < new Date(p.startsAt).getTime()) return 'Chưa bắt đầu'
  if (Date.now() >= new Date(p.endsAt).getTime()) return 'Hết hạn'
  if (p.usedCount >= p.maxUses) return 'Hết lượt'
  return 'Đang áp dụng'
}
async function run(work) {
  busy.value = true
  error.value = ''
  try {
    await work()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function load() {
  promos.value = await api('/admin/promotions')
}
function add() {
  editing.value = null
  showForm.value = true
  Object.assign(form, {
    code: '',
    name: '',
    type: 'PERCENT',
    value: 10,
    minSubtotal: 0,
    maxDiscount: '',
    maxUses: 100,
    startsAt: localDate(Date.now() - 60000),
    endsAt: localDate(Date.now() + 7 * 86400000),
    active: true,
    productIds: [],
  })
}
function edit(p) {
  editing.value = p.id
  showForm.value = true
  Object.assign(form, {
    ...p,
    maxDiscount: p.maxDiscount ?? '',
    startsAt: localDate(p.startsAt),
    endsAt: localDate(p.endsAt),
    productIds: [...p.productIds],
  })
}
async function save() {
  await run(async () => {
    const body = {
      code: form.code.trim().toUpperCase(),
      name: form.name,
      type: form.type,
      value: form.value,
      minSubtotal: form.minSubtotal,
      maxDiscount: form.maxDiscount === '' ? null : Number(form.maxDiscount),
      maxUses: form.maxUses,
      startsAt: new Date(form.startsAt).toISOString(),
      endsAt: new Date(form.endsAt).toISOString(),
      active: form.active,
      productIds: form.productIds,
    }
    await api('/admin/promotions' + (editing.value ? '/' + editing.value : ''), {
      method: editing.value ? 'PUT' : 'POST',
      body: JSON.stringify(body),
    })
    showForm.value = false
    await load()
  })
}
async function disable(p) {
  if (confirm('Ngừng mã ' + p.code + '?'))
    await run(async () => {
      await api('/admin/promotions/' + p.id, { method: 'DELETE' })
      await load()
    })
}
onMounted(() =>
  run(async () => {
    let page = 0
    const all = []
    let result
    do {
      result = await api('/products?limit=48&page=' + page++)
      all.push(...result.content)
    } while (page < result.totalPages)
    products.value = all
    await load()
  }),
)
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">PROMOTIONS / PRODUCT SCOPE</p>
        <h1>Quản lý khuyến mãi</h1>
      </div>
      <button class="button" :disabled="busy" @click="add">+ Tạo khuyến mãi</button>
    </div>
    <p v-if="error" class="error-box mb-5" role="alert">{{ error }}</p>
    <form v-if="showForm" class="admin-card mb-6" @submit.prevent="save">
      <h2 class="text-xl font-semibold mb-6">
        {{ editing ? 'Sửa mã giảm giá' : 'Tạo mã giảm giá' }}
      </h2>
      <fieldset :disabled="busy">
        <div class="admin-form-grid">
          <label class="field"
            >Mã<input
              v-model.trim="form.code"
              pattern="[A-Za-z0-9_-]{3,30}"
              required
              :readonly="!!editing" /></label
          ><label class="field"
            >Tên khuyến mãi<input v-model.trim="form.name" maxlength="120" required /></label
          ><label class="field"
            >Loại giảm<select v-model="form.type">
              <option value="PERCENT">Phần trăm (%)</option>
              <option value="FIXED">Số tiền cố định (₫)</option>
            </select></label
          ><label class="field"
            >Giá trị giảm<input
              v-model.number="form.value"
              type="number"
              min="1"
              :max="form.type === 'PERCENT' ? 100 : 1000000000"
              step="1"
              required /></label
          ><label class="field"
            >Tiền hàng áp dụng tối thiểu<input
              v-model.number="form.minSubtotal"
              type="number"
              min="0"
              step="1"
              required /></label
          ><label class="field"
            >Giảm tối đa (bỏ trống = không giới hạn)<input
              v-model="form.maxDiscount"
              type="number"
              min="1"
              step="1" /></label
          ><label class="field"
            >Bắt đầu<input v-model="form.startsAt" type="datetime-local" required /></label
          ><label class="field"
            >Kết thúc<input v-model="form.endsAt" type="datetime-local" required /></label
          ><label class="field"
            >Tổng lượt tối đa<input
              v-model.number="form.maxUses"
              type="number"
              min="1"
              max="1000000"
              step="1"
              required /></label
          ><label class="flex gap-2 items-center"
            ><input v-model="form.active" type="checkbox" />Đang hoạt động</label
          >
        </div>
        <h3 class="font-semibold mb-3">Chỉ áp dụng cho sản phẩm được chọn</h3>
        <div class="promotion-products">
          <label v-for="p in products" :key="p.id"
            ><input v-model="form.productIds" type="checkbox" :value="p.id" />{{ p.name }}
            <small>{{ p.code }}</small></label
          >
        </div>
        <p class="text-xs text-stone-500 my-4">
          {{ form.productIds.length }} sản phẩm đã chọn. Giảm trên tổng tiền của sản phẩm đủ điều
          kiện, không giảm phần hàng ngoài danh sách. Giảm % làm tròn xuống đến đồng.
        </p>
        <div class="flex gap-3">
          <button class="button" :disabled="!form.productIds.length">Lưu khuyến mãi</button
          ><button type="button" class="button button-outline" @click="showForm = false">
            Đóng
          </button>
        </div>
      </fieldset>
    </form>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã / Tên</th>
            <th>Giảm</th>
            <th>Lượt dùng</th>
            <th>Thời hạn</th>
            <th>Trạng thái</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in promos" :key="p.id">
            <td>
              <strong>{{ p.code }}</strong
              ><small>{{ p.name }}</small>
            </td>
            <td>
              {{ p.type === 'PERCENT' ? p.value + '%' : money(p.value)
              }}<small>{{ p.productIds.length }} sản phẩm</small>
            </td>
            <td>{{ p.usedCount }} / {{ p.maxUses }}</td>
            <td>
              {{ new Date(p.startsAt).toLocaleDateString('vi-VN') }} →
              {{ new Date(p.endsAt).toLocaleDateString('vi-VN') }}
            </td>
            <td>
              <span class="status-pill">{{ promotionStatus(p) }}</span>
            </td>
            <td>
              <div class="flex gap-3">
                <button class="text-link" :disabled="busy" @click="edit(p)">Sửa</button
                ><button class="text-red-700" :disabled="busy || !p.active" @click="disable(p)">
                  Ngừng
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!promos.length" class="empty-state">
        Chưa có khuyến mãi. Tạo mã và chọn sản phẩm áp dụng.
      </p>
    </div>
  </section>
</template>
