<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { api, money } from '../../lib/api'
const today = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
}).format(new Date())
const filters = reactive({ from: today.slice(0, 8) + '01', to: today, group: 'day' }),
  data = ref(null),
  busy = ref(false),
  error = ref('')
const max = computed(() => Math.max(1, ...(data.value?.buckets.map((b) => b.revenue) || [])))
async function load() {
  busy.value = true
  error.value = ''
  try {
    data.value = await api('/admin/dashboard?' + new URLSearchParams(filters))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">PERFORMANCE / ASIA HO CHI MINH</p>
        <h1>Tổng quan kinh doanh</h1>
      </div>
    </div>
    <form class="admin-card dashboard-filters" @submit.prevent="load">
      <label class="field mb-0"
        >Từ ngày<input v-model="filters.from" type="date" required :max="filters.to" /></label
      ><label class="field mb-0"
        >Đến ngày<input v-model="filters.to" type="date" required :min="filters.from" /></label
      ><label class="field mb-0"
        >Nhóm theo<select v-model="filters.group">
          <option value="day">Ngày</option>
          <option value="month">Tháng</option>
        </select></label
      ><button class="button" :disabled="busy">{{ busy ? 'Đang tính…' : 'Xem thống kê' }}</button>
    </form>
    <p v-if="error" role="alert" class="error-box my-5">{{ error }}</p>
    <template v-if="data"
      ><div class="metric-grid">
        <article>
          <span>Doanh thu đã hoàn thành</span><strong>{{ money(data.revenue) }}</strong
          ><small>Sau giảm giá · theo thời điểm hoàn thành</small>
        </article>
        <article>
          <span>Đơn tạo trong kỳ</span><strong>{{ data.totalOrders }}</strong
          ><small>Gồm đơn chờ, hoàn thành và hủy</small>
        </article>
        <article>
          <span>Đơn thành công trong tập đơn tạo</span><strong>{{ data.successfulOrders }}</strong
          ><small>Đơn hiện ở trạng thái COMPLETED</small>
        </article>
        <article>
          <span>Tỷ lệ thành công</span><strong>{{ data.successRate }}%</strong
          ><small>Đơn hoàn thành / đơn tạo trong kỳ</small>
        </article>
      </div>
      <div class="admin-card mb-6">
        <h2 class="text-xl font-semibold mb-5">
          Doanh thu theo {{ filters.group === 'day' ? 'ngày' : 'tháng' }}
        </h2>
        <p class="text-xs text-stone-500 mb-5">
          Mốc thời gian Việt Nam (UTC+7). Không tính đơn chờ/hủy. Khoảng tối đa 366 ngày.
        </p>
        <div class="revenue-chart">
          <div v-for="b in data.buckets" :key="b.label" class="revenue-row">
            <span>{{ b.label }}</span>
            <div class="bar-track">
              <div :style="{ width: Math.max(b.revenue ? 1 : 0, (b.revenue / max) * 100) + '%' }" />
            </div>
            <strong>{{ money(b.revenue) }}</strong>
          </div>
        </div>
      </div>
      <div class="admin-card table-scroll">
        <h2 class="text-xl font-semibold mb-5">Top 10 sản phẩm bán chạy</h2>
        <table class="admin-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Số đôi đã bán</th>
              <th>Tiền hàng trước giảm giá</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in data.topProducts" :key="p.productId">
              <td>
                {{ p.name }}<small>{{ p.code }}</small>
              </td>
              <td>{{ p.quantity }}</td>
              <td>{{ money(p.grossRevenue) }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!data.topProducts.length" class="empty-state">
          Chưa có đơn hoàn thành trong khoảng này.
        </p>
      </div></template
    >
  </section>
</template>
