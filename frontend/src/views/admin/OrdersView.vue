<script setup>
import { ref, onMounted } from 'vue'
import { api, money } from '../../lib/api'
import InvoiceReceipt from '../../components/InvoiceReceipt.vue'
const data = ref({ content: [], page: 0, totalPages: 0 }),
  selected = ref(null),
  busy = ref(false),
  error = ref('')
async function load(page = 0) {
  busy.value = true
  error.value = ''
  try {
    data.value = await api('/admin/orders?page=' + page)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function status(entry, status) {
  if (
    !confirm(
      status === 'CANCELLED'
        ? 'Hủy đơn và hoàn lại tồn kho?'
        : 'Xác nhận đơn đã giao và thu đủ tiền?',
    )
  )
    return
  busy.value = true
  error.value = ''
  try {
    const result = await api('/admin/orders/' + entry.id + '/status', {
      method: 'PUT',
      body: JSON.stringify({ status }),
    })
    entry.order = result
    if (selected.value?.order.code === result.code) selected.value.order = result
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
onMounted(() => load())
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">ORDER MANAGEMENT</p>
        <h1>Quản lý đơn hàng</h1>
      </div>
      <button class="button button-outline" :disabled="busy" @click="load(data.page)">
        Tải lại
      </button>
    </div>
    <p v-if="error" class="error-box mb-5" role="alert">{{ error }}</p>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã đơn / Ngày</th>
            <th>Kênh</th>
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
            <td>{{ entry.order.channel }} / {{ entry.order.paymentMethod }}</td>
            <td>{{ money(entry.order.total) }}</td>
            <td>
              <span class="status-pill">{{ entry.order.status }}</span>
            </td>
            <td>
              <div class="flex gap-3 flex-wrap">
                <button class="text-link" @click="selected = entry">Chi tiết / In</button
                ><template v-if="entry.order.status === 'PENDING'"
                  ><button class="text-link" :disabled="busy" @click="status(entry, 'COMPLETED')">
                    Hoàn thành</button
                  ><button
                    class="text-red-700"
                    :disabled="busy"
                    @click="status(entry, 'CANCELLED')"
                  >
                    Hủy đơn
                  </button></template
                >
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!data.content.length" class="empty-state">Chưa có đơn hàng.</p>
      <div v-if="data.totalPages > 1" class="pagination">
        <button :disabled="busy || !data.page" @click="load(data.page - 1)">Trước</button
        ><span>{{ data.page + 1 }} / {{ data.totalPages }}</span
        ><button :disabled="busy || data.page + 1 >= data.totalPages" @click="load(data.page + 1)">
          Sau
        </button>
      </div>
    </div>
    <p class="text-xs text-stone-500 mt-4">
      Chỉ xác nhận hoàn thành khi đã giao/thu tiền. Hủy đơn chờ hoàn tồn đúng một lần; đơn đã hoàn
      thành không tự động hoàn tiền hoặc hoàn kho.
    </p>
    <section v-if="selected" class="admin-card mt-6 no-print">
      <h2 class="font-semibold mb-4">Thông tin khách hàng / giao hàng</h2>
      <p>{{ selected.customerName }} · {{ selected.phone || 'Khách lẻ tại quầy' }}</p>
      <p class="text-sm text-stone-500 mt-2">{{ selected.email }}<br />{{ selected.address }}</p>
      <p v-if="selected.note" class="text-sm mt-3">Ghi chú: {{ selected.note }}</p>
    </section>
    <InvoiceReceipt v-if="selected" :invoice="selected.order" class="admin-card mt-6" />
  </section>
</template>
