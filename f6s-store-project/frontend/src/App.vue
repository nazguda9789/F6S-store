<script setup>
import { watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useCart } from './stores/cart'
import CartDrawer from './components/CartDrawer.vue'
const cart = useCart(),
  route = useRoute(),
  menu = ref(false)
watch(
  () => route.fullPath,
  () => {
    menu.value = false
    cart.open = false
    document.title = `${route.meta.title || 'F6S'} — F6S Sports`
  },
)
</script>
<template>
  <a href="#main" class="skip-link">Đến nội dung chính</a>
  <div class="announcement">F6S SPORTS CLUB <span>·</span> Chuyển động theo cách của bạn</div>
  <header class="site-header">
    <div class="shell flex items-center justify-between gap-6 h-22">
      <RouterLink to="/" class="brand" aria-label="F6S trang chủ"
        >F6S<span>SPORTS / EVERYDAY</span></RouterLink
      >
      <nav aria-label="Điều hướng chính" class="desktop-nav">
        <RouterLink to="/">Trang chủ</RouterLink
        ><RouterLink to="/catalog">Tất cả sản phẩm</RouterLink
        ><RouterLink to="/catalog?featured=true">F6S Select <ArrowIcon /></RouterLink>
      </nav>
      <div class="flex gap-3 items-center">
        <button class="cart-trigger" @click="cart.open = true">
          Giỏ hàng <span>{{ cart.count }}</span></button
        ><button
          class="mobile-toggle icon-button"
          :aria-expanded="menu"
          aria-label="Mở menu"
          @click="menu = !menu"
        >
          ☰
        </button>
      </div>
    </div>
    <nav v-if="menu" class="mobile-nav shell" aria-label="Menu di động">
      <RouterLink to="/">Trang chủ</RouterLink><RouterLink to="/catalog">Sản phẩm</RouterLink
      ><RouterLink to="/catalog?featured=true">F6S Select</RouterLink>
    </nav>
  </header>
  <main id="main" tabindex="-1"><RouterView /></main>
  <footer class="footer">
    <div class="shell grid md:grid-cols-3 gap-10">
      <div>
        <RouterLink to="/" class="brand text-white">F6S<span>MOVE YOUR WAY.</span></RouterLink>
        <p class="mt-5 max-w-xs text-sm text-white/60">
          Giày thể thao cho từng nhịp chuyển động. Chạy bộ, tập luyện và sống theo cách của bạn.
        </p>
      </div>
      <div>
        <h2 class="font-semibold mb-4">Khám phá</h2>
        <RouterLink to="/catalog">Bộ sưu tập</RouterLink
        ><RouterLink to="/catalog?featured=true">Sản phẩm nổi bật</RouterLink>
      </div>
      <div>
        <h2 class="font-semibold mb-4">Thông tin bản demo</h2>
        <p class="text-sm text-white/60">
          Ảnh minh họa và sản phẩm mẫu. Thanh toán COD; chưa tích hợp OTP/VNPAY của bản cũ.
        </p>
      </div>
    </div>
    <div class="shell mt-12 pt-5 border-t border-white/15 text-xs text-white/50">
      © {{ new Date().getFullYear() }} F6S SPORTS · Built to move.
    </div>
  </footer>
  <CartDrawer />
  <div v-if="cart.notice" role="status" class="toast">{{ cart.notice }}</div>
</template>
