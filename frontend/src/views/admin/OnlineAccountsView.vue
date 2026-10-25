<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../lib/api'
const data = ref({ content: [], page: 0, totalPages: 0 }),
  q = ref(''),
  error = ref(''),
  busy = ref(false)
async function load(page = 0) {
  busy.value = true
  error.value = ''
  try {
    data.value = await api('/admin/shop-accounts?' + new URLSearchParams({ q: q.value, page }))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function toggle(row) {
  if (!confirm((row.active ? 'Ngừng' : 'Bật lại') + ' tài khoản ' + row.email + '?')) return
  busy.value = true
  error.value = ''
  try {
    await api('/admin/shop-accounts/' + row.id, {
      method: 'PUT',
      body: JSON.stringify({ active: !row.active }),
    })
    await load(data.value.page)
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
    <div class="admin-heading"><h1>Tài khoản khách online</h1></div>
    <p class="text-sm text-stone-500 mb-5">
      Tạo qua email OTP, không phải hồ sơ khách tại quầy. Ngừng hoạt động sẽ chặn phiên hiện tại từ
      yêu cầu tiếp theo; lịch sử đơn không bị xóa.
    </p>
    <p v-if="error" role="alert" class="error-box mb-5">{{ error }}</p>
    <form class="admin-card flex gap-4 items-end mb-5" @submit.prevent="load()">
      <label class="field flex-1 mb-0"
        >Tìm email hoặc tên<input v-model.trim="q" maxlength="120" /></label
      ><button class="button" :disabled="busy">Tìm kiếm</button>
    </form>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Email đã xác minh</th>
            <th>Họ tên</th>
            <th>Điện thoại</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in data.content" :key="row.id">
            <td>{{ row.email }}</td>
            <td>{{ row.fullName || 'Chưa cập nhật' }}</td>
            <td>{{ row.phone || '—' }}</td>
            <td>{{ row.active ? 'Hoạt động' : 'Ngừng hoạt động' }}</td>
            <td>
              <button class="text-link" :disabled="busy" @click="toggle(row)">
                {{ row.active ? 'Ngừng hoạt động' : 'Bật lại' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!data.content.length" class="empty-state">Chưa có tài khoản phù hợp.</p>
      <div class="pagination" v-if="data.totalPages > 1">
        <button :disabled="busy || !data.page" @click="load(data.page - 1)">Trước</button
        ><span>{{ data.page + 1 }} / {{ data.totalPages }}</span
        ><button :disabled="busy || data.page + 1 >= data.totalPages" @click="load(data.page + 1)">
          Sau
        </button>
      </div>
    </div>
  </section>
</template>
