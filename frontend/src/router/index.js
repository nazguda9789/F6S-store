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
          path: 'staff',
          component: () => import('../views/admin/PeopleView.vue'),
          props: { kind: 'staff' },
          meta: { title: 'Nhân viên', adminOnly: true },
        },
        {
          path: 'customers',
          component: () => import('../views/admin/PeopleView.vue'),
          props: { kind: 'customers' },
          meta: { title: 'Khách hàng' },
        },
        {
          path: 'attributes',
          component: () => import('../views/admin/AttributesView.vue'),
          meta: { title: 'Thuộc tính sản phẩm', adminOnly: true },
        },
        {
          path: 'media',
          component: () => import('../views/admin/MediaView.vue'),
          meta: { title: 'Thư viện hình ảnh', adminOnly: true },
        },
        {
          path: '',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: 'Tổng quan', adminOnly: true },
        },
        {
          path: 'products',
          component: () => import('../views/admin/ProductsView.vue'),
          meta: { title: 'Quản lý sản phẩm', adminOnly: true },
        },
        {
          path: 'pos',
          component: () => import('../views/admin/PosView.vue'),
          meta: { title: 'Bán hàng tại quầy' },
        },
        {
          path: 'promotions',
          component: () => import('../views/admin/PromotionsView.vue'),
          meta: { title: 'Khuyến mãi', adminOnly: true },
        },
        {
          path: 'orders',
          component: () => import('../views/admin/OrdersView.vue'),
          meta: { title: 'Đơn hàng', adminOnly: true },
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
  if (to.meta.adminOnly && !useAuth().isAdmin) return '/admin/pos'
})
export default router
