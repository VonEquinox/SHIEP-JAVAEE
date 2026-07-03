<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { auth, isAdmin, logout } from './store/auth';
import { theme, toggleTheme } from './store/theme';

const router = useRouter();
const route = useRoute();

/**
 * 页面转场（需求文档 7.3 “有迹可循”）：
 * - 顶部导航是“左中右”排布，切换页面时内容区做水平滑动：
 *   去往更右侧的栏目 → 当前页向左滑出、新页自右侧滑入；反之相反。
 * - 登录/注册 → 业务页：登录页向上滑出，业务页自下方向上滑入（“埋伏”在下面的效果）。
 */
const NAV_ORDER = { '/': 0, '/orders': 1, '/profile': 2, '/admin': 3 };
const orderOf = (path) => {
  if (path.startsWith('/admin')) return NAV_ORDER['/admin'];
  return NAV_ORDER[path] ?? 0;
};
const isAuthPage = (path) => path === '/login' || path === '/register';

const transitionName = ref('slide-left');
watch(
  () => route.path,
  (to, from) => {
    if (from === undefined) return;
    if (isAuthPage(from) && !isAuthPage(to)) transitionName.value = 'slide-up';
    else if (!isAuthPage(from) && isAuthPage(to)) transitionName.value = 'slide-down';
    else if (isAuthPage(from) && isAuthPage(to)) transitionName.value = 'slide-up';
    else transitionName.value = orderOf(to) > orderOf(from) ? 'slide-left' : 'slide-right';
  },
);

function onLogout() {
  logout();
  router.push('/login');
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header" data-site-header data-push-away>
      <router-link to="/" class="brand">易宿<span>Stay</span></router-link>
      <nav class="nav">
        <router-link to="/">找酒店</router-link>
        <router-link v-if="auth.token" to="/orders">我的订单</router-link>
        <router-link v-if="auth.token" to="/profile">个人中心</router-link>
        <router-link v-if="isAdmin()" to="/admin">后台管理</router-link>
      </nav>
      <div class="who">
        <button class="theme-btn" :title="theme.mode === 'dark' ? '切换到浅色' : '切换到深色'"
                @click="toggleTheme($event)">
          {{ theme.mode === 'dark' ? '☀' : '☾' }}
        </button>
        <template v-if="auth.token">
          <span class="pill">{{ auth.user.username }} · {{ auth.user.level }}</span>
          <el-button text @click="onLogout">退出</el-button>
        </template>
        <el-button v-else type="primary" round @click="router.push('/login')">登录</el-button>
      </div>
    </header>

    <main class="app-main" data-site-main>
      <div class="view-shell">
        <router-view v-slot="{ Component }">
          <transition :name="transitionName">
            <component :is="Component" :key="route.path.startsWith('/admin') ? '/admin' : route.path" />
          </transition>
        </router-view>
      </div>
    </main>

    <footer class="app-footer" data-site-footer>
      © {{ new Date().getFullYear() }} 易宿 · JavaEE 课程设计 · Nacos 微服务版
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-x: clip;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 2rem;
  max-width: 80rem;
  width: 100%;
  margin: 0 auto;
  padding: 1.1rem 2rem;
}

.brand {
  font-size: 1.35rem;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--md-sys-color-on-surface);
  text-decoration: none;
}

.brand span {
  color: var(--md-sys-color-primary);
  margin-left: 2px;
}

.nav {
  display: flex;
  gap: 1.4rem;
  flex: 1;
}

.nav a {
  color: var(--md-sys-color-on-surface-variant);
  text-decoration: none;
  font-size: 0.98rem;
  font-weight: 500;
  padding: 0.35rem 0;
  border-bottom: 2px solid transparent;
  transition: color var(--motion-duration) var(--motion-easing);
}

.nav a.router-link-active {
  color: var(--md-sys-color-primary);
  border-bottom-color: var(--md-sys-color-primary);
}

.who {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.theme-btn {
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 999px;
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface);
  font-size: 1.05rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color var(--motion-duration) var(--motion-easing);
}

.theme-btn:hover {
  color: var(--md-sys-color-primary);
}

.app-main {
  flex: 1;
  width: 100%;
  max-width: 80rem;
  margin: 0 auto;
  padding: 1.5rem 2rem 3rem;
}

/* 转场舞台：离场页绝对定位叠在原位，与入场页同时滑动 */
.view-shell {
  position: relative;
  min-height: 60vh;
}

.app-footer {
  text-align: center;
  padding: 2rem 0;
  font-size: 0.85rem;
  color: var(--md-sys-color-on-surface-variant);
}

/* ---------- 页面滑动转场 ---------- */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active,
.slide-up-enter-active,
.slide-up-leave-active,
.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.5s var(--motion-easing), opacity 0.5s var(--motion-easing);
}

.slide-left-leave-active,
.slide-right-leave-active,
.slide-up-leave-active,
.slide-down-leave-active {
  position: absolute;
  top: 0;
  /* 左右同时钉 0（而不是 left:0 + width:100%）：
     绝对定位下 margin:auto 仍可水平居中，登录卡片才能原地上滑而不是先跳到左边 */
  left: 0;
  right: 0;
}

/* 水平：左中右关系 */
.slide-left-enter-from {
  transform: translateX(calc(100% + 4rem));
}

.slide-left-leave-to {
  transform: translateX(calc(-100% - 4rem));
}

.slide-right-enter-from {
  transform: translateX(calc(-100% - 4rem));
}

.slide-right-leave-to {
  transform: translateX(calc(100% + 4rem));
}

/* 垂直：登录页与“埋伏”在下方的业务页 */
.slide-up-enter-from {
  transform: translateY(100vh);
}

.slide-up-leave-to {
  transform: translateY(-100vh);
  opacity: 0.6;
}

.slide-down-enter-from {
  transform: translateY(-100vh);
}

.slide-down-leave-to {
  transform: translateY(100vh);
  opacity: 0.6;
}
</style>

<style>
/* 阅读态（卡片全屏展开时）：页面级导航隐藏（退场动画由 useCardExpand 的幽灵完成），
   注意不能禁用滚动 —— 展开后的长内容需要上下滚动 */
html[data-reading='true'] [data-site-header],
html[data-reading='true'] [data-site-footer] {
  display: none;
}
</style>
