import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../stores/auth'
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    return saved || { top: 0 }
  },
  routes: [
    {
      path: '/admin/login',
      component: () => import('../views/admin/LoginView.vue'),
      meta: { title: 'Đăng nhập quản trị' },
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        {
          path: '',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: 'Tổng quan' },
        },
        {
          path: 'products',
          component: () => import('../views/admin/ProductsView.vue'),
          meta: { title: 'Quản lý sản phẩm' },
        },
        {
          path: 'pos',
          component: () => import('../views/admin/PosView.vue'),
          meta: { title: 'Bán hàng tại quầy' },
        },
        {
          path: 'promotions',
          component: () => import('../views/admin/PromotionsView.vue'),
          meta: { title: 'Khuyến mãi' },
        },
        {
          path: 'orders',
          component: () => import('../views/admin/OrdersView.vue'),
          meta: { title: 'Đơn hàng' },
        },
      ],
    },
    { path: '/', component: () => import('../views/HomeView.vue'), meta: { title: 'Trang chủ' } },
    {
      path: '/catalog',
      component: () => import('../views/CatalogView.vue'),
      meta: { title: 'Bộ sưu tập' },
    },
    {
      path: '/products/:id',
      component: () => import('../views/ProductView.vue'),
      meta: { title: 'Chi tiết sản phẩm' },
    },
    {
      path: '/checkout',
      component: () => import('../views/CheckoutView.vue'),
      meta: { title: 'Thanh toán' },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: 'Không tìm thấy' },
    },
  ],
})
router.beforeEach((to) => {
  if (to.meta.admin && !useAuth().authenticated) return '/admin/login'
})
export default router
