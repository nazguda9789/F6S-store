<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import QRCode from 'qrcode'
import { api, money } from '../../lib/api'
import ProductMetadataEditor from '../../components/ProductMetadataEditor.vue'
const details = ref({ brandId: null, soleId: null, materialId: null, images: [] })
const categories = ref([]),
  result = ref({ content: [], page: 0, totalPages: 0 }),
  editing = ref(null),
  showForm = ref(false),
  rows = ref([]),
  busy = ref(false),
  error = ref(''),
  success = ref(''),
  categoryName = ref(''),
  qr = ref(null),
  qrDialog = ref(null)
const form = reactive({
  name: '',
  brand: 'F6S',
  description: '',
  imageUrl: '/images/shoe-0.svg',
  hoverImageUrl: '/images/shoe-1.svg',
  categoryId: '',
  featured: false,
  colors: 'Đen, Trắng',
  sizes: '38, 39, 40, 41, 42',
  defaultStock: 10,
  defaultPrice: 1000000,
})
let previewSignature = ''
const selection = computed(() => ({
  colors: form.colors
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
  sizes: form.sizes
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
  defaultStock: form.defaultStock,
  defaultPrice: form.defaultPrice,
}))
const signature = computed(() => JSON.stringify(selection.value)),
  ready = computed(() => rows.value.length && previewSignature === signature.value)
async function run(work) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  success.value = ''
  try {
    await work()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function list(page = 0) {
  result.value = await api('/products?limit=12&page=' + page)
}
async function refresh() {
  await run(() => list())
}
function add() {
  editing.value = null
  showForm.value = true
  rows.value = []
  details.value = { brandId: null, soleId: null, materialId: null, images: [] }
  Object.assign(form, {
    name: '',
    brand: 'F6S',
    description: '',
    imageUrl: '/images/shoe-0.svg',
    hoverImageUrl: '/images/shoe-1.svg',
    categoryId: categories.value[0]?.id || '',
    featured: false,
    colors: 'Đen, Trắng',
    sizes: '38, 39, 40, 41, 42',
    defaultStock: 10,
    defaultPrice: 1000000,
  })
  previewSignature = ''
}
async function preview() {
  await run(async () => {
    const valid = rows.value.filter(
      (r) => selection.value.colors.includes(r.color) && selection.value.sizes.includes(r.size),
    )
    rows.value = await api('/admin/products/matrix/preview', {
      method: 'POST',
      body: JSON.stringify({ ...selection.value, overrides: valid }),
    })
    previewSignature = signature.value
  })
}
async function edit(p) {
  await run(async () => {
    const detail = await api('/products/' + p.id)
    editing.value = p.id
    showForm.value = true
    Object.assign(form, {
      ...detail,
      categoryId: detail.category.id,
      colors: detail.colorways.join(', '),
      sizes: [...new Set(detail.variants.map((v) => v.size))].join(', '),
      defaultStock: 0,
      defaultPrice: detail.price,
    })
    rows.value = detail.variants.map((v) => ({
      color: v.color,
      size: v.size,
      stock: v.stock,
      price: v.price,
      weightGrams: v.weightGrams,
    }))
    details.value = {
      brandId: detail.metadata?.brand?.id ?? null,
      soleId: detail.metadata?.sole?.id ?? null,
      materialId: detail.metadata?.material?.id ?? null,
      images: (detail.metadata?.images || []).map((i) => ({
        color: i.color,
        mediaId: i.mediaId,
        url: i.url,
      })),
    }
    previewSignature = signature.value
  })
}
async function save() {
  if (!ready.value) return
  await run(async () => {
    const metadata = {
      name: form.name,
      brand: form.brand,
      description: form.description,
      imageUrl: form.imageUrl,
      hoverImageUrl: form.hoverImageUrl,
      categoryId: Number(form.categoryId),
      featured: form.featured,
      metadata: {
        brandId: details.value.brandId,
        soleId: details.value.soleId,
        materialId: details.value.materialId,
        images: details.value.images.map((i) => ({ color: i.color, mediaId: i.mediaId })),
      },
    }
    let p
    if (editing.value)
      p = await api('/admin/products/' + editing.value, {
        method: 'PUT',
        body: JSON.stringify({
          ...metadata,
          price: Math.min(...rows.value.map((r) => r.price)),
          variants: rows.value,
        }),
      })
    else
      p = await api('/admin/products/matrix', {
        method: 'POST',
        body: JSON.stringify({
          ...metadata,
          matrix: { ...selection.value, overrides: rows.value },
        }),
      })
    showForm.value = false
    await list()
    success.value = `Đã lưu ${p.name}: ${p.variants.length} biến thể. SKU được giữ ổn định khi sửa.`
  })
}
async function remove(p) {
  if (!confirm('Ngừng bán ' + p.name + '? Lịch sử đơn hàng vẫn được giữ.')) return
  await run(async () => {
    await api('/admin/products/' + p.id, { method: 'DELETE' })
    await list(result.value.page)
  })
}
async function addCategory() {
  if (!categoryName.value.trim()) return
  await run(async () => {
    const c = await api('/admin/categories', {
      method: 'POST',
      body: JSON.stringify({ name: categoryName.value.trim() }),
    })
    categories.value.push(c)
    form.categoryId = c.id
    categoryName.value = ''
  })
}
async function showQr(p) {
  await run(async () => {
    const detail = await api('/products/' + p.id)
    qr.value = {
      name: p.name,
      variants: await Promise.all(
        detail.variants.map(async (v) => ({
          ...v,
          image: await QRCode.toDataURL(v.qrPayload, { width: 160, margin: 2 }),
        })),
      ),
    }
    qrDialog.value.showModal()
  })
}
function print() {
  window.print()
}
onMounted(() =>
  run(async () => {
    categories.value = await api('/categories')
    await list()
  }),
)
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">CATALOG / VARIANT MATRIX</p>
        <h1>Quản lý sản phẩm</h1>
      </div>
      <button class="button" :disabled="busy" @click="add">+ Thêm sản phẩm</button>
    </div>
    <p v-if="error" role="alert" class="error-box mb-5">{{ error }}</p>
    <p v-if="success" role="status" class="success-message">{{ success }}</p>
    <form v-if="showForm" class="admin-card mb-6" @submit.prevent="save">
      <div class="flex justify-between mb-6">
        <h2 class="text-xl font-semibold">
          {{ editing ? 'Chỉnh sửa sản phẩm #' + editing : 'Tạo sản phẩm và biến thể' }}
        </h2>
        <button type="button" :disabled="busy" class="text-sm underline" @click="showForm = false">
          Đóng
        </button>
      </div>
      <fieldset :disabled="busy">
        <div class="admin-form-grid">
          <label class="field"
            >Tên sản phẩm<input v-model.trim="form.name" required maxlength="180" /></label
          ><label class="field"
            >Thương hiệu<input v-model.trim="form.brand" required maxlength="100" /></label
          ><label class="field"
            >Danh mục<select v-model="form.categoryId" required>
              <option value="" disabled>Chọn danh mục</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select></label
          >
          <div class="field">
            <label for="quick-category">Thêm danh mục nhanh</label>
            <div class="flex gap-2">
              <input id="quick-category" v-model.trim="categoryName" maxlength="100" /><button
                type="button"
                class="button button-outline"
                :disabled="!categoryName"
                @click="addCategory"
              >
                Thêm
              </button>
            </div>
          </div>
          <label class="field"
            >URL ảnh chính<input
              v-model.trim="form.imageUrl"
              required
              maxlength="1000"
              placeholder="/images/shoe-0.svg" /></label
          ><label class="field"
            >URL ảnh hover<input v-model.trim="form.hoverImageUrl" maxlength="1000"
          /></label>
        </div>
        <label class="field"
          >Mô tả<textarea
            v-model.trim="form.description"
            required
            maxlength="4000"
            rows="3"
          /></label
        ><label class="flex items-center gap-2 text-sm mb-6"
          ><input v-model="form.featured" type="checkbox" />Sản phẩm nổi bật</label
        >
        <div class="matrix-config">
          <div class="admin-form-grid">
            <label class="field"
              >Màu sắc (cách nhau bằng dấu phẩy)<input
                v-model="form.colors"
                required
                placeholder="Đen, Trắng" /></label
            ><label class="field"
              >Size EU (cách nhau bằng dấu phẩy)<input
                v-model="form.sizes"
                required
                placeholder="38, 39, 40" /></label
            ><label class="field"
              >Tồn mặc định<input
                v-model.number="form.defaultStock"
                type="number"
                min="0"
                max="100000"
                step="1"
                required /></label
            ><label class="field"
              >Giá mặc định (₫)<input
                v-model.number="form.defaultPrice"
                type="number"
                min="1"
                max="1000000000"
                step="1"
                required
            /></label>
          </div>
          <button type="button" class="button button-outline" @click="preview">
            Sinh / cập nhật tổ hợp màu × size
          </button>
          <p class="text-xs text-stone-500 mt-3">
            Tối đa 100 tổ hợp. Giá và tồn đã nhập của tổ hợp cũ được giữ khi sinh lại. Biến thể bỏ
            khỏi sản phẩm khi sửa sẽ ngừng bán, không xóa lịch sử.
          </p>
        </div>
        <ProductMetadataEditor
          v-model="details"
          v-model:brand="form.brand"
          :colors="selection.colors"
        />
        <div v-if="rows.length" class="table-scroll mt-5">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Màu</th>
                <th>Size</th>
                <th>Số lượng tồn</th>
                <th>Giá riêng (₫)</th>
                <th>Khối lượng gói hàng (g, tùy chọn)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.color + '/' + row.size">
                <td>{{ row.color }}</td>
                <td>{{ row.size }}</td>
                <td>
                  <input
                    v-model.number="row.stock"
                    type="number"
                    min="0"
                    max="100000"
                    step="1"
                    required
                    aria-label="Tồn biến thể"
                  />
                </td>
                <td>
                  <input
                    v-model.number="row.price"
                    type="number"
                    min="1"
                    max="1000000000"
                    step="1"
                    required
                    aria-label="Giá biến thể"
                  />
                </td>
                <td>
                  <input
                    v-model.number="row.weightGrams"
                    type="number"
                    min="1"
                    max="10000"
                    step="1"
                    placeholder="Chưa đo"
                    aria-label="Khối lượng biến thể"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="rows.length && !ready" class="text-amber-800 mt-4 text-sm">
          Lựa chọn đã đổi. Bấm sinh tổ hợp để xác nhận trước khi lưu.
        </p>
        <button class="button mt-6" :disabled="!ready">
          Lưu sản phẩm ({{ rows.length }} biến thể)
        </button>
      </fieldset>
    </form>
    <div class="admin-card">
      <div class="flex justify-between mb-4">
        <h2 class="font-semibold">Danh sách đang bán</h2>
        <button class="text-sm underline" :disabled="busy" @click="refresh">Tải lại</button>
      </div>
      <div class="table-scroll">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Mã / Sản phẩm</th>
              <th>Danh mục / Hãng</th>
              <th>Tồn</th>
              <th>Giá từ</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in result.content" :key="p.id">
              <td>
                <strong>{{ p.name }}</strong
                ><small>{{ p.code }}</small>
              </td>
              <td>
                {{ p.category.name }}<small>{{ p.brand }}</small>
              </td>
              <td>{{ p.stock }}</td>
              <td>{{ money(p.price) }}</td>
              <td>
                <div class="flex gap-3 flex-wrap">
                  <button class="text-link" :disabled="busy" @click="edit(p)">Sửa</button
                  ><button class="text-link" :disabled="busy" @click="showQr(p)">Mã QR</button
                  ><button class="text-red-700" :disabled="busy" @click="remove(p)">
                    Ngừng bán
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="result.totalPages > 1" class="pagination">
        <button :disabled="busy || result.page === 0" @click="run(() => list(result.page - 1))">
          Trước</button
        ><span>{{ result.page + 1 }} / {{ result.totalPages }}</span
        ><button
          :disabled="busy || result.page + 1 >= result.totalPages"
          @click="run(() => list(result.page + 1))"
        >
          Sau
        </button>
      </div>
    </div>
    <dialog ref="qrDialog" class="qr-label-dialog">
      <div class="flex justify-between items-center mb-4 no-print">
        <h2 class="font-semibold">Tem QR theo biến thể</h2>
        <div class="flex gap-3">
          <button class="button button-outline" @click="print">In tem</button
          ><button class="icon-button" aria-label="Đóng tem QR" @click="qrDialog.close()">✕</button>
        </div>
      </div>
      <div v-if="qr" class="qr-labels print-area">
        <article v-for="v in qr.variants" :key="v.id">
          <img :src="v.image" :alt="'QR ' + v.sku" /><strong>F6S · {{ qr.name }}</strong>
          <p>{{ v.color }} / {{ v.size }} · {{ money(v.price) }}</p>
          <small>{{ v.sku }}</small>
        </article>
      </div>
    </dialog>
  </section>
</template>
