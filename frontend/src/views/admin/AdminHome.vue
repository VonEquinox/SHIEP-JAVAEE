<script setup>
// 后台布局：左侧导航 + 内容区
const menus = [
  { path: '/admin/hotels', label: '酒店与房型' },
  { path: '/admin/orders', label: '订单管理' },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/stats', label: '统计报表' },
];
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-nav flat-card">
      <router-link v-for="m in menus" :key="m.path" :to="m.path">{{ m.label }}</router-link>
    </aside>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.admin-nav {
  display: flex;
  flex-direction: column;
  padding: 0.8rem;
  gap: 0.2rem;
  min-width: 10rem;
  position: sticky;
  top: 1rem;
}

.admin-nav a {
  padding: 0.6rem 1rem;
  border-radius: 999px;
  color: var(--md-sys-color-on-surface-variant);
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 500;
  transition: background var(--motion-duration) var(--motion-easing);
}

.admin-nav a.router-link-active {
  background: var(--md-sys-color-primary-container);
  color: var(--md-sys-color-on-primary-container);
}

.admin-content {
  flex: 1;
  min-width: 0;
}
</style>
