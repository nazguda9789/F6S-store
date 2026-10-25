<script setup>
import { ref, reactive, onMounted } from 'vue'
import { api } from '../../lib/api'
const types = {
    BRAND: 'Thương hiệu',
    SOLE: 'Loại đế',
    MATERIAL: 'Chất liệu',
    COLOR: 'Màu sắc',
    SIZE: 'Kích cỡ',
  },
  type = ref('BRAND'),
  rows = ref([]),
  busy = ref(false),
  error = ref(''),
  editing = ref(null),
  form = reactive({ name: '', active: true })
async function load() {
  busy.value = true
  error.value = ''
  try {
    rows.value = await api('/admin/attributes?type=' + type.value + '&includeInactive=true')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function save() {
  busy.value = true
  error.value = ''
  try {
    await api('/admin/attributes' + (editing.value ? '/' + editing.value : ''), {
      method: editing.value ? 'PUT' : 'POST',
      body: JSON.stringify({ type: type.value, ...form }),
    })
    editing.value = null
    form.name = ''
    form.active = true
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
function select(key) {
  type.value = key
  editing.value = null
  form.name = ''
  form.active = true
  load()
}
function edit(row) {
  editing.value = row.id
  form.name = row.name
  form.active = row.active
}
function cancelEdit() {
  Object.assign(form, { name: '', active: true })
  editing.value = null
}
onMounted(load)
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">PRODUCT ATTRIBUTES</p>
        <h1>Thuộc tính sản phẩm</h1>
      </div>
    </div>
    <div class="attribute-tabs" role="group" aria-label="Nhóm thuộc tính">
      <button
        v-for="(name, key) in types"
        :key="key"
        :class="{ selected: type === key }"
        :disabled="busy"
        @click="select(key)"
      >
        {{ name }}
      </button>
    </div>
    <p v-if="error" class="error-box mb-5" role="alert">{{ error }}</p>
    <form class="admin-card flex gap-4 flex-wrap items-end mb-5" @submit.prevent="save">
      <label class="field flex-1 mb-0"
        >{{ editing ? 'Sửa' : 'Thêm' }} {{ types[type].toLowerCase()
        }}<input
          v-model.trim="form.name"
          required
          :maxlength="type === 'COLOR' ? 60 : 100" /></label
      ><label class="field mb-0"
        >Trạng thái<select v-model="form.active">
          <option :value="true">Đang dùng</option>
          <option :value="false">Ngừng dùng</option>
        </select></label
      ><button class="button" :disabled="busy">
        {{ editing ? 'Cập nhật' : 'Thêm thuộc tính' }}</button
      ><button v-if="editing" type="button" class="button button-outline" @click="cancelEdit">
        Hủy sửa
      </button>
    </form>
    <p class="text-xs text-stone-500 mb-5">
      Ngừng dùng không xóa sản phẩm hoặc lịch sử. Thuộc tính đã tham chiếu không được đổi tên làm
      sai màu/size/SKU; hãy tạo giá trị mới.
    </p>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã</th>
            <th>Tên {{ types[type].toLowerCase() }}</th>
            <th>Trạng thái</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.code }}</td>
            <td>{{ row.name }}</td>
            <td>
              <span class="status-pill">{{ row.active ? 'Đang dùng' : 'Ngừng dùng' }}</span>
            </td>
            <td>
              <button class="text-link" :disabled="busy" @click="edit(row)">Chỉnh sửa</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
