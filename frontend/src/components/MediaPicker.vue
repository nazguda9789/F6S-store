<script setup>
import { ref } from 'vue'
import { api } from '../lib/api'
const emit = defineEmits(['select']),
  dialog = ref(null),
  data = ref({ content: [], page: 0, totalPages: 0 }),
  q = ref(''),
  busy = ref(false),
  error = ref('')
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
function open() {
  dialog.value.showModal()
  load()
}
async function upload(event) {
  busy.value = true
  error.value = ''
  try {
    for (const file of [...event.target.files].slice(0, 10)) {
      const form = new FormData()
      form.append('file', file)
      await api('/admin/media', { method: 'POST', body: form })
    }
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
    event.target.value = ''
  }
}
function choose(item) {
  emit('select', item)
  dialog.value.close()
}
defineExpose({ open })
</script>
<template>
  <dialog ref="dialog" class="media-dialog">
    <div class="flex justify-between items-center mb-5">
      <h2 class="text-xl font-semibold">Chọn hình ảnh từ thư viện</h2>
      <button class="icon-button" aria-label="Đóng chọn ảnh" @click="dialog.close()">✕</button>
    </div>
    <p class="text-xs text-stone-500 mb-4">
      Chỉ tải ảnh sản phẩm được phép sử dụng. JPEG/PNG ≤5 MB; không tải CCCD, tài liệu hoặc ảnh
      riêng tư.
    </p>
    <div class="flex gap-3 mb-5">
      <input v-model.trim="q" class="tool-input flex-1" placeholder="Tìm tên ảnh…" /><button
        class="button button-outline"
        :disabled="busy"
        @click="load()"
      >
        Tìm</button
      ><label class="button cursor-pointer"
        >Tải ảnh<input
          type="file"
          accept="image/png,image/jpeg"
          multiple
          class="hidden"
          :disabled="busy"
          @change="upload"
      /></label>
    </div>
    <p v-if="error" role="alert" class="error-box mb-4">{{ error }}</p>
    <div class="media-grid">
      <button v-for="item in data.content" :key="item.id" class="media-item" @click="choose(item)">
        <img :src="item.url" :alt="item.name" /><span>{{ item.name }}</span
        ><small>{{ item.width }} × {{ item.height }}</small>
      </button>
    </div>
    <p v-if="!data.content.length && !busy" class="empty-state">
      Chưa có ảnh. Tải ảnh thật đúng sản phẩm để sử dụng.
    </p>
    <div v-if="data.totalPages > 1" class="pagination">
      <button :disabled="busy || !data.page" @click="load(data.page - 1)">Trước</button
      ><span>{{ data.page + 1 }} / {{ data.totalPages }}</span
      ><button :disabled="busy || data.page + 1 >= data.totalPages" @click="load(data.page + 1)">
        Sau
      </button>
    </div>
  </dialog>
</template>
