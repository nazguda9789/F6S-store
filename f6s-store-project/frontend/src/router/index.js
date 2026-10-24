import { createRouter, createWebHistory } from 'vue-router'
export default createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    return saved || { top: 0 }
  },
  routes: [
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
