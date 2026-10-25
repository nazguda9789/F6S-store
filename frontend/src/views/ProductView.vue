<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, money } from '../lib/api'
import { useCart } from '../stores/cart'
const route = useRoute(),
  cart = useCart(),
  product = ref(null),
  color = ref(''),
  size = ref(''),
  quantity = ref(1),
  error = ref(''),
  loading = ref(true)
const sizes = computed(() =>
  [...new Set(product.value?.variants.map((v) => v.size) || [])].sort(
    (a, b) => Number(a) - Number(b),
  ),
)
const selected = computed(() =>
  product.value?.variants.find((v) => v.color === color.value && v.size === size.value),
)
function stock(s) {
  return product.value?.variants.find((v) => v.color === color.value && v.size === s)?.stock || 0
}
const photoIndex = ref(0),
  photos = computed(() => {
    const images =
      product.value?.metadata?.images?.filter((i) => i.color === color.value).map((i) => i.url) ||
      []
    return images.length ? images : [product.value?.imageUrl].filter(Boolean)
  }),
  mainPhoto = computed(() => photos.value[photoIndex.value] || product.value?.imageUrl)
let serial = 0
async function load() {
  const request = ++serial
  loading.value = true
  error.value = ''
  try {
    const p = await api('/products/' + encodeURIComponent(route.params.id))
    if (request !== serial) return
    product.value = p
    color.value = p.colorways[0] || ''
    size.value = ''
    quantity.value = 1
  } catch (e) {
    if (request === serial) error.value = e.message
  } finally {
    if (request === serial) loading.value = false
  }
}
watch(() => route.params.id, load, { immediate: true })
watch(color, () => {
  photoIndex.value = 0
  size.value = ''
  quantity.value = 1
})
watch(size, () => (quantity.value = 1))
</script>
<template>
  <div class="shell section">
    <p v-if="loading" role="status" class="empty-state">Đang tải sản phẩm…</p>
    <div v-else-if="error" class="error-box" role="alert">
      {{ error }} <RouterLink to="/catalog" class="underline">Về bộ sưu tập</RouterLink>
    </div>
    <template v-else-if="product"
      ><nav class="eyebrow mb-8" aria-label="Đường dẫn">
        <RouterLink to="/catalog">BỘ SƯU TẬP</RouterLink> / {{ product.category.name }} /
        {{ product.code }}
      </nav>
      <div class="pdp-grid">
        <div>
          <div class="pdp-image">
            <span class="eyebrow">F6S / {{ product.brand }}</span
            ><img :src="mainPhoto" :alt="product.name + ' / ' + color" />
          </div>
          <div v-if="photos.length > 1" class="pdp-thumbnails">
            <button
              v-for="(photo, index) in photos"
              :key="photo"
              :aria-pressed="photoIndex === index"
              @click="photoIndex = index"
            >
              <img :src="photo" :alt="'Ảnh ' + (index + 1) + ' màu ' + color" />
            </button>
          </div>
          <img
            v-if="product.hoverImageUrl"
            :src="product.hoverImageUrl"
            :alt="`Ảnh minh họa thứ hai của ${product.name}`"
            class="pdp-secondary"
          />
        </div>
        <section class="pdp-info">
          <p class="eyebrow">{{ product.brand }} · {{ product.category.name }}</p>
          <h1 class="page-title mt-3">{{ product.name }}</h1>
          <p class="text-2xl font-semibold mt-5">
            {{ selected ? '' : 'Từ ' }}{{ money(selected?.price ?? product.price) }}
          </p>
          <p class="text-stone-500 mt-5 leading-relaxed">{{ product.description }}</p>
          <dl class="product-facts">
            <div>
              <dt>Loại đế</dt>
              <dd>{{ product.metadata?.sole?.name || 'Chưa xác nhận' }}</dd>
            </div>
            <div>
              <dt>Chất liệu</dt>
              <dd>{{ product.metadata?.material?.name || 'Chưa xác nhận' }}</dd>
            </div>
            <div v-if="selected?.weightGrams">
              <dt>Khối lượng gói hàng</dt>
              <dd>{{ selected.weightGrams }} g</dd>
            </div>
          </dl>
          <fieldset class="mt-8">
            <legend class="font-semibold mb-3">Phối màu: {{ color }}</legend>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="c in product.colorways"
                :key="c"
                :aria-pressed="color === c"
                :class="['choice', { selected: color === c }]"
                @click="color = c"
              >
                {{ c }}
              </button>
            </div>
          </fieldset>
          <fieldset class="mt-7">
            <legend class="font-semibold mb-3">
              Kích cỡ EU <span class="text-stone-400 font-normal">/ Chọn size của bạn</span>
            </legend>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="s in sizes"
                :key="s"
                :disabled="!stock(s)"
                :aria-pressed="size === s"
                :class="['choice size-choice', { selected: size === s }]"
                @click="size = s"
              >
                {{ s }}
              </button>
            </div>
          </fieldset>
          <p class="text-sm text-stone-500 mt-4" aria-live="polite">
            {{
              selected
                ? `Còn ${selected.stock} đôi · SKU ${selected.sku}`
                : 'Vui lòng chọn màu và size. Size mờ là hết hàng.'
            }}
          </p>
          <div class="flex gap-3 mt-7">
            <label class="field w-24 mb-0"
              >Số lượng<input
                v-model.number="quantity"
                type="number"
                min="1"
                :max="Math.min(99, selected?.stock || 1)" /></label
            ><button
              class="button flex-1 self-end"
              :disabled="
                !selected?.stock ||
                !Number.isInteger(quantity) ||
                quantity < 1 ||
                quantity > Math.min(99, selected?.stock || 0)
              "
              @click="cart.add(product, selected, quantity)"
            >
              Thêm vào giỏ <ArrowIcon />
            </button>
          </div>
          <div class="border-t mt-8 pt-5 text-sm text-stone-500 leading-7">
            ✓ Giá bán được xác nhận tại bước đặt hàng<br />✓ Tồn kho theo từng màu và size<br />✓
            Thanh toán khi nhận hàng (COD)
          </div>
        </section>
      </div></template
    >
  </div>
</template>
