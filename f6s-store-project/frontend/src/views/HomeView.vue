<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../lib/api'
import ProductCard from '../components/ProductCard.vue'
const products = ref([]),
  categories = ref([]),
  loading = ref(true),
  error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [p, c] = await Promise.all([api('/products?featured=true&limit=8'), api('/categories')])
    products.value = p.content
    categories.value = c
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<template>
  <section class="hero">
    <div class="shell hero-grid">
      <div class="hero-copy">
        <p class="eyebrow text-white/70">THE EVERYDAY ATHLETE / F6S 2026</p>
        <h1>Không chỉ bước.<br />Hãy <em>chuyển động.</em></h1>
        <p class="hero-description">
          Tìm đôi giày cùng bạn vượt giới hạn — từ đường chạy đầu tiên đến mọi hành trình thường
          ngày.
        </p>
        <RouterLink to="/catalog" class="button button-lime"
          >Khám phá bộ sưu tập <span><ArrowIcon /></span
        ></RouterLink>
        <div class="hero-bottom"><span>01 / 05</span><span>DESIGNED FOR YOUR NEXT MOVE</span></div>
      </div>
      <div class="hero-art">
        <span class="hero-orbit"></span><span class="hero-word">MOVE</span
        ><img src="/images/shoe-0.svg" alt="Giày thể thao F6S minh họa" fetchpriority="high" />
        <div class="hero-label">
          <span>F6S VELOCITY</span><strong>Nhẹ từng bước. Vững hành trình.</strong>
        </div>
        <span class="hero-tag">SPORTS<br />ESSENTIALS <ArrowIcon /></span>
      </div>
    </div>
  </section>
  <section class="benefits shell">
    <span><ArrowIcon /> <strong>Biến thể đa dạng</strong> Màu sắc & size của bạn</span
    ><span>◇ <strong>Giá minh bạch</strong> Xác nhận trước khi đặt</span
    ><span>◉ <strong>Thanh toán COD</strong> Trả tiền khi nhận hàng</span>
  </section>
  <section class="shell section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">FIND YOUR MOVEMENT</p>
        <h2>Mỗi chuyển động. Một lựa chọn.</h2>
      </div>
      <RouterLink to="/catalog" class="text-link">Tất cả danh mục <ArrowIcon /></RouterLink>
    </div>
    <div class="category-grid">
      <RouterLink
        v-for="(category, i) in categories"
        :key="category.id"
        :to="`/catalog?category=${category.id}`"
        class="category-tile"
        ><span class="eyebrow">0{{ i + 1 }}</span
        ><img :src="`/images/shoe-${i % 5}.svg`" alt="" loading="lazy" />
        <div class="flex justify-between items-center">
          <h3>{{ category.name }}</h3>
          <span><ArrowIcon /></span></div
      ></RouterLink>
    </div>
  </section>
  <section class="shell section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">CURATED BY F6S</p>
        <h2>Sẵn sàng cho bước tiếp theo.</h2>
      </div>
      <RouterLink to="/catalog?featured=true" class="text-link"
        >Xem F6S Select <ArrowIcon
      /></RouterLink>
    </div>
    <p v-if="loading" role="status" class="empty-state">Đang tải bộ sưu tập…</p>
    <div v-else-if="error" role="alert" class="error-box">
      {{ error }} <button class="underline" @click="load">Thử lại</button>
    </div>
    <div v-else-if="products.length" class="product-grid">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
    <p v-else class="empty-state">Chưa có sản phẩm nổi bật.</p>
  </section>
  <section class="shell mb-20">
    <div class="editorial">
      <div>
        <p class="eyebrow">MORE THAN A PAIR OF SHOES</p>
        <h2>Nhịp sống của bạn.<br />Phong cách của bạn.</h2>
        <RouterLink to="/catalog" class="button mt-7"
          >Tìm đôi giày của bạn <ArrowIcon
        /></RouterLink>
      </div>
      <span class="editorial-symbol" aria-hidden="true"><ArrowIcon /></span>
    </div>
  </section>
</template>
