import { createRouter, createWebHashHistory } from 'vue-router';
import { auth, isAdmin } from '../store/auth';

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/orders', component: () => import('../views/Orders.vue'), meta: { auth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  { path: '/admin', component: () => import('../views/admin/AdminHome.vue'), meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/hotels' },
      { path: 'hotels', component: () => import('../views/admin/AdminHotels.vue') },
      { path: 'orders', component: () => import('../views/admin/AdminOrders.vue') },
      { path: 'users', component: () => import('../views/admin/AdminUsers.vue') },
      { path: 'stats', component: () => import('../views/admin/AdminStats.vue') },
    ],
  },
];

const router = createRouter({ history: createWebHashHistory(), routes });

router.beforeEach((to) => {
  if ((to.meta.auth || to.meta.admin) && !auth.token) return '/login';
  if (to.meta.admin && !isAdmin()) return '/';
});

export default router;
