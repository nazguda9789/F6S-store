<script setup>
import { watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
const auth = useAuth(),
  router = useRouter(),
  menu = ref(false)
const groups = ref({ transactions: true, products: true, accounts: true, discounts: true })
watch(
  () => auth.authenticated,
  (value) => {
    if (!value) router.replace('/admin/login')
  },
)
function logout() {
  auth.logout()
}
</script>
<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ expanded: menu }">
      <RouterLink to="/admin" class="brand">F6S<span>RETAIL WORKSPACE</span></RouterLink>
      <nav
        aria-label="Quản trị"
        @click="
          (e) => {
            if (e.target.closest('a')) menu = false
          }
        "
      >
        <RouterLink v-if="auth.isAdmin" to="/admin" exact-active-class="active"
          >◈ Tổng quan</RouterLink
        >
        <div class="nav-group">
          <button
            class="nav-group-title"
            @click="groups.transactions = !groups.transactions"
            :aria-expanded="groups.transactions"
          >
            ▣ Quản lý giao dịch <span>⌄</span>
          </button>
          <div v-if="groups.transactions" class="nav-submenu">
            <RouterLink to="/admin/pos">Bán hàng tại quầy</RouterLink
            ><RouterLink v-if="auth.isAdmin" to="/admin/orders">Danh sách hóa đơn</RouterLink>
          </div>
        </div>
        <div v-if="auth.isAdmin" class="nav-group">
          <div class="nav-group-row">
            <RouterLink to="/admin/products">◇ Quản lý sản phẩm</RouterLink
            ><button aria-label="Mở nhóm sản phẩm" @click="groups.products = !groups.products">
              ⌄
            </button>
          </div>
          <div v-if="groups.products" class="nav-submenu">
            <RouterLink to="/admin/products">Danh sách sản phẩm</RouterLink
            ><RouterLink to="/admin/attributes">Thuộc tính sản phẩm</RouterLink>
          </div>
        </div>
        <div class="nav-group">
          <button
            class="nav-group-title"
            @click="groups.accounts = !groups.accounts"
            :aria-expanded="groups.accounts"
          >
            ♙ Quản lý tài khoản <span>⌄</span>
          </button>
          <div v-if="groups.accounts" class="nav-submenu">
            <RouterLink v-if="auth.isAdmin" to="/admin/staff">Nhân viên</RouterLink
            ><RouterLink to="/admin/customers">Khách hàng</RouterLink
            ><RouterLink v-if="auth.isAdmin" to="/admin/shop-accounts">Tài khoản online</RouterLink>
          </div>
        </div>
        <div v-if="auth.isAdmin" class="nav-group">
          <button
            class="nav-group-title"
            @click="groups.discounts = !groups.discounts"
            :aria-expanded="groups.discounts"
          >
            % Giảm giá <span>⌄</span>
          </button>
          <div v-if="groups.discounts" class="nav-submenu">
            <RouterLink to="/admin/promotions">Khuyến mãi</RouterLink>
          </div>
        </div>
        <RouterLink v-if="auth.isAdmin" to="/admin/media">▧ Thư viện hình ảnh</RouterLink>
      </nav>
      <div class="sidebar-bottom">
        <RouterLink to="/">↩ Xem cửa hàng</RouterLink><button @click="logout">Đăng xuất</button>
      </div>
    </aside>
    <div class="admin-main">
      <header class="admin-topbar">
        <button class="icon-button" aria-label="Ẩn/hiện menu quản trị" @click="menu = !menu">
          ☰</button
        ><span>F6S / {{ $route.meta.title }}</span
        ><span class="ml-auto"
          >{{ auth.username }} · {{ auth.isAdmin ? 'Quản trị' : 'Nhân viên' }}</span
        >
      </header>
      <div class="admin-content"><RouterView /></div>
    </div>
  </div>
</template>
