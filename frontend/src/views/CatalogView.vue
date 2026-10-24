<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'
import ProductCard from '../components/ProductCard.vue'
const route = useRoute(),
  router = useRouter(),
  filters = reactive({
    q: '',
    category: '',
    brand: '',
    size: '',
    color: '',
    min: '',
    max: '',
    sort: 'newest',
    featured: '',
  })
const categories = ref([]),
  facets = ref({ brands: [], colors: [], sizes: [] }),
  result = ref({ content: [], totalPages: 0, totalElements: 0, page: 0 }),
  loading = ref(true),
  error = ref(''),
  showFilters = ref(false)
let controller
function sync() {
  for (const key in filters)
    filters[key] = String(route.query[key] ?? (key === 'sort' ? 'newest' : ''))
}
async function load() {
  sync()
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const data = await api('/products?' + new URLSearchParams(route.query), {
      signal: current.signal,
    })
    if (!current.signal.aborted) result.value = data
  } catch (e) {
    if (e.name !== 'AbortError' && !current.signal.aborted) error.value = e.message
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}
function apply() {
  const query = {}
  for (const [k, v] of Object.entries(filters)) if (v !== '') query[k] = v
  router.push({ path: '/catalog', query })
  showFilters.value = false
}
function reset() {
  router.push('/catalog')
}
function page(value) {
  router.push({ query: { ...route.query, page: value } })
}
watch(() => route.fullPath, load, { immediate: true })
onMounted(async () => {
  try {
    ;[categories.value, facets.value] = await Promise.all([api('/categories'), api('/facets')])
  } catch (e) {
    error.value = e.message
  }
})
onBeforeUnmount(() => controller?.abort())
</script>
<template>
  <div class="shell section">
    <p class="eyebrow">HOME / COLLECTION</p>
    <div class="section-heading mt-4">
      <div>
        <h1 class="page-title">Tìm nhịp chuyển động.</h1>
        <p class="text-stone-500 mt-3">
          {{ result.totalElements }} thiết kế cho hành trình của bạn.
        </p>
      </div>
      <button class="button button-outline mobile-filter" @click="showFilters = !showFilters">
        Bộ lọc ☷
      </button>
    </div>
    <div class="catalog-layout">
      <aside class="filter-sidebar" :class="{ 'filters-open': showFilters }">
        <form @submit.prevent="apply">
          <div class="flex justify-between items-center mb-6">
            <h2 class="font-semibold text-lg">Bộ lọc</h2>
            <button type="button" class="text-sm underline" @click="reset">Đặt lại</button>
          </div>
          <label class="field"
            >Tìm sản phẩm<input v-model="filters.q" placeholder="Tên giày…" maxlength="180"
          /></label>
          <label class="field"
            >Danh mục<select v-model="filters.category">
              <option value="">Tất cả danh mục</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select></label
          >
          <label class="field"
            >Thương hiệu<select v-model="filters.brand">
              <option value="">Tất cả thương hiệu</option>
              <option v-for="b in facets.brands" :key="b">{{ b }}</option>
            </select></label
          >
          <div class="grid grid-cols-2 gap-3">
            <label class="field"
              >Size EU<select v-model="filters.size">
                <option value="">Tất cả</option>
                <option v-for="s in facets.sizes" :key="s">{{ s }}</option>
              </select></label
            ><label class="field"
              >Màu sắc<select v-model="filters.color">
                <option value="">Tất cả</option>
                <option v-for="c in facets.colors" :key="c">{{ c }}</option>
              </select></label
            >
          </div>
          <label class="field"
            >Giá từ (₫)<input
              v-model="filters.min"
              type="number"
              min="0"
              step="1"
              placeholder="0" /></label
          ><label class="field"
            >Giá đến (₫)<input
              v-model="filters.max"
              type="number"
              min="0"
              step="1"
              placeholder="Không giới hạn"
          /></label>
          <label class="field"
            >Sắp xếp<select v-model="filters.sort">
              <option value="newest">Mới nhất</option>
              <option value="price-asc">Giá thấp → cao</option>
              <option value="price-desc">Giá cao → thấp</option>
              <option value="name">Tên A → Z</option>
            </select></label
          ><button class="button w-full">Áp dụng bộ lọc <ArrowIcon /></button>
        </form>
        <div class="filter-note">Chọn màu và size để tìm đúng biến thể còn hàng.</div>
      </aside>
      <section aria-label="Danh sách sản phẩm" :aria-busy="loading">
        <p v-if="loading" role="status" class="empty-state">Đang tìm đôi giày phù hợp…</p>
        <div v-else-if="error" role="alert" class="error-box">
          {{ error }} <button class="underline" @click="load">Thử lại</button>
        </div>
        <template v-else-if="result.content.length"
          ><div class="product-grid catalog-products">
            <ProductCard v-for="product in result.content" :key="product.id" :product="product" />
          </div>
          <nav v-if="result.totalPages > 1" aria-label="Phân trang" class="pagination">
            <button :disabled="result.page === 0" @click="page(result.page - 1)">← Trước</button
            ><span>{{ result.page + 1 }} / {{ result.totalPages }}</span
            ><button
              :disabled="result.page + 1 >= result.totalPages"
              @click="page(result.page + 1)"
            >
              Sau →
            </button>
          </nav></template
        >
        <div v-else class="empty-state">
          <h2 class="text-xl mb-3">Chưa tìm thấy sản phẩm phù hợp</h2>
          <button class="text-link" @click="reset">Xóa bộ lọc và thử lại <ArrowIcon /></button>
        </div>
      </section>
    </div>
  </div>
</template>
