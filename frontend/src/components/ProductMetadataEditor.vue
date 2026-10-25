<script setup>
import { ref, watch, onMounted } from 'vue'
import { api } from '../lib/api'
import MediaPicker from './MediaPicker.vue'
const props = defineProps({ modelValue: Object, brand: String, colors: Array }),
  emit = defineEmits(['update:modelValue', 'update:brand']),
  attributes = ref({ BRAND: [], SOLE: [], MATERIAL: [] }),
  picker = ref(null),
  selectedColor = ref(''),
  error = ref(''),
  newType = ref('BRAND'),
  newName = ref(''),
  busy = ref(false)
function set(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
function brand(event) {
  const id = Number(event.target.value) || null
  set('brandId', id)
  const item = attributes.value.BRAND.find((x) => x.id === id)
  if (item) emit('update:brand', item.name)
}
function pick(color) {
  selectedColor.value = color
  picker.value.open()
}
function image(item) {
  const images = props.modelValue.images || []
  if (images.some((i) => i.color === selectedColor.value && i.mediaId === item.id)) return
  if (images.filter((i) => i.color === selectedColor.value).length >= 5) {
    error.value = 'Tối đa 5 ảnh/màu'
    return
  }
  set('images', [...images, { color: selectedColor.value, mediaId: item.id, url: item.url }])
}
function remove(index) {
  set(
    'images',
    props.modelValue.images.filter((_, i) => i !== index),
  )
}
async function add() {
  if (!newName.value.trim()) return
  busy.value = true
  error.value = ''
  try {
    const item = await api('/admin/attributes', {
      method: 'POST',
      body: JSON.stringify({ type: newType.value, name: newName.value.trim(), active: true }),
    })
    attributes.value[newType.value].push(item)
    if (newType.value === 'BRAND') {
      set('brandId', item.id)
      emit('update:brand', item.name)
    } else set(newType.value === 'SOLE' ? 'soleId' : 'materialId', item.id)
    newName.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
watch(
  () => props.colors,
  (colors) => {
    if ((props.modelValue.images || []).some((i) => !colors.includes(i.color)))
      set(
        'images',
        props.modelValue.images.filter((i) => colors.includes(i.color)),
      )
  },
  { deep: true },
)
onMounted(async () => {
  try {
    for (const type of ['BRAND', 'SOLE', 'MATERIAL'])
      attributes.value[type] = await api('/admin/attributes?type=' + type)
    const found = attributes.value.BRAND.find((b) => b.name === props.brand)
    if (found && !props.modelValue.brandId) set('brandId', found.id)
  } catch (e) {
    error.value = e.message
  }
})
</script>
<template>
  <section class="product-metadata">
    <h3 class="font-semibold mb-4">Thuộc tính giày & ảnh theo màu</h3>
    <p v-if="error" role="alert" class="error-box mb-4">{{ error }}</p>
    <div class="admin-form-grid">
      <label class="field"
        >Thương hiệu từ danh mục<select :value="modelValue.brandId || ''" @change="brand">
          <option value="">{{ brand || 'Chọn thương hiệu' }}</option>
          <option v-for="a in attributes.BRAND" :key="a.id" :value="a.id">{{ a.name }}</option>
        </select></label
      ><label class="field"
        >Loại đế<select
          :value="modelValue.soleId || ''"
          @change="set('soleId', Number($event.target.value) || null)"
        >
          <option value="">Chưa xác nhận</option>
          <option v-for="a in attributes.SOLE" :key="a.id" :value="a.id">{{ a.name }}</option>
        </select></label
      ><label class="field"
        >Chất liệu<select
          :value="modelValue.materialId || ''"
          @change="set('materialId', Number($event.target.value) || null)"
        >
          <option value="">Chưa xác nhận</option>
          <option v-for="a in attributes.MATERIAL" :key="a.id" :value="a.id">{{ a.name }}</option>
        </select></label
      >
    </div>
    <div class="quick-attribute">
      <select v-model="newType" aria-label="Nhóm thuộc tính thêm nhanh">
        <option value="BRAND">Thương hiệu</option>
        <option value="SOLE">Loại đế</option>
        <option value="MATERIAL">Chất liệu</option></select
      ><input
        v-model.trim="newName"
        placeholder="Tên thuộc tính mới…"
        maxlength="100"
        aria-label="Tên thuộc tính thêm nhanh"
      /><button
        type="button"
        class="button button-outline"
        :disabled="busy || !newName"
        @click="add"
      >
        Thêm nhanh
      </button>
    </div>
    <article v-for="color in colors" :key="color" class="color-image-group">
      <div class="flex justify-between items-center mb-3">
        <h4 class="font-semibold">Các sản phẩm màu {{ color }}</h4>
        <button type="button" class="button button-outline" @click="pick(color)">
          Chọn ảnh {{ color }}
        </button>
      </div>
      <div class="color-thumbnails">
        <template
          v-for="(item, index) in modelValue.images || []"
          :key="item.color + '/' + item.mediaId"
          ><div v-if="item.color === color">
            <img :src="item.url" :alt="'Ảnh màu ' + color" /><button
              type="button"
              aria-label="Bỏ ảnh khỏi màu"
              @click="remove(index)"
            >
              ✕
            </button>
          </div></template
        >
        <p
          v-if="!(modelValue.images || []).some((i) => i.color === color)"
          class="text-sm text-stone-500 py-5"
        >
          Chưa chọn ảnh cho màu này.
        </p>
      </div>
    </article>
    <MediaPicker ref="picker" @select="image" />
  </section>
</template>
