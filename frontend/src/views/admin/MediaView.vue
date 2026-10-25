<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../lib/api'
const data = ref({ content: [], page: 0, totalPages: 0 }),
  q = ref(''),
  busy = ref(false),
  error = ref(''),
  success = ref('')
async function load(page = 0) {
  busy.value = true
  error.value = ''
  try {
    data.value = await api('/admin/media?' + new URLSearchParams({ q: q.value, page }))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function upload(event) {
  busy.value = true
  error.value = ''
  success.value = ''
  let count = 0
  try {
    for (const file of [...event.target.files].slice(0, 10)) {
      const body = new FormData()
      body.append('file', file)
      await api('/admin/media', { method: 'POST', body })
      count++
    }
    success.value = `Đã nhận ${count} ảnh (ảnh trùng được tái sử dụng).`
    await load()
  } catch (e) {
    error.value = `Đã nhận ${count} ảnh. ` + e.message
  } finally {
    busy.value = false
    event.target.value = ''
  }
}
async function remove(item) {
  if (!confirm('Ẩn ảnh này khỏi thư viện?')) return
  busy.value = true
  error.value = ''
  try {
    await api('/admin/media/' + item.id, { method: 'DELETE' })
    await load(data.value.page)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function copy(item) {
  try {
    await navigator.clipboard.writeText(item.url)
    success.value = 'Đã chép đường dẫn ảnh'
  } catch {
    error.value = 'Trình duyệt chưa cấp quyền clipboard. Đường dẫn: ' + item.url
  }
}
onMounted(() => load())
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">MEDIA LIBRARY</p>
        <h1>Thư viện hình ảnh</h1>
      </div>
      <label class="button cursor-pointer"
        >+ Tải ảnh sản phẩm<input
          class="hidden"
          type="file"
          accept="image/png,image/jpeg"
          multiple
          :disabled="busy"
          @change="upload"
      /></label>
    </div>
    <p class="text-sm text-stone-500 mb-5">
      Ảnh công khai cho sản phẩm. Chỉ tải JPEG/PNG có quyền sử dụng, tối đa 5 MB/ảnh và 10 ảnh/lần.
      Ảnh được kiểm tra, mã hóa lại để bỏ metadata; không tải dữ liệu riêng tư.
    </p>
    <p v-if="error" class="error-box mb-5" role="alert">{{ error }}</p>
    <p v-if="success" class="success-message" role="status">{{ success }}</p>
    <form class="admin-card flex gap-3 mb-6" @submit.prevent="load()">
      <input
        v-model.trim="q"
        class="tool-input flex-1"
        placeholder="Tìm tên ảnh…"
        maxlength="120"
      /><button class="button button-outline" :disabled="busy">Tìm ảnh</button>
    </form>
    <div class="media-grid">
      <article v-for="item in data.content" :key="item.id" class="media-item">
        <img :src="item.url" :alt="item.name" loading="lazy" /><strong>{{ item.name }}</strong
        ><small
          >{{ item.width }} × {{ item.height }} · {{ Math.ceil(item.byteSize / 1024) }} KB</small
        >
        <div class="flex gap-3 mt-3">
          <button class="text-link" @click="copy(item)">Chép URL</button
          ><button class="text-red-700 text-sm" :disabled="busy" @click="remove(item)">
            Ẩn ảnh
          </button>
        </div>
      </article>
    </div>
    <p v-if="!data.content.length && !busy" class="empty-state">
      Chưa có ảnh phù hợp. Không tự lấy ảnh giày khác để gắn sai model.
    </p>
    <div v-if="data.totalPages > 1" class="pagination">
      <button :disabled="busy || !data.page" @click="load(data.page - 1)">Trước</button
      ><span>{{ data.page + 1 }} / {{ data.totalPages }}</span
      ><button :disabled="busy || data.page + 1 >= data.totalPages" @click="load(data.page + 1)">
        Sau
      </button>
    </div>
  </section>
</template>
