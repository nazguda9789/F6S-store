<script setup>
import { watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
const auth = useAuth(),
  router = useRouter(),
  menu = ref(false)
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
      <nav aria-label="Quản trị" @click="menu = false">
        <RouterLink to="/admin" exact-active-class="active">◈ Tổng quan</RouterLink
        ><RouterLink to="/admin/pos">▣ Bán hàng tại quầy</RouterLink
        ><RouterLink to="/admin/products">◇ Quản lý sản phẩm</RouterLink
        ><RouterLink to="/admin/promotions">% Khuyến mãi</RouterLink
        ><RouterLink to="/admin/orders">▤ Quản lý đơn hàng</RouterLink>
      </nav>
      <div class="sidebar-bottom">
        <RouterLink to="/">↩ Xem cửa hàng</RouterLink><button @click="logout">Đăng xuất</button>
      </div>
    </aside>
    <div class="admin-main">
      <header class="admin-topbar">
        <button class="icon-button" aria-label="Ẩn/hiện menu quản trị" @click="menu = !menu">
          ☰</button
        ><span>F6S / Quản trị bán hàng</span><span class="ml-auto">{{ auth.username }}</span>
      </header>
      <div class="admin-content"><RouterView /></div>
    </div>
  </div>
</template>
