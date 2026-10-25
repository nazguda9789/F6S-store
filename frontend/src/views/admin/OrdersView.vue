<script setup>
import { ref, reactive, onMounted } from 'vue'
import { api, money } from '../../lib/api'
import InvoiceReceipt from '../../components/InvoiceReceipt.vue'
const labels = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PACKING: 'Chờ giao hàng',
  SHIPPING: 'Đang giao',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
}
const next = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'PACKING',
  PACKING: 'SHIPPING',
  SHIPPING: 'COMPLETED',
}
const actions = {
  CONFIRMED: 'Xác nhận đơn',
  PACKING: 'Chuẩn bị xong',
  SHIPPING: 'Giao vận chuyển',
  COMPLETED: 'Xác nhận đã giao / thu tiền',
}
const filters = reactive({ q: '', status: '', channel: '' }),
  shipment = reactive({ carrier: 'Giao hàng demo', trackingCode: '' }),
  refund = ref(null),
  refundReason = ref(''),
  refundReference = ref(''),
  goodsReceived = ref(false),
  data = ref({ content: [], page: 0, totalPages: 0, totalElements: 0 }),
  selected = ref(null),
  busy = ref(false),
  error = ref('')
async function load(page = 0) {
  busy.value = true
  error.value = ''
  try {
    data.value = await api('/admin/orders?' + new URLSearchParams({ ...filters, page }))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function detail(entry) {
  busy.value = true
  error.value = ''
  try {
    selected.value = await api('/admin/orders/' + entry.id)
    Object.assign(shipment, {
      carrier: selected.value.carrier || 'Giao hàng demo',
      trackingCode: selected.value.trackingCode || '',
    })
    refund.value = await api('/admin/orders/' + entry.id + '/refund')
    refundReason.value = ''
    refundReference.value = ''
    goodsReceived.value = false
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function transition(entry, status) {
  const message =
    status === 'CANCELLED'
      ? 'Hủy trước khi giao vận chuyển và hoàn lại tồn kho?'
      : status === 'COMPLETED'
        ? 'Xác nhận đã giao thành công và đã thu đủ tiền?'
        : 'Chuyển bước: ' + labels[status] + '?'
  if (!confirm(message)) return
  busy.value = true
  error.value = ''
  try {
    await api('/admin/orders/' + entry.id + '/status', {
      method: 'PUT',
      body: JSON.stringify({
        status,
        expectedStatus: entry.order.status,
        reason: status === 'CANCELLED' ? 'Hủy trước khi bàn giao vận chuyển' : '',
      }),
    })
    if (selected.value?.id === entry.id) selected.value = await api('/admin/orders/' + entry.id)
    await load(data.value.page)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
function selectStatus(value) {
  filters.status = value
  load()
}
async function afterSales(work) {
  busy.value = true
  error.value = ''
  try {
    await work()
    selected.value = await api('/admin/orders/' + selected.value.id)
    refund.value = await api('/admin/orders/' + selected.value.id + '/refund')
    await load(data.value.page)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function saveShipment() {
  await afterSales(() =>
    api('/admin/orders/' + selected.value.id + '/shipment', {
      method: 'PUT',
      body: JSON.stringify(shipment),
    }),
  )
}
async function requestRefund() {
  await afterSales(() =>
    api('/admin/orders/' + selected.value.id + '/refund', {
      method: 'POST',
      body: JSON.stringify({ reason: refundReason.value }),
    }),
  )
}
async function resolveRefund(approve) {
  if (
    !confirm(
      approve
        ? 'Chỉ xác nhận nếu đã thực hiện hoàn tiền thực tế và có chứng từ. Tiếp tục?'
        : 'Từ chối yêu cầu hoàn?',
    )
  )
    return
  await afterSales(() =>
    api('/admin/orders/' + selected.value.id + '/refund', {
      method: 'PUT',
      body: JSON.stringify({
        approve,
        goodsReceived: goodsReceived.value,
        settlementReference: refundReference.value,
      }),
    }),
  )
}
onMounted(() => load())
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">TRANSACTIONS / INVOICE MANAGEMENT</p>
        <h1>Danh sách hóa đơn</h1>
        <p class="text-sm text-stone-500 mt-2">{{ data.totalElements }} đơn phù hợp</p>
      </div>
      <button class="button button-outline" :disabled="busy" @click="load(data.page)">
        Tải lại
      </button>
    </div>
    <p v-if="error" role="alert" class="error-box mb-5">{{ error }}</p>
    <div class="order-status-tabs" role="group" aria-label="Trạng thái hóa đơn">
      <button :class="{ selected: !filters.status }" :disabled="busy" @click="selectStatus('')">
        Tất cả</button
      ><button
        v-for="(name, key) in labels"
        :key="key"
        :class="{ selected: filters.status === key }"
        :disabled="busy"
        @click="selectStatus(key)"
      >
        {{ name }}
      </button>
    </div>
    <form class="admin-card flex flex-wrap gap-4 items-end mb-5" @submit.prevent="load()">
      <label class="field flex-1 mb-0"
        >Mã đơn / Khách hàng / Số điện thoại<input
          v-model.trim="filters.q"
          maxlength="120" /></label
      ><label class="field mb-0"
        >Loại đơn<select v-model="filters.channel">
          <option value="">Tất cả</option>
          <option value="ONLINE">Online</option>
          <option value="POS">Tại quầy</option>
        </select></label
      ><button class="button" :disabled="busy">Lọc hóa đơn</button>
    </form>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã đơn / Ngày tạo</th>
            <th>Khách hàng</th>
            <th>Loại đơn</th>
            <th>Tổng tiền</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in data.content" :key="entry.id">
            <td>
              <strong>{{ entry.order.code }}</strong
              ><small>{{ new Date(entry.order.createdAt).toLocaleString('vi-VN') }}</small>
            </td>
            <td>
              {{ entry.customerName }}<small>{{ entry.phone || 'Khách lẻ' }}</small>
            </td>
            <td>
              <span class="status-pill">{{
                entry.order.channel === 'POS' ? 'Tại quầy' : 'Online'
              }}</span>
            </td>
            <td>{{ money(entry.order.total) }}</td>
            <td>{{ labels[entry.order.status] || entry.order.status }}</td>
            <td>
              <div class="flex gap-3 flex-wrap">
                <button class="text-link" :disabled="busy" @click="detail(entry)">
                  Chi tiết / In</button
                ><button
                  v-if="next[entry.order.status]"
                  class="text-link"
                  :disabled="busy"
                  @click="transition(entry, next[entry.order.status])"
                >
                  {{ actions[next[entry.order.status]] }}</button
                ><button
                  v-if="['PENDING', 'CONFIRMED', 'PACKING'].includes(entry.order.status)"
                  class="text-red-700"
                  :disabled="busy"
                  @click="transition(entry, 'CANCELLED')"
                >
                  Hủy đơn
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!data.content.length" class="empty-state">Không có hóa đơn phù hợp.</p>
      <div v-if="data.totalPages > 1" class="pagination">
        <button :disabled="busy || !data.page" @click="load(data.page - 1)">Trước</button
        ><span>{{ data.page + 1 }} / {{ data.totalPages }}</span
        ><button :disabled="busy || data.page + 1 >= data.totalPages" @click="load(data.page + 1)">
          Sau
        </button>
      </div>
    </div>
    <section v-if="selected" class="admin-card mt-6 no-print">
      <div class="flex justify-between mb-5">
        <h2 class="text-xl font-semibold">Chi tiết hóa đơn {{ selected.order.code }}</h2>
        <button class="text-link" @click="selected = null">Đóng chi tiết</button>
      </div>
      <ol class="order-timeline">
        <li v-for="event in selected.timeline" :key="event.id">
          <span></span>
          <div>
            <strong>{{ labels[event.toStatus] || event.toStatus }}</strong
            ><small
              >{{ new Date(event.createdAt).toLocaleString('vi-VN') }} · {{ event.actor }}</small
            >
            <p v-if="event.note">{{ event.note }}</p>
          </div>
        </li>
      </ol>
      <p v-if="!selected.timeline?.length" class="text-sm text-stone-500 mb-5">
        Đơn cũ chưa có sự kiện lịch sử; hệ thống không tự dựng mốc thời gian giả.
      </p>
      <div class="customer-summary">
        <h3 class="font-semibold">Khách hàng / Giao hàng</h3>
        <p>{{ selected.customerName }} · {{ selected.phone || 'Khách lẻ tại quầy' }}</p>
        <p>{{ selected.email }}<br />{{ selected.address }}</p>
        <p v-if="selected.note">Ghi chú: {{ selected.note }}</p>
        <p class="mt-3">
          Thanh toán: {{ selected.order.paymentMethod }} / {{ selected.order.paymentStatus }} · Phí
          giao {{ money(selected.order.shippingFee) }}
        </p>
        <p v-if="selected.trackingCode">
          Vận đơn: {{ selected.carrier }} · {{ selected.trackingCode }}
        </p>
      </div>
      <form
        v-if="
          selected.order.channel === 'ONLINE' &&
          ['CONFIRMED', 'PACKING'].includes(selected.order.status)
        "
        class="admin-card mt-5"
        @submit.prevent="saveShipment"
      >
        <h3 class="font-semibold mb-4">Giao hàng demo / vận đơn thủ công</h3>
        <label class="field"
          >Đơn vị giao hàng<input v-model.trim="shipment.carrier" required maxlength="60" /></label
        ><label class="field"
          >Mã vận đơn<input
            v-model.trim="shipment.trackingCode"
            required
            pattern="[A-Za-z0-9._-]{3,100}"
            maxlength="100"
            placeholder="DEMO-001" /></label
        ><button class="button" :disabled="busy">Lưu vận đơn</button>
      </form>
      <section
        v-if="
          ['COMPLETED', 'CANCELLED'].includes(selected.order.status) &&
          ['PAID', 'REFUNDED'].includes(selected.order.paymentStatus)
        "
        class="admin-card mt-5"
      >
        <h3 class="font-semibold mb-4">Trả hàng / Hoàn toàn bộ tiền</h3>
        <p class="text-sm mb-4">
          Hoàn tiền được thực hiện ngoài hệ thống (tiền mặt/chuyển khoản/cổng VNPAY), sau đó ghi mã
          chứng từ để đối soát. Không tự chuyển tiền khi bấm nút.
        </p>
        <form v-if="!refund" @submit.prevent="requestRefund">
          <label class="field"
            >Lý do hoàn<textarea
              v-model.trim="refundReason"
              required
              minlength="5"
              maxlength="500"
            /></label
          ><button class="button" :disabled="busy">Mở yêu cầu hoàn</button>
        </form>
        <template v-else
          ><p>{{ refund.status }} · {{ money(refund.amount) }} · {{ refund.reason }}</p>
          <p v-if="refund.settlementReference">Chứng từ: {{ refund.settlementReference }}</p>
          <form
            v-if="refund.status === 'REQUESTED'"
            class="mt-4"
            @submit.prevent="resolveRefund(true)"
          >
            <label class="field"
              >Mã chứng từ đã hoàn tiền<input
                v-model.trim="refundReference"
                required
                maxlength="120" /></label
            ><label v-if="selected.order.status === 'COMPLETED'" class="block mb-4"
              ><input v-model="goodsReceived" type="checkbox" required /> Đã nhận và kiểm tra toàn
              bộ hàng trả về kho</label
            ><button class="button" :disabled="busy">Xác nhận đã hoàn tiền</button
            ><button
              type="button"
              class="button button-outline ml-3"
              :disabled="busy"
              @click="resolveRefund(false)"
            >
              Từ chối yêu cầu
            </button>
          </form></template
        >
      </section>
    </section>
    <InvoiceReceipt v-if="selected" :invoice="selected.order" class="admin-card mt-6" />
    <p class="text-xs text-stone-500 mt-5">
      Không bỏ qua bước xử lý. Chỉ hủy trước giao vận chuyển; hệ thống hoàn tồn và lượt mã đúng một
      lần. Hoàn tiền chỉ ghi nhận sau chứng từ thực tế; trả hàng hỗ trợ toàn bộ đơn, không hỗ trợ
      trả từng phần.
    </p>
  </section>
</template>
